package com.example.neurozen_front.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.neurozen_front.domain.logic.DateTimeUtils
import com.example.neurozen_front.domain.logic.ReminderMath
import com.example.neurozen_front.domain.model.Appointment
import com.example.neurozen_front.domain.model.AppointmentStatus
import com.example.neurozen_front.domain.usecase.ReminderCanceller
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local reminders with AlarmManager (inexact + allowWhileIdle: no exact-alarm permission needed).
 * Only one alarm per kind is pending; the receiver schedules the next one when it fires.
 */
@Singleton
class ReminderScheduler @Inject constructor(@ApplicationContext private val context: Context) : ReminderCanceller {

    private val alarms = context.getSystemService(AlarmManager::class.java)
    val store = ReminderSettingsStore(context)

    fun scheduleNext(kind: ReminderKind, now: ZonedDateTime = ZonedDateTime.now(DateTimeUtils.LIMA)): ZonedDateTime? {
        cancel(kind.ordinal, kind)
        val config = store.get(kind)
        if (!config.enabled) return null
        val next = ReminderMath.nextOccurrence(now, config.days, config.start, config.end, config.everyMinutes) ?: return null
        set(next.toInstant(), kind.ordinal, kind, null)
        return next
    }

    fun rescheduleRecurring() {
        scheduleNext(ReminderKind.BREAK)
        scheduleNext(ReminderKind.ERGONOMICS)
    }

    /** 1 hour before each CONFIRMED appointment; cancelled ones lose their reminder. */
    fun syncAppointments(appointments: List<Appointment>, now: Instant = Instant.now()) {
        store.scheduledAppointmentIds().forEach { cancel(appointmentRequestCode(it), ReminderKind.APPOINTMENT) }
        val scheduled = mutableSetOf<Long>()
        appointments.filter { it.status == AppointmentStatus.CONFIRMED }.forEach { appointment ->
            ReminderMath.appointmentReminder(appointment.dateTime, now)?.let { at ->
                val text = "Tu sesión es a las ${DateTimeUtils.formatTime(appointment.dateTime)}. Busca un lugar tranquilo."
                set(at, appointmentRequestCode(appointment.id), ReminderKind.APPOINTMENT, text)
                scheduled += appointment.id
            }
        }
        store.setScheduledAppointmentIds(scheduled)
    }

    override fun cancelAll() {
        ReminderKind.entries.filter { it != ReminderKind.APPOINTMENT }.forEach { cancel(it.ordinal, it) }
        store.scheduledAppointmentIds().forEach { cancel(appointmentRequestCode(it), ReminderKind.APPOINTMENT) }
        store.clear()
    }

    private fun set(at: Instant, requestCode: Int, kind: ReminderKind, text: String?) {
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending(requestCode, kind, text))
    }

    private fun cancel(requestCode: Int, kind: ReminderKind) = alarms.cancel(pending(requestCode, kind, null))

    private fun pending(requestCode: Int, kind: ReminderKind, text: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("com.example.neurozen_front.REMINDER_${kind.key}")
            .putExtra(ReminderReceiver.EXTRA_KIND, kind.name)
            .putExtra(ReminderReceiver.EXTRA_TEXT, text)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun appointmentRequestCode(id: Long): Int = 10_000 + (id % 1_000_000).toInt()
}
