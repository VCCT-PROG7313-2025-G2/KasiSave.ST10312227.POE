package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.auth.AuthManager

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val signInText = findViewById<TextView>(R.id.signInText)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        signInText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        createAccountButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use Firebase Authentication to create the account
            AuthManager.signUp(email, password) { success, error ->
                runOnUiThread {
                    if (success) {
                        val userId = AuthManager.getCurrentUserUid()

                        if (!userId.isNullOrBlank()) {
                            // Save UID using the consistent "user_id" key
                            val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
                            sharedPrefs.edit()
                                .putString("user_id", userId)
                                .apply()

                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, DashboardActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                            finish()
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
