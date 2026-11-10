package com.example.neurozen_front.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.neurozen_front.domain.model.Session
import com.example.neurozen_front.domain.repository.SessionRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "neurozen_session")

/**
 * Session persisted encrypted in DataStore and mirrored in memory so the OkHttp interceptor can read the
 * token synchronously. Emits [expiredEvents] when the backend answers 401 on an authenticated call.
 */
@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crypto: CryptoManager
) : SessionRepository {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _session = MutableStateFlow<Session?>(null)
    private val _isLoaded = MutableStateFlow(false)
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val session: StateFlow<Session?> = _session.asStateFlow()
    override val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()
    val expiredEvents: SharedFlow<Unit> = _expired.asSharedFlow()

    init {
        scope.launch {
            val prefs = context.sessionDataStore.data.first()
            val restored = prefs[KEY_SESSION]?.let(crypto::decrypt)
                ?.let { runCatching { gson.fromJson(it, Session::class.java) }.getOrNull() }
            _session.value = restored?.takeUnless { it.isExpired(System.currentTimeMillis() / 1000) }
            if (restored != null && _session.value == null) clear()
            _isLoaded.value = true
        }
    }

    override suspend fun save(session: Session, isNewAccount: Boolean) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_SESSION] = crypto.encrypt(gson.toJson(session))
            prefs[KEY_ONBOARDING] = isNewAccount && session.role == com.example.neurozen_front.domain.model.Role.USER
        }
        _session.value = session
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.remove(KEY_SESSION); it.remove(KEY_ONBOARDING) }
        _session.value = null
    }

    override suspend fun consumeOnboardingFlag(): Boolean {
        val pending = context.sessionDataStore.data.first()[KEY_ONBOARDING] ?: false
        if (pending) context.sessionDataStore.edit { it[KEY_ONBOARDING] = false }
        return pending
    }

    override suspend fun shouldShowVerifiedWelcome(userId: Long): Boolean =
        context.sessionDataStore.data.first()[welcomeKey(userId)] != true

    override suspend fun markVerifiedWelcomeShown(userId: Long) {
        context.sessionDataStore.edit { it[welcomeKey(userId)] = true }
    }

    override fun requireUserId(): Long = _session.value?.userId ?: error("No hay sesión activa")

    /** Synchronous accessor for the interceptor. */
    fun currentToken(): String? = _session.value?.token

    /** Called by the interceptor on a 401 of an authenticated request (never on business errors). */
    fun notifyExpired() {
        if (_session.value != null) _expired.tryEmit(Unit)
    }

    private fun welcomeKey(userId: Long) = booleanPreferencesKey("verified_welcome_$userId")

    private companion object {
        val KEY_SESSION = stringPreferencesKey("session_encrypted")
        val KEY_ONBOARDING = booleanPreferencesKey("pending_onboarding")
    }
}
