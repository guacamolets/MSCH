package com.example.msch.logic

import com.example.msch.data.PeriodRecord
import java.util.concurrent.TimeUnit

object CyclePredictor {
    const val DEFAULT_CYCLE_DAYS = 28L

    fun predictNextCycle(records: List<PeriodRecord>): Long {
        if (records.isEmpty()) {
            return System.currentTimeMillis() + AppConfig.MILLIS_IN_DAY * AppConfig.DEFAULT_CYCLE_LENGTH
        }

        if (records.size < 2) {
            return records.first().startDate + AppConfig.MILLIS_IN_DAY * AppConfig.DEFAULT_CYCLE_LENGTH
        }

        val limitedRecords = records.take(AppConfig.MAX_RECORDS + 1)
        val intervals = mutableListOf<Long>()
        for (i in 0 until limitedRecords.size - 1) {
            val diff = limitedRecords[i].startDate - limitedRecords[i + 1].startDate
            intervals.add(diff)
        }

        return records.first().startDate + intervals.average().toLong()
    }

    fun calculateAverage(records: List<PeriodRecord>): Int {
        if (records.size < 2) return AppConfig.DEFAULT_CYCLE_LENGTH

        val limitedRecords = records.take(AppConfig.MAX_RECORDS + 1)
        val durations = mutableListOf<Long>()
        for (i in 0 until limitedRecords.size - 1) {
            val diff = limitedRecords[i].startDate - limitedRecords[i + 1].startDate
            durations.add(diff / AppConfig.MILLIS_IN_DAY)
        }

        return durations.average().toInt()
    }
}