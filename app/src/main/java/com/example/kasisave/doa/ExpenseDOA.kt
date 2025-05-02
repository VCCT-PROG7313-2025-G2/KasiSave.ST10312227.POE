package com.example.kasisave

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface ExpenseDao {

    @Insert
    suspend fun insert(expense: Expense)

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :startDate AND :endDate AND category = :category ORDER BY dateMillis DESC")
    suspend fun getExpensesByDateAndCategory(startDate: Long, endDate: Long, category: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE dateMillis BETWEEN :start AND :end")
    suspend fun getExpensesBetween(start: Long, end: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpensesForUser(userId: Int): List<Expense>


    @Query("SELECT * FROM expenses WHERE category = :category")
    suspend fun getExpensesByCategory(category: String): List<Expense>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId")
    suspend fun getTotalExpensesForUser(userId: Int): Double?

    @Query("SELECT category, SUM(amount) AS total FROM expenses WHERE userId = :userId GROUP BY category")
    suspend fun getTotalAmountByCategoryForUser(userId: Int): List<CategoryTotal>


    @Query("SELECT * FROM expenses WHERE userid = :userId AND dateMillis BETWEEN :startDate AND :endDate")
    suspend fun getExpensesBetweenForUser(userId: Int, startDate: Long, endDate: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE userid = :userId AND dateMillis BETWEEN :startDate AND :endDate AND category = :category")
    suspend fun getExpensesByDateAndCategoryForUser(userId: Int, startDate: Long, endDate: Long, category: String): List<Expense>

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalExpenses(): Double?

    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category")
    suspend fun getTotalAmountByCategory(): List<CategoryTotal>

    data class CategoryTotal(
        val category: String,
        val total: Double
    )
}
