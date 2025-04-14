package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Feature1Activity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature1)

        // Initialize gesture detector
        gestureDetector = GestureDetector(this, SingleTapListener())

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Delegate touch events to gesture detector
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }

    inner class SingleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Start Feature2Activity on any screen tap
            val intent = Intent(this@Feature1Activity, Feature2Activity::class.java)
            startActivity(intent)
            return true
        }
    }

}
