package com.example.kasisave.dao

import androidx.room.*
import com.example.kasisave.model.Goal

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Query("SELECT * FROM goals WHERE month = :month LIMIT 1")
    suspend fun getGoalForMonth(month: String): Goal?

    @Query("SELECT * FROM goals")
    suspend fun getAllGoals(): List<Goal>

    @Query("SELECT * FROM goals WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getGoalForMonth(month: String, year: Int): Goal?
}