package com.example.neurozen_front.neurozen.home.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.neurozen_front.neurozen.data.network.UserRole
import com.example.neurozen_front.neurozen.data.session.UserSession
import com.example.neurozen_front.neurozen.home.presentation.login.Login
import com.example.neurozen_front.neurozen.home.presentation.onboarding.OnBoarding
import com.example.neurozen_front.neurozen.psychologist.presentation.navigation.PsychologistNavHost

private enum class AppStage {
    Welcome,
    Login,
    Dashboard
}

@Composable
fun NeurozenApp() {
    var stage by remember { mutableStateOf(AppStage.Welcome) }
    val sessionState by UserSession.state.collectAsState()

    when (stage) {
        AppStage.Welcome -> OnBoarding(
            onStart = { stage = AppStage.Login },
            onExplore = {
                UserSession.clear()
                stage = AppStage.Dashboard
            }
        )

        AppStage.Login -> Login(
            onLoginSuccess = { stage = AppStage.Dashboard },
            onDemoAccess = {
                UserSession.clear()
                stage = AppStage.Dashboard
            }
        )

        AppStage.Dashboard -> {
            if (sessionState.role == UserRole.PSYCHOLOGIST) {
                PsychologistNavHost(
                    onLogout = {
                        UserSession.clear()
                        stage = AppStage.Login
                    }
                )
            } else {
                HomeNavHost(
                    onLogout = {
                        UserSession.clear()
                        stage = AppStage.Login
                    }
                )
            }
        }
    }
}
