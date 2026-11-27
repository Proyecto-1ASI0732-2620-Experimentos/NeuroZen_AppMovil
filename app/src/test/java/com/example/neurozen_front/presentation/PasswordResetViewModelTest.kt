package com.example.neurozen_front.presentation

import com.example.neurozen_front.BuildConfig
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.repository.AuthRepository
import com.example.neurozen_front.presentation.auth.PasswordViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** A3: the user can paste the whole link; the token is validated before calling reset-password. */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val auth = mockk<AuthRepository>()
    private val token = "0123456789abcdefABCDEF"

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun pastedLink_extractsToken_andResets() = runTest(dispatcher) {
        coEvery { auth.resetPassword(token, "Clave2026") } returns Outcome.Success("ok")
        val vm = PasswordViewModel(auth)
        vm.update { it.copy(token = "https://${BuildConfig.RESET_LINK_HOST}/reset-password?token=$token", password = "Clave2026", confirm = "Clave2026") }
        vm.reset()
        advanceUntilIdle()
        assertEquals("ok", vm.state.value.success)
        assertEquals("", vm.state.value.token)
        coVerify { auth.resetPassword(token, "Clave2026") }
    }

    @Test
    fun missingOrInvalidLink_showsLocalMessage_withoutCallingBackend() = runTest(dispatcher) {
        val vm = PasswordViewModel(auth)
        vm.update { it.copy(password = "Clave2026", confirm = "Clave2026") }
        vm.reset()
        assertEquals(R.string.reset_link_missing, vm.state.value.errorRes)
        vm.update { it.copy(token = "https://otro-sitio.com/reset-password?token=$token") }
        assertNull(vm.state.value.errorRes)
        vm.reset()
        assertEquals(R.string.reset_link_invalid, vm.state.value.errorRes)
        coVerify(exactly = 0) { auth.resetPassword(any(), any()) }
    }

    @Test
    fun weakPassword_orMismatch_areReported() = runTest(dispatcher) {
        val vm = PasswordViewModel(auth)
        vm.update { it.copy(token = token, password = "corta", confirm = "corta") }
        vm.reset()
        assertEquals("La contraseña debe tener al menos 8 caracteres", vm.state.value.error)
        vm.update { it.copy(password = "Clave2026", confirm = "Clave2027") }
        vm.reset()
        assertEquals(R.string.reset_mismatch, vm.state.value.errorRes)
    }

    @Test
    fun backendError_isShown() = runTest(dispatcher) {
        coEvery { auth.resetPassword(any(), any()) } returns Outcome.Failure(AppError.Validation("El enlace expiró"))
        val vm = PasswordViewModel(auth)
        vm.update { it.copy(token = token, password = "Clave2026", confirm = "Clave2026") }
        vm.reset()
        advanceUntilIdle()
        assertEquals("El enlace expiró", vm.state.value.error)
    }
}
