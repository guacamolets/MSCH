package com.example.msch.logic

object AppConfig {
    const val MAX_RECORDS = 6
    const val AUTO_CLOSE_DAYS = 7
    const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24

    object Thresholds {
        const val MIN_CYCLE_DAYS = 21
        const val MAX_CYCLE_DAYS = 35
        const val MIN_PERIOD_DAYS = 3
        const val MAX_PERIOD_DAYS = 8
    }
}