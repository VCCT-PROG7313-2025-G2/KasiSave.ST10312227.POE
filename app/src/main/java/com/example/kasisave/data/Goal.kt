package com.example.kasisave.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val minAmount: Float,
    val maxAmount: Float,
    val month: String,
    val year: Int,
    val minGoal: Double

)