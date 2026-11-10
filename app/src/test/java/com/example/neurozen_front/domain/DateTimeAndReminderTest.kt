package com.example.neurozen_front.domain

import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.ReminderMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class DateTimeAndReminderTest {

    @Test
    fun limaSlot_isConvertedToUtc() {
        assertEquals("2026-11-10T15:00:00Z", DateTimeUtils.limaSlotToUtcIso(LocalDate.of(2026, 11, 10), "10:00"))
        assertEquals("2026-11-11T04:30:00Z", DateTimeUtils.limaSlotToUtcIso(LocalDate.of(2026, 11, 10), "23:30"))
    }

    @Test
    fun parseInstant_acceptsBackendFormats() {
        val expected = Instant.parse("2026-11-10T15:00:00Z")
        assertEquals(expected, DateTimeUtils.parseInstant("2026-11-10T15:00:00Z"))
        assertEquals(expected, DateTimeUtils.parseInstant("2026-11-10T10:00:00-05:00"))
        assertEquals(expected, DateTimeUtils.parseInstant("2026-11-10T15:00:00"))
        assertEquals(expected, DateTimeUtils.parseInstant("2026-11-10T15:00:00.000Z"))
        assertNull(DateTimeUtils.parseInstant("no es fecha"))
        assertNull(DateTimeUtils.parseInstant(null))
    }

    @Test
    fun utcToLima_isMinusFiveHours() {
        val lima = DateTimeUtils.toLima(Instant.parse("2026-11-10T03:00:00Z"))
        assertEquals(LocalDate.of(2026, 11, 9), lima.toLocalDate())
        assertEquals(22, lima.hour)
    }

    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

    @Test
    fun nextBreak_sameDayNextSlot() {
        // Tuesday 10:15 -> 11:00 with breaks every 60 min from 09:00
        val now = ZonedDateTime.of(2026, 11, 10, 10, 15, 0, 0, DateTimeUtils.LIMA)
        val next = ReminderMath.nextOccurrence(now, weekdays, LocalTime.of(9, 0), LocalTime.of(18, 0), 60)
        assertEquals(ZonedDateTime.of(2026, 11, 10, 11, 0, 0, 0, DateTimeUtils.LIMA), next)
    }

    @Test
    fun nextBreak_afterWorkDay_jumpsToNextWorkingDay() {
        // Friday 19:00 -> Monday 09:00
        val now = ZonedDateTime.of(2026, 11, 13, 19, 0, 0, 0, DateTimeUtils.LIMA)
        val next = ReminderMath.nextOccurrence(now, weekdays, LocalTime.of(9, 0), LocalTime.of(18, 0), 60)
        assertEquals(ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, DateTimeUtils.LIMA), next)
    }

    @Test
    fun nextBreak_exactlyOnSlot_returnsFollowingOne() {
        val now = ZonedDateTime.of(2026, 11, 10, 9, 0, 0, 0, DateTimeUtils.LIMA)
        val next = ReminderMath.nextOccurrence(now, weekdays, LocalTime.of(9, 0), LocalTime.of(18, 0), 45)
        assertEquals(ZonedDateTime.of(2026, 11, 10, 9, 45, 0, 0, DateTimeUtils.LIMA), next)
    }

    @Test
    fun nextBreak_invalidConfig_returnsNull() {
        val now = ZonedDateTime.of(2026, 11, 10, 9, 0, 0, 0, DateTimeUtils.LIMA)
        assertNull(ReminderMath.nextOccurrence(now, emptySet(), LocalTime.of(9, 0), LocalTime.of(18, 0), 60))
        assertNull(ReminderMath.nextOccurrence(now, weekdays, LocalTime.of(18, 0), LocalTime.of(9, 0), 60))
        assertNull(ReminderMath.nextOccurrence(now, weekdays, LocalTime.of(9, 0), LocalTime.of(18, 0), 0))
    }

    @Test
    fun appointmentReminder_oneHourBefore_onlyIfFuture() {
        val at = Instant.parse("2026-11-10T15:00:00Z")
        assertEquals(Instant.parse("2026-11-10T14:00:00Z"), ReminderMath.appointmentReminder(at, Instant.parse("2026-11-10T10:00:00Z")))
        assertNull(ReminderMath.appointmentReminder(at, Instant.parse("2026-11-10T14:30:00Z")))
    }
}
