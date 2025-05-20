package com.example.kasisave.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Get current logged-in user or null
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // Sign up with email & password
    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)  // success
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    // Sign in with email & password
    fun signIn(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(true, null)  // success
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun getCurrentUserUid(): String? {
        return auth.currentUser?.uid
    }
    // Sign out current user
    fun signOut() {
        auth.signOut()
    }
}
