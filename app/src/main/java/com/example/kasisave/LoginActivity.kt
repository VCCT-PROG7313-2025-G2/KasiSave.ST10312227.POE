package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.auth.AuthManager

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInputLogin: EditText
    private lateinit var passwordInputLogin: EditText
    private lateinit var logInButton: Button
    private lateinit var signUpLinkText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // UI Components
        emailInputLogin = findViewById(R.id.emailInput)
        passwordInputLogin = findViewById(R.id.passwordInput)
        logInButton = findViewById(R.id.loginButton)
        signUpLinkText = findViewById(R.id.signUpLinkText)

        signUpLinkText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        logInButton.setOnClickListener {
            val email = emailInputLogin.text.toString().trim()
            val password = passwordInputLogin.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AuthManager.signIn(email, password) { success, error ->
                runOnUiThread {
                    if (success) {
                        val userId = AuthManager.getCurrentUserUid()

                        if (!userId.isNullOrBlank()) {
                            // Save UID
                            val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
                            sharedPrefs.edit().putString("user_id", userId).apply()

                            // Immediately transition to dashboard
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
