package com.example.kasisave

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class RewardsExpensesActivity : AppCompatActivity() {

    private lateinit var tvCongrats: TextView
    private lateinit var konfettiView: KonfettiView
    private lateinit var rewardAnimation: LottieAnimationView
    private lateinit var btnBack: MaterialButton

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards_expenses)

        tvCongrats = findViewById(R.id.tvCongrats)
        konfettiView = findViewById(R.id.konfettiView)
        rewardAnimation = findViewById(R.id.rewardAnimation)
        btnBack = findViewById(R.id.btnBackToExpenses)

        incrementUserRewards()

        rewardAnimation.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                showCongratsAndKonfetti()
            }
        })

        btnBack.setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
            finish()
        }
    }

    private fun incrementUserRewards() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("RewardsExpensesActivity", "User not logged in, cannot increment rewards")
            return
        }

        val rewardsRef = firestore.collection("rewards").document(userId)

        rewardsRef.update("coins", FieldValue.increment(25))
            .addOnSuccessListener {
                Log.d("RewardsExpensesActivity", "Successfully incremented rewards by 25 points")
                playRewardSound()
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("NOT_FOUND") == true || e.message?.contains("No document") == true) {
                    val newReward = hashMapOf("coins" to 25)
                    rewardsRef.set(newReward)
                        .addOnSuccessListener {
                            Log.d("RewardsExpensesActivity", "Rewards document created with 25 points")
                            playRewardSound()
                        }
                        .addOnFailureListener { err ->
                            Log.e("RewardsExpensesActivity", "Failed to create rewards document: ${err.message}")
                        }
                } else {
                    Log.e("RewardsExpensesActivity", "Failed to update rewards: ${e.message}")
                }
            }
    }

    private fun playRewardSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.rewards_sound)
        mediaPlayer?.start()
    }

    private fun showCongratsAndKonfetti() {
        tvCongrats.visibility = View.VISIBLE

        val party = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(
                0xfff44336.toInt(),
                0xff4caf50.toInt(),
                0xff2196f3.toInt(),
                0xffffeb3b.toInt()
            ),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50),
            position = Position.Relative(0.5, 0.0)
        )

        konfettiView.start(party)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
