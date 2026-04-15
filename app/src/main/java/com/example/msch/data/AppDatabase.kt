package com.example.msch.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PeriodRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
}