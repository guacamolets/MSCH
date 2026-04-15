package com.example.msch.logic

import com.example.msch.data.PeriodRecord
import java.util.concurrent.TimeUnit

object CyclePredictor {
    fun predictNextCycle(records: List<PeriodRecord>, defaultCycleLength: Int): Long {
        val cycleMillis = TimeUnit.DAYS.toMillis(defaultCycleLength.toLong())

        if (records.isEmpty()) {
            return System.currentTimeMillis() + cycleMillis
        }

        if (records.size < 2) {
            return records.first().startDate + cycleMillis
        }

        val limitedRecords = records.take(AppConfig.MAX_RECORDS + 1)
        val intervals = mutableListOf<Long>()
        for (i in 0 until limitedRecords.size - 1) {
            val diff = limitedRecords[i].startDate - limitedRecords[i + 1].startDate
            intervals.add(diff)
        }

        val averageInterval = intervals.average().toLong()
        return records.first().startDate + if (averageInterval > 0) averageInterval else cycleMillis
    }

    fun calculateAverage(records: List<PeriodRecord>, defaultCycleLength: Int): Int {
        if (records.size < 2) return defaultCycleLength

        val limitedRecords = records.take(AppConfig.MAX_RECORDS + 1)
        val durations = mutableListOf<Long>()
        for (i in 0 until limitedRecords.size - 1) {
            val diff = limitedRecords[i].startDate - limitedRecords[i + 1].startDate
            durations.add(diff / AppConfig.MILLIS_IN_DAY)
        }

        return durations.average().toInt()
    }
    fun getCurrentDay(records: List<PeriodRecord>): Int? {
        if (records.isEmpty()) return null

        val lastStart = records.first().startDate
        val diff = System.currentTimeMillis() - lastStart

        return (diff / AppConfig.MILLIS_IN_DAY).toInt() + 1
    }

    fun getDaysUntilNext(nextDateMillis: Long): Int {
        val diff = nextDateMillis - System.currentTimeMillis()

        return (diff / AppConfig.MILLIS_IN_DAY).toInt()
    }

}