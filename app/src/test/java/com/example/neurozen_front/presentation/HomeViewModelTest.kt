package com.example.neurozen_front.presentation

import com.example.neurozen_front.domain.logic.StressSource
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.Assessment
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.model.Role
import com.example.neurozen_front.domain.model.Session
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressTrigger
import com.example.neurozen_front.domain.model.WorkloadEntry
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.repository.WellnessRepository
import com.example.neurozen_front.notifications.ReminderScheduler
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.worker.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.time.LocalDate

/** A1: Home distinguishes loading, error, no data and data; never sample data. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sessions = mockk<SessionRepository> {
        every { session } returns MutableStateFlow(Session("t", 5, "Ana Pérez", "ana@x.pe", Role.USER))
    }
    private val stress = mockk<StressRepository>()
    private val appointments = mockk<AppointmentRepository>()
    private val professionals = mockk<ProfessionalRepository>()
    private val wellness = mockk<WellnessRepository>()
    private val reminders = mockk<ReminderScheduler>(relaxed = true) { every { scheduleNext(any(), any()) } returns null }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { appointments.getMine() } returns Outcome.Success(emptyList())
        every { wellness.observeWorkload() } returns flowOf(emptyList())
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = HomeViewModel(sessions, stress, appointments, professionals, wellness, reminders)

    @Test
    fun startsLoading_thenNoData_showsWelcomeState() = runTest(dispatcher) {
        coEvery { stress.getTriggers() } returns Outcome.Success(emptyList())
        coEvery { stress.getAssessments() } returns Outcome.Success(emptyList())
        val vm = vm()
        assertEquals(UiState.Loading, vm.state.value)
        advanceUntilIdle()
        val data = (vm.state.value as UiState.Success).data
        assertNull(data.summary)
        assertFalse(data.hasStressData)
        assertEquals("Ana", data.firstName)
        assertEquals(0, data.stats.triggersCount)
    }

    @Test
    fun triggersFail_showsErrorWithBackendMessage_andRetryReloads() = runTest(dispatcher) {
        coEvery { stress.getTriggers() } returns Outcome.Failure(AppError.Network())
        coEvery { stress.getAssessments() } returns Outcome.Success(emptyList())
        val vm = vm()
        advanceUntilIdle()
        assertEquals(UiState.Error(AppError.Network().message), vm.state.value)

        coEvery { stress.getTriggers() } returns Outcome.Success(emptyList())
        vm.load()
        assertEquals(UiState.Loading, vm.state.value)
        advanceUntilIdle()
        assertTrue(vm.state.value is UiState.Success)
    }

    @Test
    fun assessmentsFail_showsError() = runTest(dispatcher) {
        coEvery { stress.getTriggers() } returns Outcome.Success(emptyList())
        coEvery { stress.getAssessments() } returns Outcome.Failure(AppError.Server("El servidor tuvo un problema."))
        val vm = vm()
        advanceUntilIdle()
        assertEquals(UiState.Error("El servidor tuvo un problema."), vm.state.value)
    }

    @Test
    fun withData_buildsSummaryOnTheSingleScale_andNextAppointment() = runTest(dispatcher) {
        val now = Instant.now()
        coEvery { stress.getTriggers() } returns Outcome.Success(listOf(StressTrigger(1, "", "work", 8, "high", now.minusSeconds(600))))
        coEvery { stress.getAssessments() } returns Outcome.Success(listOf(Assessment(1, List(10) { 2 }, 26, StressLevel.MODERATE, now.minusSeconds(86_400))))
        val upcoming = Appointment(9, 5, null, null, 3, now.plusSeconds(86_400), 1, AppointmentStatus.CONFIRMED, null, false)
        coEvery { appointments.getMine() } returns Outcome.Success(listOf(upcoming))
        coEvery { professionals.getById(3) } returns Outcome.Success(Professional(3, 9, "Dra. Ruiz", "clinical", "", 5, null, 0.0, 0, "", null))
        every { wellness.observeWorkload() } returns flowOf(listOf(WorkloadEntry(1, LocalDate.now(), 9.5, "")))

        val vm = vm()
        advanceUntilIdle()
        val data = (vm.state.value as UiState.Success).data
        val summary = data.summary!!
        assertEquals(80, summary.current.index)
        assertEquals(StressSource.TRIGGER, summary.current.source)
        assertEquals(StressLevel.HIGH, summary.level)
        assertEquals(65, summary.lastAssessment!!.index)
        assertEquals(26, summary.lastAssessment!!.rawValue)
        assertEquals(2, summary.evolution.size)
        assertEquals("Dra. Ruiz", data.nextAppointmentProfessional)
        assertEquals(9.5, data.weeklyHours, 0.0)
        assertEquals(StressLevel.MODERATE, data.level) // recommendations still use the latest test level
        verify { reminders.syncAppointments(listOf(upcoming), any()) }
    }

    @Test
    fun appointmentsFailure_doesNotHideTheDashboard() = runTest(dispatcher) {
        coEvery { stress.getTriggers() } returns Outcome.Success(listOf(StressTrigger(1, "", "work", 3, "low", Instant.now())))
        coEvery { stress.getAssessments() } returns Outcome.Success(emptyList())
        coEvery { appointments.getMine() } returns Outcome.Failure(AppError.Network())
        val vm = vm()
        advanceUntilIdle()
        val data = (vm.state.value as UiState.Success).data
        assertNull(data.nextAppointment)
        assertEquals(30, data.summary!!.current.index)
    }
}
