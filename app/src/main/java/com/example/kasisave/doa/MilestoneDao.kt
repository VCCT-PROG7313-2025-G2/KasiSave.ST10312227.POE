package com.example.kasisave

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneDao {

    @Query("SELECT * FROM milestones WHERE userId = :userId ORDER BY id DESC")
    fun getAllMilestonesForUser(userId: Int): Flow<List<Milestone>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: Milestone)

    @Delete
    suspend fun deleteMilestone(milestone: Milestone)

    @Update
    suspend fun updateMilestone(milestone: Milestone)

    @Query("""
        SELECT * FROM milestones 
        WHERE strftime('%m', deadline) = :month 
        AND strftime('%Y', deadline) = :year 
        AND userId = :userId 
        LIMIT 1
    """)
    suspend fun getMilestoneForMonthAndUser(month: String, year: Int, userId: Int): Milestone?
}

