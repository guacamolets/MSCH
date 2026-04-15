package com.example.msch.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Insert
    fun insert(record: PeriodRecord): Long

    @Update
    fun update(record: PeriodRecord): Int

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAllRecords(): Flow<List<PeriodRecord>>

    @Delete
    fun delete(record: PeriodRecord)
}