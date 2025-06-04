package com.example.kasisave

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class RewardsSignupActivity : AppCompatActivity() {

    private lateinit var rewardAnimation: LottieAnimationView
    private lateinit var konfettiView: KonfettiView
    private lateinit var tvCongrats: TextView
    private lateinit var btnBackToDashboard: MaterialButton
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards_signup)

        rewardAnimation = findViewById(R.id.rewardAnimation)
        konfettiView = findViewById(R.id.konfettiView)
        tvCongrats = findViewById(R.id.tvCongrats)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)

        tvCongrats.visibility = View.GONE

        rewardAnimation.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                startConfetti()
                playRewardSound()
            }

            override fun onAnimationEnd(animation: Animator) {
                tvCongrats.visibility = View.VISIBLE
            }
        })

        btnBackToDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }

    private fun startConfetti() {
        val party = Party(
            speed = 10f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(
                0xFFF44336.toInt(), // Red
                0xFFFFC107.toInt(), // Amber
                0xFF4CAF50.toInt(), // Green
                0xFF2196F3.toInt()  // Blue
            ),
            position = Position.Relative(0.5, 0.0),
            emitter = Emitter(duration = 3, TimeUnit.SECONDS).perSecond(100),
            timeToLive = 3000L
        )
        konfettiView.start(party)
    }

    private fun playRewardSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.rewards_sound)
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
