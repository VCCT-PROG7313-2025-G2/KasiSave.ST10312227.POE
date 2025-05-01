package com.example.kasisave

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE category = :category")
    suspend fun getExpensesByCategory(category: String): List<Expense>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalExpenses(): Double?
}
