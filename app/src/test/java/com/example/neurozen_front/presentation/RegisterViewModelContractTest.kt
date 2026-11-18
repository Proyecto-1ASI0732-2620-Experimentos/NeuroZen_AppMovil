package com.example.neurozen_front.presentation

import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.repository.AuthRepository
import com.example.neurozen_front.presentation.auth.RegisterViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelContractTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun filled(vm: RegisterViewModel) = vm.update {
        it.copy(fullName = "Lucía Mendoza", dni = "12345678", email = "lucia@correo.com", password = "Secreta123",
            confirmPassword = "Secreta123", acceptedTerms = true, acceptedPrivacyPolicy = true, acceptedHealthData = true)
    }

    @Test
    fun consents_startUnchecked_andEachIsRequired() {
        val vm = RegisterViewModel(mockk())
        assertFalse(vm.state.value.acceptedTerms)
        assertFalse(vm.state.value.acceptedPrivacyPolicy)
        filled(vm)
        val noPrivacy = vm.validate(vm.state.value.copy(acceptedPrivacyPolicy = false))
        assertEquals("Debes aceptar la Política de Privacidad", noPrivacy["privacy"])
        assertFalse("terms" in noPrivacy)
        val noTerms = vm.validate(vm.state.value.copy(acceptedTerms = false))
        assertEquals("Debes aceptar los Términos y Condiciones", noTerms["terms"])
    }

    @Test
    fun dniAndSpecialty_areValidated() {
        val vm = RegisterViewModel(mockk())
        filled(vm)
        assertEquals("El DNI es obligatorio", vm.validate(vm.state.value.copy(dni = ""))["dni"])
        val pro = vm.validate(vm.state.value.copy(role = Role.PSYCHOLOGIST, specialty = "Terapia Cognitivo Conductual", licenseNumber = "1234567"))
        assertEquals("Selecciona una especialidad válida", pro["specialty"])
        assertEquals("El número de colegiatura debe tener entre 3 y 6 dígitos", pro["license"])
    }

    @Test
    fun conflict409Dni_isShownUnderDniField_andClearsWhenEdited() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        coEvery { auth.signUp(any()) } returns Outcome.Failure(AppError.Conflict("Ya existe una cuenta con este DNI"))
        val vm = RegisterViewModel(auth)
        filled(vm)
        vm.submit()
        advanceUntilIdle()
        assertEquals("Ya existe una cuenta con este DNI", vm.state.value.errors["dni"])
        assertNull(vm.state.value.error)
        vm.update { it.copy(dni = "87654321") }
        assertFalse("dni" in vm.state.value.errors)
        coVerify { auth.signUp(match { it.dni == "12345678" && it.acceptedPrivacyPolicy && it.confirmPassword == "Secreta123" }) }
    }

    @Test
    fun validation400_mapsEveryFieldError() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        coEvery { auth.signUp(any()) } returns Outcome.Failure(AppError.Validation("Datos inválidos", mapOf(
            "dni" to listOf("El DNI debe tener exactamente 8 dígitos numéricos"),
            "confirmPassword" to listOf("Las contraseñas no coinciden"),
            "acceptedPrivacyPolicy" to listOf("Debes aceptar la Política de Privacidad"),
            "FullName" to listOf("Ingresa tu nombre y apellido")
        )))
        val vm = RegisterViewModel(auth)
        filled(vm)
        vm.submit()
        advanceUntilIdle()
        val e = vm.state.value.errors
        assertEquals("El DNI debe tener exactamente 8 dígitos numéricos", e["dni"])
        assertEquals("Las contraseñas no coinciden", e["confirm"])
        assertEquals("Debes aceptar la Política de Privacidad", e["privacy"])
        assertEquals("Ingresa tu nombre y apellido", e["fullName"])
        assertEquals("Revisa los campos marcados.", vm.state.value.error)
    }

    @Test
    fun tooManyRequests429_showsGeneralMessage() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        coEvery { auth.signUp(any()) } returns Outcome.Failure(AppError.TooManyRequests())
        val vm = RegisterViewModel(auth)
        filled(vm)
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.error!!.startsWith("Demasiados intentos"))
        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test
    fun submit_withInvalidForm_showsErrors_andDoesNotCallBackend() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        val vm = RegisterViewModel(auth)
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.state.value.errors.isNotEmpty())
        assertFalse(vm.state.value.loading)
        coVerify(exactly = 0) { auth.signUp(any()) }
    }

    @Test
    fun submit_success_clearsPasswordsFromState() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        coEvery { auth.signUp(any()) } returns Outcome.Success(mockk(relaxed = true))
        val vm = RegisterViewModel(auth)
        filled(vm)
        vm.submit()
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertEquals("", vm.state.value.password)
        assertEquals("", vm.state.value.confirmPassword)
        assertTrue(vm.state.value.errors.isEmpty())
    }
}
