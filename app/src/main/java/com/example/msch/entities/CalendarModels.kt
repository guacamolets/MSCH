package com.example.msch.entities

import java.util.Date

data class CalendarDay(
    val date: Date,
    val isToday: Boolean,
    val status: DayStatus
)

enum class DayStatus {
    None, Period, Prediction
}