package com.example.neurozen_front

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.presentation.components.StatusPill
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.repository.LibraryRepository
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

/** Library without resources (instrumented tests have no mocking library). */
private object EmptyLibrary : LibraryRepository {
    override suspend fun getAll(): Outcome<List<LibraryResource>> = Outcome.Success(emptyList())
    override suspend fun getById(id: Long): Outcome<LibraryResource> = Outcome.Failure(AppError.NotFound("No encontrado"))
    override suspend fun search(query: String, limit: Int): Outcome<List<LibraryResource>> = Outcome.Success(emptyList())
}

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
        // Updated: the ViewModel now needs the library (resource cards). A crisis message never queries it.
        val vm = ZenBotViewModel(EmptyLibrary)
        compose.setContent { NeuroZenTheme { ZenBotScreen(onBack = {}, onSuggestion = {}, vm = vm) } }
        compose.onNodeWithTag("zenbot_input").performTextInput("ya no quiero vivir")
        compose.onNodeWithTag("zenbot_send").performClick()
        compose.onNodeWithTag("crisis_message").assertIsDisplayed()
        compose.onNodeWithText("Llamar 113").assertIsDisplayed()
    }

    @Test
    fun zenBot_showsWelcomeAndQuickOptions() {
        val vm = ZenBotViewModel(EmptyLibrary)
        compose.setContent { NeuroZenTheme { ZenBotScreen(onBack = {}, onSuggestion = {}, vm = vm) } }
        compose.onNodeWithText("Ver mis citas").assertIsDisplayed()
    }

    @Test
    fun statusPill_readsStateForTalkBack() {
        compose.setContent { NeuroZenTheme { StatusPill(AppointmentStatus.CONFIRMED) } }
        compose.onNodeWithContentDescription("Estado: Confirmada").assertIsDisplayed()
    }
}
