package com.example.neurozen_front.domain.usecase

import com.example.neurozen_front.domain.logic.AppointmentRules
import com.example.neurozen_front.domain.logic.StressStatsCalculator
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.BookingRequest
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.model.Report
import com.example.neurozen_front.domain.model.StressStats
import com.example.neurozen_front.domain.repository.AppointmentRepository
import com.example.neurozen_front.domain.repository.ReportRepository
import com.example.neurozen_front.domain.repository.SessionRepository
import com.example.neurozen_front.domain.repository.StressRepository
import com.example.neurozen_front.domain.repository.WellnessRepository
import java.time.Instant
import javax.inject.Inject

/** Hook to cancel scheduled reminders without the domain knowing about Android. */
interface ReminderCanceller {
    fun cancelAll()
}

/** Logout or account deletion: clears the session, local Room data and reminders. */
class LogoutUseCase @Inject constructor(
    private val session: SessionRepository,
    private val wellness: WellnessRepository,
    private val reminders: ReminderCanceller
) {
    suspend operator fun invoke() {
        reminders.cancelAll()
        wellness.clearAll()
        session.clear()
    }
}

class GetStressStatsUseCase @Inject constructor(private val stress: StressRepository) {
    suspend operator fun invoke(now: Instant = Instant.now()): Outcome<StressStats> {
        val triggers = when (val r = stress.getTriggers()) {
            is Outcome.Success -> r.data
            is Outcome.Failure -> return r
        }
        val assessments = when (val r = stress.getAssessments()) {
            is Outcome.Success -> r.data
            is Outcome.Failure -> return r
        }
        return Outcome.Success(StressStatsCalculator.calculate(triggers, assessments, now))
    }
}

/** Builds the progress report from the user's real data and shares it on an appointment. */
class ShareReportUseCase @Inject constructor(
    private val stats: GetStressStatsUseCase,
    private val reports: ReportRepository
) {
    suspend operator fun invoke(appointment: Appointment, note: String?): Outcome<Report> {
        if (!AppointmentRules.canShareReport(appointment)) {
            return Outcome.Failure(AppError.Validation("Solo puedes compartir un informe en citas pendientes o confirmadas."))
        }
        val computed = when (val r = stats()) {
            is Outcome.Success -> r.data
            is Outcome.Failure -> return r
        }
        return reports.share(appointment.id, appointment.professionalId, computed, note?.takeIf { it.isNotBlank() })
    }
}

class BookAppointmentUseCase @Inject constructor(private val appointments: AppointmentRepository) {
    suspend operator fun invoke(request: BookingRequest): Outcome<Appointment> {
        if (request.slot.isBlank()) return Outcome.Failure(AppError.Validation("Elige un horario."))
        if (request.type !in 1..3) return Outcome.Failure(AppError.Validation("Elige el tipo de sesión."))
        if ((request.notes?.length ?: 0) > 500) return Outcome.Failure(AppError.Validation("La nota no puede superar 500 caracteres."))
        return appointments.book(request)
    }
}
