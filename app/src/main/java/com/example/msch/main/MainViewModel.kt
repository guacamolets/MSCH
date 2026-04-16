package com.example.msch.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.msch.data.PeriodDao
import com.example.msch.entities.PeriodRecord
import com.example.msch.services.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

class MainViewModel(private val dao: PeriodDao, private val settingsManager: SettingsManager) : ViewModel() {
    val records: StateFlow<List<PeriodRecord>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), emptyList())

    val predictedNextDate: StateFlow<Long> = records.map { list ->
        calculateNextPeriod(list, settingsManager.defaultCycleLength)
    }.stateIn(viewModelScope, SharingStarted.Companion.WhileSubscribed(5000), System.currentTimeMillis())

    private fun calculateNextPeriod(records: List<PeriodRecord>, cycleLength: Int): Long {
        val lastDate = records.maxOfOrNull { it.startDate } ?: return System.currentTimeMillis()

        return Calendar.getInstance().apply {
            timeInMillis = lastDate + (cycleLength * 86400000L)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}