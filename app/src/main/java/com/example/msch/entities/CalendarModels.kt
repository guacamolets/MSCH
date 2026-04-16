package com.example.msch.entities

import java.util.Date

data class CalendarDay(
    val date: Date,
    val timeInMillis: Long,
    val isToday: Boolean,
    val status: DayStatus,
    val dayOfCycle: Int?
)

enum class DayStatus {
    None, Period, Prediction
}