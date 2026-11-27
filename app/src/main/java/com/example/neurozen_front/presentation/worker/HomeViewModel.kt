package com.example.neurozen_front.presentation.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.neurozen_front.domain.logic.AppointmentRules
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.Recommendation
import com.example.neurozen_front.domain.logic.RecommendationEngine
import com.example.neurozen_front.domain.logic.StressStatsCalculator
import com.example.neurozen_front.domain.logic.StressSummary
import com.example.neurozen_front.domain.logic.StressSummaryBuilder
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.StressLevel
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.model.getOrNull
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ProfessionalRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.repository.WellnessRepository
import com.example.neurozen_front.notifications.ReminderKind
import com.example.neurozen_front.notifications.ReminderScheduler
import com.example.neurozen_front.presentation.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

data class HomeData(
    val firstName: String,
    val stats: StressStats,
    val level: StressLevel?,
    val recommendations: List<Recommendation>,
    val nextAppointment: Appointment?,
    val nextAppointmentProfessional: String?,
    val nextBreak: ZonedDateTime?,
    val weeklyHours: Double,
    /** null = the user has no PSS-10 nor trigger yet: Home shows the welcome state (never sample data). */
    val summary: StressSummary? = null
) {
    val hasStressData: Boolean get() = summary != null
}

/**
 * Home dashboard. Loading, error (with retry) and "no data yet" are distinct states:
 * - [UiState.Loading] while the requests run;
 * - [UiState.Error] when the stress data cannot be read (the backend message is shown with «Reintentar»);
 * - [UiState.Success] with [HomeData.summary] = null when the user has no PSS-10 nor trigger.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val stress: StressRepository,
    private val appointments: AppointmentRepository,
    private val professionals: ProfessionalRepository,
    private val wellness: WellnessRepository,
    private val reminders: ReminderScheduler
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val triggersDeferred = async { stress.getTriggers() }
            val assessmentsDeferred = async { stress.getAssessments() }
            val appointmentsDeferred = async { appointments.getMine() }
            val triggers = triggersDeferred.await()
            val assessments = assessmentsDeferred.await()
            if (triggers is Outcome.Failure) { _state.value = UiState.Error(triggers.error.message); return@launch }
            if (assessments is Outcome.Failure) { _state.value = UiState.Error(assessments.error.message); return@launch }
            val triggerList = triggers.getOrNull().orEmpty()
            val assessmentList = assessments.getOrNull().orEmpty()
            val now = Instant.now()
            val stats = StressStatsCalculator.calculate(triggerList, assessmentList, now)

            // Appointments are optional on Home: a failure there should not hide the dashboard.
            val myAppointments = appointmentsDeferred.await().getOrNull().orEmpty()
            reminders.syncAppointments(myAppointments)
            val next = myAppointments.filter { AppointmentRules.isUpcoming(it, now) }.minByOrNull { it.dateTime }
            val professionalName = next?.let { professionals.getById(it.professionalId).getOrNull()?.name }

            val weekStart = now.atZone(DateTimeUtils.LIMA).toLocalDate().minusDays(6)
            val weeklyHours = wellness.observeWorkload().first().filter { !it.date.isBefore(weekStart) }.sumOf { it.hours }

            val level = assessmentList.firstOrNull()?.level
                ?: triggerList.firstOrNull()?.let { StressLevel.fromTriggerLevel(it.stressLevel) }
            val topCategory = stats.topCategory.takeIf { stats.triggersCount > 0 }

            _state.value = UiState.Success(
                HomeData(
                    firstName = sessions.session.value?.fullName?.substringBefore(' ').orEmpty(),
                    stats = stats,
                    level = level,
                    recommendations = RecommendationEngine.recommend(level, topCategory, weeklyHours, assessmentList.isNotEmpty()).take(3),
                    nextAppointment = next,
                    nextAppointmentProfessional = professionalName,
                    nextBreak = reminders.scheduleNext(ReminderKind.BREAK),
                    weeklyHours = weeklyHours,
                    summary = StressSummaryBuilder.build(triggerList, assessmentList)
                )
            )
        }
    }
}
