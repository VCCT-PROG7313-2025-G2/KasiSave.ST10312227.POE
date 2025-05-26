package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class Feature1Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature1)

        val skipButton: ImageButton = findViewById(R.id.skipButton)
        skipButton.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val nextButton: Button = findViewById(R.id.nextButton)

        nextButton.setOnClickListener {
            val intent = Intent(this@Feature1Activity, Feature2Activity::class.java)
            startActivity(intent)

            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
