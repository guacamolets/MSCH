package com.example.msch.logic

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var defaultCycleLength: Int
        get() = prefs.getInt("cycle_length", 28)
        set(value) = prefs.edit().putInt("cycle_length", value).apply()

    var defaultPeriodLength: Int
        get() = prefs.getInt("period_length", 5)
        set(value) = prefs.edit().putInt("period_length", value).apply()
}
