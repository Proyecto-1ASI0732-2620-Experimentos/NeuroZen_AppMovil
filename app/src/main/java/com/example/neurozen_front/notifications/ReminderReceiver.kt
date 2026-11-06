package com.example.neurozen_front.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() } ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: kind.text
        NotificationHelper.show(context, kind, kind.ordinal + (System.currentTimeMillis() % 1000).toInt(), text)
        if (kind != ReminderKind.APPOINTMENT) ReminderScheduler(context.applicationContext).scheduleNext(kind)
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_TEXT = "text"
    }
}

/** Alarms are lost on reboot: re-create the recurring ones. Appointments resync when the app opens. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler(context.applicationContext).rescheduleRecurring()
        }
    }
}
