package com.example.kasisave

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

    @Query("SELECT * FROM milestones")
    fun getAllMilestones(): Flow<List<Milestone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: Milestone)

    @Delete
    suspend fun deleteMilestone(milestone: Milestone)

    @Update
    suspend fun updateMilestone(milestone: Milestone)

    @Query("SELECT * FROM milestones WHERE strftime('%m', deadline) = :month AND strftime('%Y', deadline) = :year LIMIT 1")
    suspend fun getMilestoneForMonth(month: String, year: Int): Milestone?
}
