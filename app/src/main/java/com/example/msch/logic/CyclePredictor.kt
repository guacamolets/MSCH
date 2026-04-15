package com.example.msch.logic

import com.example.msch.data.PeriodRecord
import java.util.concurrent.TimeUnit

object CyclePredictor {
    const val DEFAULT_CYCLE_DAYS = 28L

    fun predictNextCycle(records: List<PeriodRecord>): Long {
        if (records.size < 2) {
            val lastDate = records.firstOrNull()?.startDate ?: System.currentTimeMillis()
            return lastDate + TimeUnit.DAYS.toMillis(DEFAULT_CYCLE_DAYS)
        }

        val intervals = mutableListOf<Long>()
        for (i in 0 until records.size - 1) {
            val diff = records[i].startDate - records[i + 1].startDate
            intervals.add(diff)
        }

        val averageInterval = intervals.average().toLong()

        return records.first().startDate + averageInterval
    }
}