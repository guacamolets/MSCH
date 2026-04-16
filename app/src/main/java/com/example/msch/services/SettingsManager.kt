package com.example.msch.services

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CYCLE_LENGTH = "cycle_length"
        private const val KEY_PERIOD_LENGTH = "period_length"
        private const val KEY_REMINDER_DAYS = "reminder_days_before"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_REMIND_TODAY = "remind_today"
        private const val KEY_REMIND_BEFORE_ENABLED = "remind_before_enabled"
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"
    }

    var defaultCycleLength: Int
        get() = prefs.getInt(KEY_CYCLE_LENGTH, 28)
        set(value) = prefs.edit().putInt(KEY_CYCLE_LENGTH, value).apply()

    var defaultPeriodLength: Int
        get() = prefs.getInt(KEY_PERIOD_LENGTH, 5)
        set(value) = prefs.edit().putInt(KEY_PERIOD_LENGTH, value).apply()

    var reminderDaysBefore: Int
        get() = prefs.getInt(KEY_REMINDER_DAYS, 2)
        set(value) = prefs.edit().putInt(KEY_REMINDER_DAYS, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    var remindToday: Boolean
        get() = prefs.getBoolean(KEY_REMIND_TODAY, true)
        set(value) = prefs.edit().putBoolean(KEY_REMIND_TODAY, value).apply()

    var remindBeforeEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMIND_BEFORE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMIND_BEFORE_ENABLED, value).apply()

    var appTheme: Int
        get() = prefs.getInt(KEY_THEME, 0)
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()
}