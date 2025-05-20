package com.example.kasisave

data class Expense(
    val id: String = "", // Firestore document ID
    val userId: String = "", // Firebase user UID
    val category: String = "",
    val amount: Double = 0.0,
    val dateMillis: Long = 0L,
    val startTime: String? = null,
    val endTime: String? = null,
    val description: String? = null,
    val photoUri: String? = null,
    val isRecurring: Boolean = false
)
