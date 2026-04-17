package com.example.msch.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.msch.entities.PeriodRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(record: PeriodRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(records: List<PeriodRecord>)

    @Update
    fun update(record: PeriodRecord)

    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAllRecords(): Flow<List<PeriodRecord>>

    @Delete
    fun delete(record: PeriodRecord)

    @Query("DELETE FROM periods")
    fun deleteAll()
}