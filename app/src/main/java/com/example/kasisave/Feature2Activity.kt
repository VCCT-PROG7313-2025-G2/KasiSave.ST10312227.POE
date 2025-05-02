package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class Feature2Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature2)

        // Find the RadioGroup and RadioButtons by their IDs
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroup)
        val radioButton2 = findViewById<RadioButton>(R.id.radioButton2)
        val radioButton5 = findViewById<RadioButton>(R.id.radioButton5)
        val radioButton6 = findViewById<RadioButton>(R.id.radioButton6)
        val radioButton4 = findViewById<RadioButton>(R.id.radioButton4)

        // Setup Next button logic
        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            val intent = Intent(this, Feature3Activity::class.java)
            startActivity(intent)

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
