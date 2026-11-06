package com.example.neurozen_front.presentation

import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Report
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.model.Session
import com.example.neurozen_front.domain.model.StressTrigger
import com.example.neurozen_front.domain.repository.AuthRepository
import com.example.neurozen_front.domain.repository.ReportRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.usecase.GetStressStatsUseCase
import com.example.neurozen_front.domain.usecase.ShareReportUseCase
import com.example.neurozen_front.presentation.auth.LoginViewModel
import com.example.neurozen_front.presentation.auth.RegisterViewModel
import com.example.neurozen_front.presentation.worker.ZenBotViewModel
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTests {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    // ----- Login -----

    @Test
    fun login_invalidEmail_showsErrorAndDoesNotCallBackend() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        val vm = LoginViewModel(auth)
        vm.onEmail("ana@")
        vm.onPassword("x")
        vm.submit()
        assertEquals("Ingresa un correo válido", vm.state.value.emailError)
        coVerify(exactly = 0) { auth.signIn(any(), any()) }
    }

    @Test
    fun login_wrongCredentials_showsBackendMessage() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        coEvery { auth.signIn(any(), any()) } returns Outcome.Failure(AppError.Unauthorized("Correo o contraseña incorrectos"))
        val vm = LoginViewModel(auth)
        vm.onEmail("ana@empresa.pe")
        vm.onPassword("mala")
        vm.submit()
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()
        assertFalse(vm.state.value.loading)
        assertEquals("Correo o contraseña incorrectos", vm.state.value.error)
    }

    @Test
    fun login_success_clearsPassword() = runTest(dispatcher) {
        val auth = mockk<AuthRepository>()
        coEvery { auth.signIn("ana@empresa.pe", "Clave2026") } returns Outcome.Success(Session("t", 1, "Ana", "ana@empresa.pe", Role.USER))
        val vm = LoginViewModel(auth)
        vm.onEmail("ana@empresa.pe")
        vm.onPassword("Clave2026")
        vm.submit()
        advanceUntilIdle()
        assertEquals("", vm.state.value.password)
        assertNull(vm.state.value.error)
    }

    // ----- Register -----

    @Test
    fun register_psychologistRequiresLicense_userRequiresHealthConsent() {
        val vm = RegisterViewModel(mockk())
        val base = vm.state.value.copy(fullName = "Ana Pérez", email = "ana@x.pe", password = "Clave2026", confirmPassword = "Clave2026", acceptedTerms = true)
        val psychologistErrors = vm.validate(base.copy(role = Role.PSYCHOLOGIST, specialty = "Clínica", licenseNumber = ""))
        assertTrue("license" in psychologistErrors)
        assertFalse("health" in psychologistErrors)
        val userErrors = vm.validate(base.copy(role = Role.USER, acceptedHealthData = false))
        assertTrue("health" in userErrors)
        assertTrue(vm.validate(base.copy(role = Role.USER, acceptedHealthData = true)).isEmpty())
    }

    // ----- ZenBot -----

    @Test
    fun zenBot_crisisMessageIsFlagged() {
        val vm = ZenBotViewModel()
        vm.onInput("no quiero vivir más")
        vm.send()
        val last = vm.ui.value.messages.last()
        assertTrue(last.isCrisis)
        assertEquals("", vm.ui.value.input)
    }

    // ----- Share report use case -----

    private fun appointment(status: AppointmentStatus) =
        Appointment(11, 5, null, null, 3, Instant.parse("2030-01-01T15:00:00Z"), 1, status, null, false)

    @Test
    fun shareReport_completedAppointment_isRejectedLocally() = runTest(dispatcher) {
        val stress = mockk<StressRepository>()
        val reports = mockk<ReportRepository>()
        val useCase = ShareReportUseCase(GetStressStatsUseCase(stress), reports)
        val result = useCase(appointment(AppointmentStatus.COMPLETED), null)
        assertTrue((result as Outcome.Failure).error is AppError.Validation)
        coVerify(exactly = 0) { reports.share(any(), any(), any(), any()) }
    }

    @Test
    fun shareReport_buildsStatsFromRealData() = runTest(dispatcher) {
        val stress = mockk<StressRepository>()
        val reports = mockk<ReportRepository>()
        coEvery { stress.getTriggers() } returns Outcome.Success(listOf(
            StressTrigger(1, "", "work", 7, "high", Instant.now().minusSeconds(3600)),
            StressTrigger(2, "", "work", 5, "medium", Instant.now().minusSeconds(7200))
        ))
        coEvery { stress.getAssessments() } returns Outcome.Success(emptyList())
        coEvery { reports.share(any(), any(), any(), any()) } returns Outcome.Success(mockk<Report>())
        val useCase = ShareReportUseCase(GetStressStatsUseCase(stress), reports)

        val result = useCase(appointment(AppointmentStatus.CONFIRMED), "  ")

        assertTrue(result is Outcome.Success)
        coVerify { reports.share(11, 3, match { it.triggersCount == 2 && it.current == 7.0 && it.average == 6.0 && it.topCategory == "work" }, null) }
    }
}
