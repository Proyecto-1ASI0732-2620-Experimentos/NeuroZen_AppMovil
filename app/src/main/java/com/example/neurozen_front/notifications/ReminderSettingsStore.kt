package com.example.neurozen_front.notifications

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalTime

/** Non-sensitive reminder configuration. Plain SharedPreferences so BroadcastReceivers can read it. */
data class ReminderConfig(
    val enabled: Boolean = false,
    val days: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(18, 0),
    val everyMinutes: Int = 60
)

enum class ReminderKind(val key: String, val channelId: String, val title: String, val text: String) {
    BREAK("break", "active_breaks", "Hora de una pausa activa", "Levántate, estira cuello y hombros y respira profundo por 2 minutos."),
    ERGONOMICS("ergonomics", "ergonomics", "Revisa tu postura", "Espalda apoyada, pantalla a la altura de los ojos y pies en el piso."),
    APPOINTMENT("appointment", "appointments", "Tu sesión es en 1 hora", "Prepárate para tu cita con tu psicólogo en NeuroZen.")
}

class ReminderSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("neurozen_reminders", Context.MODE_PRIVATE)

    fun get(kind: ReminderKind): ReminderConfig {
        val k = kind.key
        val defaults = ReminderConfig(everyMinutes = if (kind == ReminderKind.ERGONOMICS) 90 else 60)
        if (!prefs.contains("${k}_enabled")) return defaults
        return ReminderConfig(
            enabled = prefs.getBoolean("${k}_enabled", false),
            days = prefs.getStringSet("${k}_days", null)?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }?.toSet() ?: defaults.days,
            start = runCatching { LocalTime.parse(prefs.getString("${k}_start", null)) }.getOrDefault(defaults.start),
            end = runCatching { LocalTime.parse(prefs.getString("${k}_end", null)) }.getOrDefault(defaults.end),
            everyMinutes = prefs.getInt("${k}_every", defaults.everyMinutes)
        )
    }

    fun save(kind: ReminderKind, config: ReminderConfig) {
        val k = kind.key
        prefs.edit()
            .putBoolean("${k}_enabled", config.enabled)
            .putStringSet("${k}_days", config.days.map { it.name }.toSet())
            .putString("${k}_start", config.start.toString())
            .putString("${k}_end", config.end.toString())
            .putInt("${k}_every", config.everyMinutes)
            .apply()
    }

    fun scheduledAppointmentIds(): Set<Long> = prefs.getStringSet("appointment_ids", emptySet())!!.mapNotNull { it.toLongOrNull() }.toSet()

    fun setScheduledAppointmentIds(ids: Set<Long>) {
        prefs.edit().putStringSet("appointment_ids", ids.map { it.toString() }.toSet()).apply()
    }

    fun clear() = prefs.edit().clear().apply()
}
