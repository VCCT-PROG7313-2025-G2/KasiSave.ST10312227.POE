package com.example.kasisave.data

data class User(
    val id: String? = null,       // Firestore document ID (nullable)
    val email: String = "",
    val passwordHash: String = ""
)
