package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Feature4Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature4)

        // Set up Next button click listener
        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)


            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

