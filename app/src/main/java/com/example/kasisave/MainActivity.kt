package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getStartedButton: Button = findViewById(R.id.button2)

        getStartedButton.setOnClickListener {


            // Start Feature1Activity
            val intent = Intent(this, Feature1Activity::class.java)
            startActivity(intent)
        }
    }
}
