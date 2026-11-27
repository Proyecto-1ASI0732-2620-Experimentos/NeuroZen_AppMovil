package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.AvailabilityCalendar
import com.example.neurozen_front.domain.model.AvailabilitySchedule
import java.time.LocalDate

/** Where the enabled days come from. */
enum class CalendarSource {
    /** GET /appointments/availability: only days with free slots are enabled. */
    BACKEND,
    /** Fallback: weekly availabilitySchedule of the professional (free hours are checked when the day is chosen). */
    SCHEDULE,
    /** Neither source answered: every day is enabled and the slots request decides. */
    UNVERIFIED
}

data class BookingDay(val date: LocalDate, val enabled: Boolean)

data class BookingCalendarState(val days: List<BookingDay>, val nextAvailable: LocalDate?, val source: CalendarSource) {
    val hasEnabledDays: Boolean get() = days.any { it.enabled }
}

/** Builds the booking calendar (B2). Pure, so it is unit tested. */
object BookingCalendar {
    /** Days shown in the calendar, today included (the backend accepts up to 92). */
    const val WINDOW_DAYS = 30

    fun endDate(today: LocalDate, windowDays: Int = WINDOW_DAYS): LocalDate = today.plusDays((windowDays - 1).toLong())

    private fun window(today: LocalDate, windowDays: Int): List<LocalDate> =
        (0 until windowDays.coerceAtLeast(1)).map { today.plusDays(it.toLong()) }

    fun fromAvailability(calendar: AvailabilityCalendar, today: LocalDate, windowDays: Int = WINDOW_DAYS): BookingCalendarState {
        val days = window(today, windowDays).map { BookingDay(it, calendar.days[it].orEmpty().isNotEmpty()) }
        val next = calendar.nextAvailableDate?.takeUnless { it.isBefore(today) } ?: days.firstOrNull { it.enabled }?.date
        return BookingCalendarState(days, next, CalendarSource.BACKEND)
    }

    fun fromSchedule(schedule: AvailabilitySchedule, today: LocalDate, windowDays: Int = WINDOW_DAYS): BookingCalendarState {
        val weekdays = schedule.days.mapNotNull(DayCodec::parse).toSet()
        val days = window(today, windowDays).map { BookingDay(it, it.dayOfWeek in weekdays) }
        return BookingCalendarState(days, days.firstOrNull { it.enabled }?.date, CalendarSource.SCHEDULE)
    }

    fun unverified(today: LocalDate, windowDays: Int = WINDOW_DAYS): BookingCalendarState =
        BookingCalendarState(window(today, windowDays).map { BookingDay(it, true) }, null, CalendarSource.UNVERIFIED)
}
