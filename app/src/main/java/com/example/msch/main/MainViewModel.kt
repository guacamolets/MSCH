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

    fun addRecord(millis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insert(PeriodRecord(startDate = millis))
        }
    }

    fun updateRecord(record: PeriodRecord, newMillis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.update(record.copy(startDate = newMillis))
        }
    }

    fun deleteRecord(record: PeriodRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.delete(record)
        }
    }

    fun importRecords(records: List<PeriodRecord>) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertAll(records)
        }
    }

    private fun calculateNextPeriod(records: List<PeriodRecord>, cycleLength: Int): Long {
        val lastDate = records.maxOfOrNull { it.startDate } ?: return System.currentTimeMillis()
        return Calendar.getInstance().apply {
            timeInMillis = lastDate + (cycleLength * 86400000L)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }.timeInMillis
    }

    fun endPeriod(millis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val lastRecord = records.value.maxByOrNull { it.startDate }
            if (lastRecord != null && lastRecord.endDate == null) {
                val endDateMillis = if (millis < lastRecord.startDate) lastRecord.startDate else millis
                dao.update(lastRecord.copy(endDate = endDateMillis))
            }
        }
    }

    fun sanitizeRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val limit = AppConfig.AUTO_CLOSE_DAYS * AppConfig.MILLIS_IN_DAY
            val defaultDuration = settingsManager.defaultPeriodLength * AppConfig.MILLIS_IN_DAY

            records.value.filter { it.endDate == null && (now - it.startDate) > limit }
                .forEach { record -> dao.update(record.copy(endDate = record.startDate + defaultDuration)) }
        }
    }
}