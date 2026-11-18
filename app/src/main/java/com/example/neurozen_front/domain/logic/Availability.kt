package com.example.neurozen_front.domain.logic

import com.example.neurozen_front.domain.model.AvailabilitySchedule
import java.time.DayOfWeek

/**
 * Weekly availability being edited. The backend requires `availabilitySchedule` in EVERY PUT /professionals/me
 * (400 «Indica tus días y horario de atención» when missing) and a newly registered psychologist has none, so the
 * screens that save the profile ask for it in the same form when it does not exist yet.
 */
data class AvailabilityDraft(
    val days: Set<DayOfWeek> = DEFAULT_DAYS,
    val start: String = "09:00",
    val end: String = "18:00"
) {
    /** Error shown to the user, or null when valid (same rules as the backend: at least one day, end after start). */
    val error: String?
        get() = when {
            days.isEmpty() -> "Selecciona al menos un día de atención"
            !Validators.isValidHour(start) || !Validators.isValidHour(end) -> "Usa el formato HH:mm (por ejemplo 09:00)"
            end <= start -> "La hora de fin debe ser posterior a la de inicio"
            else -> null
        }

    val isValid: Boolean get() = error == null

    fun toggle(day: DayOfWeek) = copy(days = if (day in days) days - day else days + day)

    /** English day names: the backend only accepts monday…sunday (it rejects «Lunes» as an invalid day). */
    fun toSchedule() = AvailabilitySchedule(days.sorted().map { DayCodec.encode(it, DayCodec.Style.ENGLISH_FULL) }, start, end)

    fun summary(): String = DayCodec.summary(days, start, end)

    companion object {
        val DEFAULT_DAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

        /** Draft from the saved schedule, or the editable proposal (Mon-Fri 09:00-18:00) when there is none. */
        fun from(schedule: AvailabilitySchedule?): AvailabilityDraft {
            val days = schedule?.days.orEmpty().mapNotNull(DayCodec::parse).toSet()
            if (days.isEmpty()) return AvailabilityDraft()
            return AvailabilityDraft(days, schedule?.start?.ifBlank { null } ?: "09:00", schedule?.end?.ifBlank { null } ?: "18:00")
        }

        fun hasSchedule(schedule: AvailabilitySchedule?): Boolean = schedule?.days.orEmpty().any { DayCodec.parse(it) != null }
    }
}
