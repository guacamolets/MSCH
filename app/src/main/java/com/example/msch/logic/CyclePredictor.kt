package com.example.msch.logic

import com.example.msch.entities.PeriodRecord
import java.util.concurrent.TimeUnit

object CyclePredictor {
    private fun getRecordDuration(record: PeriodRecord, defaultLength: Int): Long {
        return if (record.endDate != null) {
            record.endDate - record.startDate
        } else {
            defaultLength * AppConfig.MILLIS_IN_DAY
        }
    }

    fun predictNextCycle(records: List<PeriodRecord>, defaultCycleLength: Int): Long {
        val cycleMillis = TimeUnit.DAYS.toMillis(defaultCycleLength.toLong())

        if (records.isEmpty()) return System.currentTimeMillis() + cycleMillis
        if (records.size < 2) return records.first().startDate + cycleMillis

        val limitedRecords = records.take(AppConfig.MAX_RECORDS + 1)
        val intervals = mutableListOf<Long>()

        for (i in 0 until limitedRecords.size - 1) {
            val diff = limitedRecords[i].startDate - limitedRecords[i + 1].startDate
            intervals.add(diff)
        }

        val averageInterval = intervals.average().toLong()
        return records.first().startDate + if (averageInterval > 0) averageInterval else cycleMillis
    }

    fun calculateAverageCycle(records: List<PeriodRecord>, defaultCycleLength: Int): Int {
        if (records.size < 2) return defaultCycleLength

        val limitedRecords = records.take(AppConfig.MAX_RECORDS + 1)
        val durations = mutableListOf<Long>()

        for (i in 0 until limitedRecords.size - 1) {
            val diff = limitedRecords[i].startDate - limitedRecords[i + 1].startDate
            durations.add(diff / AppConfig.MILLIS_IN_DAY)
        }

        return if (durations.isNotEmpty()) durations.average().toInt() else defaultCycleLength
    }

    fun calculateAveragePeriod(records: List<PeriodRecord>, defaultLength: Int): Int {
        if (records.isEmpty()) return defaultLength

        val durations = records.take(10).map { record ->
            getRecordDuration(record, defaultLength) / AppConfig.MILLIS_IN_DAY
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

        return kotlin.math.ceil(diff.toDouble() / AppConfig.MILLIS_IN_DAY).toInt()
    }

    fun getLastStats(records: List<PeriodRecord>): Pair<Int?, Int?> {
        val sorted = records.filter { it.endDate != null }.sortedByDescending { it.startDate }
        if (sorted.size < 2) return null to sorted.firstOrNull()?.let {
            ((it.endDate!! - it.startDate) / AppConfig.MILLIS_IN_DAY).toInt() + 1
        }

        val lastRecord = sorted[0]
        val prevRecord = sorted[1]

        val lastPeriodLen = ((lastRecord.endDate!! - lastRecord.startDate) / AppConfig.MILLIS_IN_DAY).toInt() + 1
        val lastCycleLen = ((lastRecord.startDate - prevRecord.startDate) / AppConfig.MILLIS_IN_DAY).toInt()

        return lastCycleLen to lastPeriodLen
    }

    fun getVariations(records: List<PeriodRecord>, defaultCycle: Int): Pair<String, String> {
        val completed = records.filter { it.endDate != null }.sortedBy { it.startDate }
        if (completed.isEmpty()) return "--" to "--"

        val periodLengths = completed.map {
            ((it.endDate!! - it.startDate) / AppConfig.MILLIS_IN_DAY).toInt() + 1
        }

        val cycleLengths = mutableListOf<Int>()
        for (i in 0 until completed.size - 1) {
            val len = ((completed[i+1].startDate - completed[i].startDate) / AppConfig.MILLIS_IN_DAY).toInt()
            cycleLengths.add(len)
        }

        val periodVar = "${periodLengths.minOrNull() ?: 0}–${periodLengths.maxOrNull() ?: 0}"
        val cycleVar = if (cycleLengths.isEmpty()) "$defaultCycle"
        else "${cycleLengths.minOrNull()}–${cycleLengths.maxOrNull()}"

        return cycleVar to periodVar
    }
}