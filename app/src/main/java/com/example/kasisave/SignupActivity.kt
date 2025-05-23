package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.auth.AuthManager
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

    private lateinit var firstNameInput: EditText
    private lateinit var lastNameInput: EditText
    private lateinit var contactInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var createAccountButton: Button
    private lateinit var signInText: TextView

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize input fields
        firstNameInput = findViewById(R.id.firstNameInput)
        lastNameInput = findViewById(R.id.lastNameInput)
        contactInput = findViewById(R.id.contactInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        createAccountButton = findViewById(R.id.createAccountButton)
        signInText = findViewById(R.id.signInText)

        signInText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        createAccountButton.setOnClickListener {
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val contact = contactInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (firstName.isEmpty() || lastName.isEmpty() || contact.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AuthManager.signUp(email, password) { success, error ->
                runOnUiThread {
                    if (success) {
                        val userId = AuthManager.getCurrentUserUid()

                        if (!userId.isNullOrBlank()) {
                            // Save UID to shared preferences
                            val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
                            sharedPrefs.edit().putString("user_id", userId).apply()

                            // Create Firestore user profile
                            val userProfile = hashMapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "contactNumber" to contact,
                                "email" to email
                            )

                            db.collection("users").document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(this, DashboardActivity::class.java)
                                    intent.putExtra("userId", userId)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_LONG).show()
                                }

                        } else {
                            Toast.makeText(this, "Failed to retrieve user ID", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
