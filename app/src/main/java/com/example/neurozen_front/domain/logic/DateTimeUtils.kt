package com.example.neurozen_front.domain.logic

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** The backend stores UTC; the UI and available slots use Lima time (UTC-5, no DST). */
object DateTimeUtils {
    val LIMA: ZoneId = ZoneId.of("America/Lima")
    val SPANISH: Locale = Locale.forLanguageTag("es-PE")

    private val dateFormatter = DateTimeFormatter.ofPattern("EEE d 'de' MMM, yyyy", SPANISH)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", SPANISH)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", SPANISH)

    /** Tolerant parser: accepts "...Z", offsets, or a bare local date-time (treated as UTC). */
    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }
            .recoverCatching { OffsetDateTime.parse(value).toInstant() }
            .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
            .recoverCatching { LocalDate.parse(value.take(10)).atStartOfDay().toInstant(ZoneOffset.UTC) }
            .getOrNull()
    }

    /** "2026-11-10" + "10:00" (Lima) -> "2026-11-10T15:00:00Z". */
    fun limaSlotToUtcIso(date: LocalDate, slot: String): String {
        val time = LocalTime.parse(slot)
        return date.atTime(time).atZone(LIMA).toInstant().toString()
    }

    fun toLima(instant: Instant): LocalDateTime = instant.atZone(LIMA).toLocalDateTime()

    fun todayInLima(now: Instant = Instant.now()): LocalDate = now.atZone(LIMA).toLocalDate()

    fun isSameLimaDay(instant: Instant, day: LocalDate): Boolean = instant.atZone(LIMA).toLocalDate() == day

    fun formatDate(date: LocalDate): String = date.format(dateFormatter).replaceFirstChar { it.uppercase() }

    fun formatDateTime(instant: Instant): String =
        instant.atZone(LIMA).format(dateTimeFormatter).replaceFirstChar { it.uppercase() }

    fun formatTime(instant: Instant): String = instant.atZone(LIMA).format(timeFormatter)

    fun dayName(day: DayOfWeek): String =
        day.getDisplayName(TextStyle.FULL, SPANISH).replaceFirstChar { it.uppercase() }

    fun dayShort(day: DayOfWeek): String =
        day.getDisplayName(TextStyle.SHORT, SPANISH).replaceFirstChar { it.uppercase() }.removeSuffix(".")
}
