package com.example.neurozen_front.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.neurozen_front.MainActivity
import com.example.neurozen_front.R

object NotificationHelper {

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        listOf(
            NotificationChannel(ReminderKind.BREAK.channelId, "Pausas activas", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(ReminderKind.ERGONOMICS.channelId, "Ergonomía", NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(ReminderKind.APPOINTMENT.channelId, "Citas", NotificationManager.IMPORTANCE_HIGH)
        ).forEach(manager::createNotificationChannel)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, kind: ReminderKind, id: Int, text: String = kind.text) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_stat_neurozen)
            .setContentTitle(kind.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }
}
