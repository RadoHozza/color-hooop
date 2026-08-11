package com.example.pillwidget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slot = intent.getStringExtra("slot") ?: return

        if (slot == "MIDNIGHT") {
            // Len vynut reset a prekresli widget - ziadna notifikacia
            PillPrefs.resetIfNewDay(context)
            PillWidgetProvider.updateAllWidgets(context)
            AlarmScheduler.scheduleOne(context, hour = 0, minute = 0, slot = "MIDNIGHT", requestCode = 999)
            return
        }

        PillPrefs.resetIfNewDay(context)
        val taken = PillPrefs.isTaken(context, slot)
        if (!taken) {
            showNotification(context, slot)
        }

        // Znova naplanuj ten isty budik na zajtra (rovnaky cas)
        val requestCode = if (slot == "RANO") 100 else 200
        val hour = if (slot == "RANO") 6 else 20
        AlarmScheduler.scheduleOne(context, hour = hour, minute = 0, slot = slot, requestCode = requestCode)
    }

    private fun showNotification(context: Context, slot: String) {
        val channelId = "pill_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Pripomienky liekov", NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val label = if (slot == "RANO") "ranný" else "večerný"
        val openIntent = Intent(context, ConfirmActivity::class.java).apply {
            putExtra("slot", slot)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, if (slot == "RANO") 300 else 400, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Nezabudni na liek")
            .setContentText("Ešte si nepotvrdil $label liek")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (slot == "RANO") 1 else 2
        manager.notify(notificationId, notification)
    }
}
