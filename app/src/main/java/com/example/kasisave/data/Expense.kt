package com.example.kasisave

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val category: String,
    val amount: Double,
    val dateMillis: Long,
    val startTime: String? = null,
    val endTime: String? = null,
    val description: String? = null,
    val photoUri: String? = null,
    val isRecurring: Boolean = false
)

