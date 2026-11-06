package com.example.neurozen_front.domain.logic

import java.time.DayOfWeek

/**
 * Availability days travel as strings. The swagger does not fix the format, so we keep whatever style
 * the backend already returned (English full/short or Spanish) and default to English uppercase.
 */
object DayCodec {
    enum class Style { ENGLISH_FULL, ENGLISH_SHORT, SPANISH }

    private val spanish = mapOf(
        DayOfWeek.MONDAY to "Lunes", DayOfWeek.TUESDAY to "Martes", DayOfWeek.WEDNESDAY to "Miércoles",
        DayOfWeek.THURSDAY to "Jueves", DayOfWeek.FRIDAY to "Viernes", DayOfWeek.SATURDAY to "Sábado", DayOfWeek.SUNDAY to "Domingo"
    )

    fun parse(value: String): DayOfWeek? {
        val v = CrisisDetector.normalize(value)
        DayOfWeek.entries.firstOrNull { it.name.equals(v, true) || it.name.take(3).equals(v, true) }?.let { return it }
        return spanish.entries.firstOrNull { CrisisDetector.normalize(it.value) == v || CrisisDetector.normalize(it.value).take(3) == v }?.key
    }

    fun detectStyle(existing: List<String>): Style {
        val first = existing.firstOrNull() ?: return Style.ENGLISH_FULL
        val normalized = CrisisDetector.normalize(first)
        return when {
            spanish.values.any { CrisisDetector.normalize(it) == normalized } -> Style.SPANISH
            normalized.length == 3 -> Style.ENGLISH_SHORT
            else -> Style.ENGLISH_FULL
        }
    }

    fun encode(day: DayOfWeek, style: Style): String = when (style) {
        Style.ENGLISH_FULL -> day.name
        Style.ENGLISH_SHORT -> day.name.take(3)
        Style.SPANISH -> spanish.getValue(day)
    }

    /** Human readable summary, e.g. "Lunes a Viernes, 09:00 - 18:00". */
    fun summary(days: Set<DayOfWeek>, start: String, end: String): String {
        if (days.isEmpty()) return ""
        val sorted = days.sorted()
        val consecutive = sorted.zipWithNext().all { (a, b) -> b.value - a.value == 1 }
        val label = if (consecutive && sorted.size > 2) "${spanish.getValue(sorted.first())} a ${spanish.getValue(sorted.last())}"
        else sorted.joinToString(", ") { spanish.getValue(it) }
        return "$label, $start - $end"
    }
}
