package com.example.neurozen_front.domain.logic

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime

/** Pure scheduling math for local reminders (active breaks, ergonomics and appointments). */
object ReminderMath {

    /**
     * Next reminder strictly after [now] on one of [days], inside [start]..[end],
     * every [everyMinutes] starting at [start]. Returns null when nothing is configured.
     */
    fun nextOccurrence(
        now: ZonedDateTime,
        days: Set<DayOfWeek>,
        start: LocalTime,
        end: LocalTime,
        everyMinutes: Int
    ): ZonedDateTime? {
        if (days.isEmpty() || everyMinutes <= 0 || !end.isAfter(start)) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek !in days) continue
            var candidate = date.atTime(start).atZone(now.zone)
            val limit = date.atTime(end).atZone(now.zone)
            while (!candidate.isAfter(limit)) {
                if (candidate.isAfter(now)) return candidate
                candidate = candidate.plusMinutes(everyMinutes.toLong())
            }
        }
        return null
    }

    /** One hour before the appointment, only if that moment is still in the future. */
    fun appointmentReminder(appointmentAt: Instant, now: Instant, before: Duration = Duration.ofHours(1)): Instant? {
        val at = appointmentAt.minus(before)
        return if (at.isAfter(now)) at else null
    }
}
