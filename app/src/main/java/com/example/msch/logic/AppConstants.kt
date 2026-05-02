package com.example.msch.logic

object AppConfig {
    const val MILLIS_IN_DAY = 1000L * 60 * 60 * 24
    const val MAX_NORMAL_VARIABILITY_DAYS = 8
    const val AUTO_CLOSE_DAYS = 7
    const val DAYS_IN_MONTH = 30
    const val STATS_PERIOD_MONTHS = 6
    const val STATS_LOOKBACK_DAYS = STATS_PERIOD_MONTHS * DAYS_IN_MONTH

    object Thresholds {
        const val MIN_CYCLE_DAYS = 21
        const val MAX_CYCLE_DAYS = 35
        const val MIN_PERIOD_DAYS = 3
        const val MAX_PERIOD_DAYS = 8
    }
}