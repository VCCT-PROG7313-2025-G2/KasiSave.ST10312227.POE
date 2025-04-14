package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity

class Feature4Activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature4)

        // Set up screen-tap listener
        findViewById<View>(android.R.id.content).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
