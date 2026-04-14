package com.example.msch

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Insert
    fun insert(record: PeriodRecord): Long

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAllRecords(): Flow<List<PeriodRecord>>

    @Delete
    fun delete(record: PeriodRecord)
}