package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.model.AppointmentStatus.CANCELLED
import com.example.neurozen_front.domain.model.AppointmentStatus.COMPLETED
import com.example.neurozen_front.domain.model.AppointmentStatus.CONFIRMED
import com.example.neurozen_front.domain.model.AppointmentStatus.PENDING
import com.example.neurozen_front.domain.model.Role
import java.time.Instant

/**
 * Appointment state machine, identical to the backend:
 * PENDING -> CONFIRMED | CANCELLED, CONFIRMED -> COMPLETED | CANCELLED.
 * CONFIRMED / COMPLETED only by the psychologist; CANCELLED by either participant.
 */
object AppointmentRules {

    fun allowedTransitions(current: AppointmentStatus, role: Role): Set<AppointmentStatus> = when (role) {
        Role.PSYCHOLOGIST -> when (current) {
            PENDING -> setOf(CONFIRMED, CANCELLED)
            CONFIRMED -> setOf(COMPLETED, CANCELLED)
            else -> emptySet()
        }
        Role.USER -> if (current == PENDING || current == CONFIRMED) setOf(CANCELLED) else emptySet()
        else -> emptySet()
    }

    fun canTransition(current: AppointmentStatus, target: AppointmentStatus, role: Role): Boolean =
        target in allowedTransitions(current, role)

    /** A report can be shared once per appointment while it is PENDING or CONFIRMED. */
    fun canShareReport(appointment: Appointment): Boolean =
        appointment.status == PENDING || appointment.status == CONFIRMED

    /** Only COMPLETED sessions that were not reviewed yet can be rated. */
    fun canReview(appointment: Appointment): Boolean =
        appointment.status == COMPLETED && !appointment.reviewed

    fun isUpcoming(appointment: Appointment, now: Instant): Boolean =
        appointment.dateTime.isAfter(now) && (appointment.status == PENDING || appointment.status == CONFIRMED)
}
