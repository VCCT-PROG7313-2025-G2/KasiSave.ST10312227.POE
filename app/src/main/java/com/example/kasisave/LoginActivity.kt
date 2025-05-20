package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.auth.AuthManager

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val signUpLinkText: TextView = findViewById(R.id.signUpLinkText)
        val logInButton: Button = findViewById(R.id.createAccountButton)
        val emailInputLogin: EditText = findViewById(R.id.emailInput)
        val passwordInputLogin: EditText = findViewById(R.id.passwordInput)

        signUpLinkText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        logInButton.setOnClickListener {
            val email = emailInputLogin.text.toString().trim()
            val password = passwordInputLogin.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Use Firebase Authentication to sign in
            AuthManager.signIn(email, password) { success, error ->
                runOnUiThread {
                    if (success) {
                        val userId = AuthManager.getCurrentUserUid()

                        if (!userId.isNullOrBlank()) {
                            // Save UID for later use
                            val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
                            sharedPrefs.edit()
                                .putString("user_id", userId) // use "user_id" to match ExpensesActivity
                                .apply()

                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, DashboardActivity::class.java)
                            intent.putExtra("userId", userId)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to retrieve user ID", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "Email or Password is incorrect: $error", Toast.LENGTH_LONG).show()
                        emailInputLogin.error = "Incorrect email"
                        passwordInputLogin.error = "Incorrect password"
                    }
                }
            }
        }
    }
}
