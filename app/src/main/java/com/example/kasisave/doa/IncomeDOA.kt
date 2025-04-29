package com.example.kasisave

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface IncomeDao {
    @Insert
    suspend fun insertIncome(income: Income)

    @Query("SELECT * FROM Income")
    suspend fun getAllIncomes(): List<Income>

    @Query("SELECT SUM(amount) FROM Income")
    suspend fun getTotalIncome(): Double?

    @Delete
    suspend fun deleteIncome(income: Income)
}

