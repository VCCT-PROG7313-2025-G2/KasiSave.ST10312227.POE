package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val signUpLinkText: TextView = findViewById(R.id.signUpLinkText)
        val logInButton: Button = findViewById(R.id.createAccountButton)
        val emailInputLogin: EditText = findViewById(R.id.emailInput)
        val passwordInputLogin: EditText = findViewById(R.id.passwordInput)

        val db = AppDatabase.getDatabase(applicationContext)
        val userDao = db.userDao()

        logInButton.setOnClickListener {
            val email = emailInputLogin.text.toString().trim()
            val password = passwordInputLogin.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val user = userDao.getUserByEmail(email).first()
                if (user != null && user.passwordHash == password) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Email or Password is incorrect", Toast.LENGTH_SHORT).show()
                        emailInputLogin.error = "Incorrect email"
                        passwordInputLogin.error = "Incorrect password"
                    }
                }
            }
        }

        signUpLinkText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}
