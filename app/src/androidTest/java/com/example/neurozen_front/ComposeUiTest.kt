package com.example.neurozen_front

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurozen_front.presentation.auth.LoginScreen
import com.example.neurozen_front.presentation.auth.LoginUiState
import com.example.neurozen_front.presentation.theme.NeuroZenTheme
import com.example.neurozen_front.presentation.worker.ZenBotScreen
import com.example.neurozen_front.presentation.worker.ZenBotViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun login_typingAndSubmitting_callsCallbacks() {
        var email = ""
        var submitted = false
        compose.setContent {
            NeuroZenTheme {
                LoginScreen(
                    state = LoginUiState(), notice = null, onNoticeShown = {},
                    onEmail = { email = it }, onPassword = {}, onSubmit = { submitted = true },
                    onForgot = {}, onRegister = {}, onBack = {}
                )
            }
        }
        compose.onNodeWithTag("login_email").performTextInput("ana@empresa.pe")
        compose.onNodeWithTag("login_submit").performClick()
        assertEquals("ana@empresa.pe", email)
        assertTrue(submitted)
    }

    @Test
    fun login_showsBackendError() {
        compose.setContent {
            NeuroZenTheme {
                LoginScreen(
                    state = LoginUiState(error = "Correo o contraseña incorrectos"), notice = null, onNoticeShown = {},
                    onEmail = {}, onPassword = {}, onSubmit = {}, onForgot = {}, onRegister = {}, onBack = {}
                )
            }
        }
        compose.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
    }

    @Test
    fun zenBot_crisisMessage_showsHelplines() {
        compose.setContent { NeuroZenTheme { ZenBotScreen(onBack = {}, onSuggestion = {}, vm = ZenBotViewModel()) } }
        compose.onNodeWithTag("zenbot_input").performTextInput("ya no quiero vivir")
        compose.onNodeWithTag("zenbot_send").performClick()
        compose.onNodeWithTag("crisis_message").assertIsDisplayed()
        compose.onNodeWithText("Llamar 113").assertIsDisplayed()
    }
}
