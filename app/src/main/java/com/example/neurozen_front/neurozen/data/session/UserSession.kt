package com.example.neurozen_front.neurozen.data.session

import com.example.neurozen_front.neurozen.data.network.AuthSession
import com.example.neurozen_front.neurozen.data.network.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSessionState(
    val token: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: UserRole = UserRole.CLIENT,
    val professionalId: Int? = null
)

object UserSession {
    private val _state = MutableStateFlow(UserSessionState())
    val state: StateFlow<UserSessionState> = _state.asStateFlow()

    fun save(authSession: AuthSession) {
        _state.value = UserSessionState(
            token = authSession.token,
            userId = authSession.userId,
            email = authSession.email,
            name = authSession.username,
            role = authSession.role,
            professionalId = authSession.professionalId
        )
    }

    fun setProfessionalId(id: Int) {
        _state.value = _state.value.copy(professionalId = id)
    }

    fun clear() {
        _state.value = UserSessionState()
    }

    val current: UserSessionState?
        get() = if (hasActiveSession()) _state.value else null

    fun hasActiveSession(): Boolean {
        return !_state.value.token.isNullOrBlank() && _state.value.userId != null
    }

    fun isPsychologist(): Boolean = _state.value.role == UserRole.PSYCHOLOGIST

    fun bearerTokenOrEmpty(): String {
        val token = _state.value.token.orEmpty()
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }
}
