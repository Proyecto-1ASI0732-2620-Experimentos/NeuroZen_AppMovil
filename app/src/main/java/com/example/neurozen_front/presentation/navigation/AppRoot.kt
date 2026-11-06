package com.example.neurozen_front.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.data.session.SessionRepositoryImpl
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.usecase.LogoutUseCase
import com.example.neurozen_front.presentation.auth.AuthNavHost
import com.example.neurozen_front.presentation.auth.ResetPasswordScreen
import com.example.neurozen_front.presentation.components.PrimaryButton
import com.example.neurozen_front.presentation.psychologist.PsychologistRoot
import com.example.neurozen_front.presentation.worker.WorkerNavHost
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessions: SessionRepositoryImpl,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {
    val session = sessions.session
    val isLoaded = sessions.isLoaded
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    init {
        viewModelScope.launch {
            sessions.expiredEvents.collect {
                logoutUseCase()
                _notice.value = "Tu sesión expiró. Inicia sesión otra vez."
            }
        }
    }

    fun logout() = viewModelScope.launch { logoutUseCase() }
    fun clearNotice() { _notice.value = null }
}

/** Splash -> reset link -> auth -> navigation by role. ADMIN is web-only. */
@Composable
fun AppRoot(resetToken: String?, onResetTokenConsumed: () -> Unit, viewModel: AppViewModel = hiltViewModel()) {
    val loaded by viewModel.isLoaded.collectAsState()
    val session by viewModel.session.collectAsState()
    val notice by viewModel.notice.collectAsState()

    when {
        !loaded -> Splash()
        resetToken != null -> ResetPasswordScreen(initialToken = resetToken, onDone = onResetTokenConsumed)
        session == null -> AuthNavHost(notice = notice, onNoticeShown = viewModel::clearNotice)
        else -> when (session!!.role) {
            Role.USER -> WorkerNavHost(onLogout = viewModel::logout)
            Role.PSYCHOLOGIST -> PsychologistRoot(onLogout = viewModel::logout)
            Role.ADMIN, Role.UNKNOWN -> WebOnlyScreen(onLogout = viewModel::logout)
        }
    }
}

@Composable
private fun Splash() {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("NeuroZen", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator()
    }
}

@Composable
private fun WebOnlyScreen(onLogout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Usa la versión web para administrar", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "La verificación de psicólogos requiere revisar el portal del CPsP con calma, por eso el panel de administración solo está disponible en la web.",
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Cerrar sesión", onClick = onLogout)
    }
}
