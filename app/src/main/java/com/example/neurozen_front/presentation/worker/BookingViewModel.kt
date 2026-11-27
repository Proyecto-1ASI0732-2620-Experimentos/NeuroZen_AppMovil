package com.example.neurozen_front.presentation.worker

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.BookingCalendar
import com.example.neurozen_front.domain.logic.BookingCalendarState
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.AppointmentType
import com.example.neurozen_front.domain.model.BookingRequest
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.getOrNull
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.usecase.BookAppointmentUseCase
import com.example.neurozen_front.presentation.components.MessageChannel
import com.example.neurozen_front.presentation.components.UiState
import com.example.neurozen_front.presentation.components.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class BookingUi(
    val step: Int = 1,
    val types: UiState<List<AppointmentType>> = UiState.Loading,
    val type: AppointmentType? = null,
    val calendar: UiState<BookingCalendarState> = UiState.Loading,
    val date: LocalDate? = null,
    val slots: UiState<List<String>> = UiState.Empty,
    val slot: String? = null,
    val notes: String = "",
    val submitting: Boolean = false,
    val booked: Boolean = false,
    /** Local notice from strings.xml, e.g. «Ese horario ya no está disponible…». */
    @param:StringRes val notice: Int? = null
)

/**
 * 4-step booking (type, day, hour, summary) with real availability (B2):
 * - only days with free hours are enabled (GET /appointments/availability; fallback: availabilitySchedule);
 * - date, hour or type can be changed from the summary without losing the rest;
 * - the hour is revalidated right before confirming and a 409 keeps every other field.
 */
@HiltViewModel
class BookingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val professionals: ProfessionalRepository,
    private val appointments: AppointmentRepository,
    private val book: BookAppointmentUseCase
) : ViewModel() {
    val professionalId: Long = savedState["id"] ?: 0L
    private val _ui = MutableStateFlow(BookingUi())
    val ui = _ui.asStateFlow()
    val messages = MessageChannel()

    init { loadTypes() }

    fun loadTypes() = viewModelScope.launch { _ui.update { it.copy(types = appointments.getTypes().toUiState()) } }

    private fun duration(): Int? = _ui.value.type?.durationMinutes

    fun selectType(type: AppointmentType) {
        val changed = _ui.value.type?.value != type.value
        _ui.update {
            // A different duration can change the free hours: the chosen hour is cleared, the date and notes are kept.
            if (changed) it.copy(type = type, step = 2, slot = null, notice = null) else it.copy(type = type, step = 2, notice = null)
        }
        if (changed || _ui.value.calendar !is UiState.Success) loadCalendar()
    }

    fun loadCalendar() = viewModelScope.launch {
        _ui.update { it.copy(calendar = UiState.Loading) }
        val today = DateTimeUtils.todayInLima()
        val end = BookingCalendar.endDate(today)
        val state = when (val r = appointments.getAvailability(professionalId, today, end, duration())) {
            is Outcome.Success -> BookingCalendar.fromAvailability(r.data, today)
            is Outcome.Failure -> {
                val schedule = professionals.getById(professionalId).getOrNull()?.schedule
                if (schedule != null) BookingCalendar.fromSchedule(schedule, today) else BookingCalendar.unverified(today)
            }
        }
        _ui.update { it.copy(calendar = UiState.Success(state)) }
    }

    fun selectDate(date: LocalDate) {
        _ui.update { it.copy(date = date, slot = null, slots = UiState.Loading, step = 3, notice = null) }
        loadSlots()
    }

    fun loadSlots() = viewModelScope.launch {
        val date = _ui.value.date ?: return@launch
        _ui.update { it.copy(slots = UiState.Loading) }
        val result = professionals.getAvailableSlots(professionalId, date, duration())
        _ui.update { it.copy(slots = result.toUiState()) }
    }

    fun selectSlot(slot: String) = _ui.update { it.copy(slot = slot, step = 4, notice = null) }

    fun back() = _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(1), notice = null) }

    /** From the summary: go back to one step keeping every other choice (type, date, notes). */
    fun changeType() = _ui.update { it.copy(step = 1, notice = null) }
    fun changeDate() {
        _ui.update { it.copy(step = 2, notice = null) }
        if (_ui.value.calendar !is UiState.Success) loadCalendar()
    }
    fun changeTime() {
        _ui.update { it.copy(step = 3, notice = null) }
        loadSlots()
    }

    fun updateNotes(v: String) = _ui.update { it.copy(notes = v.take(500)) }

    fun confirm() {
        val s = _ui.value
        if (s.submitting) return
        val type = s.type ?: return
        val date = s.date ?: return
        val slot = s.slot ?: return
        _ui.update { it.copy(submitting = true, notice = null) }
        viewModelScope.launch {
            // Revalidate the hour first. If this check fails (network), the server still validates on POST.
            val fresh = professionals.getAvailableSlots(professionalId, date, type.durationMinutes)
            if (fresh is Outcome.Success && slot !in fresh.data) {
                slotNoLongerAvailable(fresh.data)
                return@launch
            }
            when (val r = book(BookingRequest(professionalId, date, slot, type.value, s.notes))) {
                is Outcome.Success -> _ui.update { it.copy(submitting = false, booked = true) }
                is Outcome.Failure -> {
                    messages.send(r.error.message)
                    if (r.error is AppError.Conflict) {
                        _ui.update { it.copy(submitting = false, step = 3, slot = null, notice = R.string.booking_slot_taken) }
                        loadSlots()
                    } else {
                        _ui.update { it.copy(submitting = false) }
                    }
                }
            }
        }
    }

    private fun slotNoLongerAvailable(freshSlots: List<String>) = _ui.update {
        it.copy(
            submitting = false, step = 3, slot = null, notice = R.string.booking_slot_taken,
            slots = if (freshSlots.isEmpty()) UiState.Empty else UiState.Success(freshSlots)
        )
    }
}
