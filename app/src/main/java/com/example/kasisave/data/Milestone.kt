package com.example.kasisave

data class Milestone(
    var id: String? = null,         // Firestore document ID (nullable, assigned on create)
    val userId: String = "",        // Firebase user ID (String)
    val name: String = "",
    val targetAmount: Double = 0.0,
    val deadline: String = "",
    val minMonthlySpend: Double = 0.0,
    val maxMonthlySpend: Double = 0.0
)
