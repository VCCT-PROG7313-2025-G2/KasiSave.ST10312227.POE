package com.example.kasisave

data class Income(
    val id: String = "",             // Firestore document ID
    val userId: String = "",         // Firebase UID
    val amount: Double = 0.0,
    val date: String = "",
    val category: String = "",
    val isRecurring: Boolean = false
)
