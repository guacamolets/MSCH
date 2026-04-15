package com.example.msch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "periods")
data class PeriodRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startDate: Long,
    val endDate: Long? = null
)