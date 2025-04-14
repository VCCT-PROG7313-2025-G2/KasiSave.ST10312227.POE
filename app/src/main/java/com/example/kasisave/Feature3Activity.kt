package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Feature3Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature3)

        // Find the RadioGroup and RadioButtons by their IDs
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val radioButton2 = findViewById<RadioButton>(R.id.radioButton2)
        val radioButton5 = findViewById<RadioButton>(R.id.radioButton5)
        val radioButton6 = findViewById<RadioButton>(R.id.radioButton6)
        val radioButton4 = findViewById<RadioButton>(R.id.radioButton4)

        // Disable click events for RadioButtons
        radioButton2.isClickable = false
        radioButton5.isClickable = false
        radioButton6.isClickable = false
        radioButton4.isClickable = false


        // Set up screen-tap listener on root view
        findViewById<View>(android.R.id.content).setOnClickListener {
            val intent = Intent(this, Feature4Activity::class.java)
            startActivity(intent)

            // Optional: Add transition animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
