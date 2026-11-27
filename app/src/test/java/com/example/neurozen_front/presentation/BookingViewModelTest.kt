package com.example.neurozen_front.presentation

import androidx.lifecycle.SavedStateHandle
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.CalendarSource
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AppointmentType
import com.example.neurozen_front.domain.model.AvailabilityCalendar
import com.example.neurozen_front.domain.model.AvailabilitySchedule
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Professional
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.usecase.BookAppointmentUseCase
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.worker.BookingViewModel
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
import java.time.LocalDate

/** B2: real availability, change from the summary, revalidation and 409 without losing data. */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val professionals = mockk<ProfessionalRepository>()
    private val appointments = mockk<AppointmentRepository>()
    private val type60 = AppointmentType(1, "Individual", "", 60)
    private val type30 = AppointmentType(3, "Seguimiento", "", 30)
    private val day = LocalDate.of(2030, 1, 7)

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { appointments.getTypes() } returns Outcome.Success(listOf(type60, type30))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm() = BookingViewModel(SavedStateHandle(mapOf("id" to 3L)), professionals, appointments, BookAppointmentUseCase(appointments))

    private fun appointment() = Appointment(1, 5, null, null, 3, Instant.parse("2030-01-07T15:00:00Z"), 1, AppointmentStatus.PENDING, null, false)

    /** Goes to the summary: type 60, day, 10:00, with notes. */
    private fun kotlinx.coroutines.test.TestScope.toSummary(vm: BookingViewModel) {
        coEvery { appointments.getAvailability(3, any(), any(), any()) } returns
            Outcome.Success(AvailabilityCalendar("America/Lima", 60, emptyMap(), day))
        coEvery { professionals.getAvailableSlots(3, day, 60) } returns Outcome.Success(listOf("09:00", "10:00"))
        advanceUntilIdle()
        vm.selectType(type60)
        advanceUntilIdle()
        vm.selectDate(day)
        advanceUntilIdle()
        vm.selectSlot("10:00")
        vm.updateNotes("Primera vez")
    }

    @Test
    fun selectType_loadsBackendCalendar_withTheTypeDuration() = runTest(dispatcher) {
        coEvery { appointments.getAvailability(3, any(), any(), 30) } returns
            Outcome.Success(AvailabilityCalendar("America/Lima", 30, mapOf(day to listOf("09:00")), day))
        val vm = vm()
        advanceUntilIdle()
        assertTrue(vm.ui.value.types is UiState.Success)
        vm.selectType(type30)
        assertEquals(2, vm.ui.value.step)
        advanceUntilIdle()
        val calendar = (vm.ui.value.calendar as UiState.Success).data
        assertEquals(CalendarSource.BACKEND, calendar.source)
        assertEquals(day, calendar.nextAvailable)
    }

    @Test
    fun availabilityFails_fallsBackToSchedule_thenToUnverified() = runTest(dispatcher) {
        coEvery { appointments.getAvailability(any(), any(), any(), any()) } returns Outcome.Failure(AppError.Network())
        coEvery { professionals.getById(3) } returns Outcome.Success(
            Professional(3, 9, "Ana", "clinical", "", null, null, 0.0, 0, "", null, AvailabilitySchedule(listOf("monday"), "09:00", "12:00"))
        )
        val vm = vm()
        vm.selectType(type60)
        advanceUntilIdle()
        assertEquals(CalendarSource.SCHEDULE, (vm.ui.value.calendar as UiState.Success).data.source)

        coEvery { professionals.getById(3) } returns Outcome.Failure(AppError.Network())
        vm.loadCalendar()
        advanceUntilIdle()
        val calendar = (vm.ui.value.calendar as UiState.Success).data
        assertEquals(CalendarSource.UNVERIFIED, calendar.source)
        assertTrue(calendar.days.all { it.enabled })
    }

    @Test
    fun summary_canChangeDateOrTime_keepingTypeAndNotes() = runTest(dispatcher) {
        val vm = vm()
        toSummary(vm)
        assertEquals(4, vm.ui.value.step)

        vm.changeDate()
        assertEquals(2, vm.ui.value.step)
        assertEquals(type60, vm.ui.value.type)
        assertEquals("Primera vez", vm.ui.value.notes)

        vm.changeTime()
        assertEquals(3, vm.ui.value.step)
        advanceUntilIdle()
        assertEquals(listOf("09:00", "10:00"), (vm.ui.value.slots as UiState.Success).data)

        vm.changeType()
        assertEquals(1, vm.ui.value.step)
        vm.selectType(type60) // same type: keeps the chosen hour and does not reload the calendar
        assertEquals("10:00", vm.ui.value.slot)
        coVerify(exactly = 1) { appointments.getAvailability(any(), any(), any(), any()) }
    }

    @Test
    fun changingToAnotherDuration_clearsHour_keepsDateAndNotes() = runTest(dispatcher) {
        val vm = vm()
        toSummary(vm)
        coEvery { appointments.getAvailability(3, any(), any(), 30) } returns Outcome.Success(AvailabilityCalendar("America/Lima", 30, emptyMap(), null))
        vm.changeType()
        vm.selectType(type30)
        advanceUntilIdle()
        assertNull(vm.ui.value.slot)
        assertEquals(day, vm.ui.value.date)
        assertEquals("Primera vez", vm.ui.value.notes)
    }

    @Test
    fun confirm_revalidatesHour_andBooks() = runTest(dispatcher) {
        coEvery { appointments.book(any()) } returns Outcome.Success(appointment())
        val vm = vm()
        toSummary(vm)
        vm.confirm()
        assertTrue(vm.ui.value.submitting)
        advanceUntilIdle()
        assertTrue(vm.ui.value.booked)
        coVerify { appointments.book(match { it.slot == "10:00" && it.notes == "Primera vez" && it.type == 1 }) }
    }

    @Test
    fun confirm_hourNoLongerFree_goesBackToHours_withoutPosting() = runTest(dispatcher) {
        val vm = vm()
        toSummary(vm)
        coEvery { professionals.getAvailableSlots(3, day, 60) } returns Outcome.Success(listOf("11:00"))
        vm.confirm()
        advanceUntilIdle()
        val ui = vm.ui.value
        assertEquals(3, ui.step)
        assertNull(ui.slot)
        assertEquals(R.string.booking_slot_taken, ui.notice)
        assertEquals(listOf("11:00"), (ui.slots as UiState.Success).data)
        assertEquals("Primera vez", ui.notes)
        assertFalse(ui.submitting)
        coVerify(exactly = 0) { appointments.book(any()) }

        coEvery { professionals.getAvailableSlots(3, day, 60) } returns Outcome.Success(emptyList())
        vm.selectSlot("11:00")
        vm.confirm()
        advanceUntilIdle()
        assertEquals(UiState.Empty, vm.ui.value.slots)
    }

    @Test
    fun confirm_409_keepsData_andAsksForAnotherHour() = runTest(dispatcher) {
        coEvery { appointments.book(any()) } returns Outcome.Failure(AppError.Conflict("El horario ya no está disponible"))
        val vm = vm()
        toSummary(vm)
        vm.confirm()
        advanceUntilIdle()
        val ui = vm.ui.value
        assertEquals(3, ui.step)
        assertNull(ui.slot)
        assertEquals(type60, ui.type)
        assertEquals(day, ui.date)
        assertEquals("Primera vez", ui.notes)
        assertEquals(R.string.booking_slot_taken, ui.notice)
        assertFalse(ui.booked)
    }

    @Test
    fun confirm_revalidationFails_stillPosts_andShowsOtherErrors() = runTest(dispatcher) {
        val vm = vm()
        toSummary(vm)
        coEvery { professionals.getAvailableSlots(3, day, 60) } returns Outcome.Failure(AppError.Network())
        coEvery { appointments.book(any()) } returns Outcome.Failure(AppError.Validation("La fecha no puede estar en el pasado"))
        vm.confirm()
        advanceUntilIdle()
        assertEquals(4, vm.ui.value.step)
        assertFalse(vm.ui.value.submitting)
        coVerify(exactly = 1) { appointments.book(any()) }
    }

    @Test
    fun confirm_incompleteOrTwice_isIgnored() = runTest(dispatcher) {
        val vm = vm()
        advanceUntilIdle()
        vm.confirm()
        advanceUntilIdle()
        coVerify(exactly = 0) { appointments.book(any()) }
        vm.back()
        assertEquals(1, vm.ui.value.step)
    }
}
