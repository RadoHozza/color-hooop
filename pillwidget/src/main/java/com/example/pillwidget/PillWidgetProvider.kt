package com.example.pillwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.net.Uri
import android.widget.RemoteViews

class PillWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        PillPrefs.resetIfNewDay(context)
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onEnabled(context: Context) {
        AlarmScheduler.scheduleAll(context)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PillWidgetProvider::class.java))
            for (id in ids) {
                updateWidget(context, manager, id)
            }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            PillPrefs.resetIfNewDay(context)
            val views = RemoteViews(context.packageName, R.layout.widget_pill)

            views.setImageViewResource(
                R.id.morningIcon,
                if (PillPrefs.isTaken(context, "RANO")) R.drawable.circle_green else R.drawable.circle_red
            )
            views.setImageViewResource(
                R.id.eveningIcon,
                if (PillPrefs.isTaken(context, "VECER")) R.drawable.circle_green else R.drawable.circle_red
            )

            val morningIntent = Intent(context, ConfirmActivity::class.java).apply {
                putExtra("slot", "RANO")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse("pillwidget://rano/$id")
            }
            val morningPi = PendingIntent.getActivity(
                context, id * 10 + 1, morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.morningIcon, morningPi)

            val eveningIntent = Intent(context, ConfirmActivity::class.java).apply {
                putExtra("slot", "VECER")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = Uri.parse("pillwidget://vecer/$id")
            }
            val eveningPi = PendingIntent.getActivity(
                context, id * 10 + 2, eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.eveningIcon, eveningPi)

            manager.updateAppWidget(id, views)
        }
    }
}
