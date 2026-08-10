package com.example.pillwidget

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PillPrefs {
    private const val PREFS = "pill_prefs"
    private const val KEY_DATE = "date"
    private const val KEY_RANO = "rano_taken"
    private const val KEY_VECER = "vecer_taken"

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // Ak sa zmenil den od posledneho zaznamu, vynuluj oba priznaky (novy den = nezobrate lieky)
    fun resetIfNewDay(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = todayString()
        val savedDate = prefs.getString(KEY_DATE, null)
        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putBoolean(KEY_RANO, false)
                .putBoolean(KEY_VECER, false)
                .apply()
        }
    }

    fun isTaken(context: Context, slot: String): Boolean {
        resetIfNewDay(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (slot == "RANO") KEY_RANO else KEY_VECER
        return prefs.getBoolean(key, false)
    }

    fun setTaken(context: Context, slot: String, value: Boolean) {
        resetIfNewDay(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = if (slot == "RANO") KEY_RANO else KEY_VECER
        prefs.edit().putBoolean(key, value).apply()
    }
}
