package com.example.msch.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.msch.data.PeriodDao
import com.example.msch.entities.PeriodRecord
import com.example.msch.logic.AppConfig
import com.example.msch.services.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(private val dao: PeriodDao, private val settingsManager: SettingsManager) : ViewModel() {
    val records: StateFlow<List<PeriodRecord>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val predictedNextDate: StateFlow<Long> = records.map { list ->
        calculateNextPeriod(list, settingsManager.defaultCycleLength)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    fun addRecord(startDate: Long, endDate: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(PeriodRecord(startDate = startDate, endDate = endDate))
        }
    }

    fun updateRecord(updatedRecord: PeriodRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(updatedRecord)
        }
    }

    fun deleteRecord(record: PeriodRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(record)
        }
    }

    fun importRecords(recordsToImport: List<PeriodRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitized = applySanitization(recordsToImport)
            dao.insertAll(sanitized)
        }
    }

    fun sanitizeRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val allRecords = records.value.ifEmpty { dao.getAllRecords().first() }
            if (allRecords.isEmpty()) return@launch

            val sanitized = applySanitization(allRecords)

            sanitized.filterIndexed { index, record -> record != allRecords[index] }
                .forEach { updated -> dao.update(updated) }
        }
    }

    private fun applySanitization(list: List<PeriodRecord>): List<PeriodRecord> {
        val now = System.currentTimeMillis()
        val limit = AppConfig.AUTO_CLOSE_DAYS * AppConfig.MILLIS_IN_DAY
        val periodLength = settingsManager.defaultPeriodLength

        return list.map { record ->
            if (record.endDate == null && (now - record.startDate) > limit) {
                record.copy(endDate = record.startDate.toEndOfDay(periodLength - 1))
            } else {
                record
            }
        }
    }

    private fun calculateNextPeriod(records: List<PeriodRecord>, cycleLength: Int): Long {
        val lastDate = records.maxOfOrNull { it.startDate } ?: return System.currentTimeMillis()
        return Calendar.getInstance().apply {
            timeInMillis = lastDate + (cycleLength * AppConfig.MILLIS_IN_DAY)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endPeriod(millis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val lastRecord = records.value.maxByOrNull { it.startDate }
            if (lastRecord != null && lastRecord.endDate == null) {
                val rawEnd = if (millis < lastRecord.startDate) lastRecord.startDate else millis
                dao.update(lastRecord.copy(endDate = rawEnd.toEndOfDay()))
            }
        }
    }

    private fun Long.toEndOfDay(addDays: Int = 0): Long {
        return Calendar.getInstance().apply {
            timeInMillis = this@toEndOfDay
            add(Calendar.DAY_OF_YEAR, addDays)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}