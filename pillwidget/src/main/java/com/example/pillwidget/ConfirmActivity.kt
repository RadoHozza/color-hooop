package com.example.pillwidget

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slot = intent.getStringExtra("slot")
        if (slot != "RANO" && slot != "VECER") {
            finish()
            return
        }

        ensurePermissionsAndAlarms()

        val takenNow = PillPrefs.isTaken(this, slot)
        val label = if (slot == "RANO") "ranný" else "večerný"
        val title = if (slot == "RANO") "Ráno" else "Večer"
        val message = if (!takenNow) {
            "Potvrdiť, že si zobral $label liek?"
        } else {
            "Zrušiť potvrdenie – $label liek NEBOL zobratý?"
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Áno") { _, _ ->
                PillPrefs.setTaken(this, slot, !takenNow)
                PillWidgetProvider.updateAllWidgets(this)
                finish()
            }
            .setNegativeButton("Nie") { _, _ ->
                finish()
            }
            .setOnDismissListener {
                finish()
            }
            .show()
    }

    private fun ensurePermissionsAndAlarms() {
        try {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        } catch (e: Exception) {
            // ignoruj, budik pojde v nepresnom rezime
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        AlarmScheduler.scheduleAll(this)
    }
}
