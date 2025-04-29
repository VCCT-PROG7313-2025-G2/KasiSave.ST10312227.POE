package com.example.kasisave

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Income::class], version = 1)
abstract class IncomeDatabase : RoomDatabase() {
    abstract fun incomeDao(): IncomeDao
}
