package com.example.kasisave

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

import androidx.room.*

@Dao
interface IncomeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income)

    @Query("SELECT * FROM Income ORDER BY id DESC")
    suspend fun getAllIncomes(): List<Income>

    @Query("SELECT * FROM Income WHERE category = :category")
    suspend fun getIncomeByCategory(category: String): List<Income>

    @Query("SELECT * FROM Income WHERE isRecurring = 1")
    suspend fun getRecurringIncome(): List<Income>

    @Query("SELECT SUM(amount) FROM income WHERE userId = :userId")
    suspend fun getTotalIncomeForUser(userId: Int): Double?


    @Query("SELECT SUM(amount) FROM Income")
    suspend fun getTotalIncome(): Double?

    @Query("SELECT * FROM income WHERE userId = :userId ORDER BY date DESC")
    suspend fun getIncomesForUser(userId: Int): List<Income>

    @Delete
    suspend fun deleteIncome(income: Income)
}


