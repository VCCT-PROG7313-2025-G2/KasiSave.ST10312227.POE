package com.example.kasisave

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.databinding.ActivityRewardsMilestonesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class RewardsMilestonesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsMilestonesBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsMilestonesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackToMilestones.setOnClickListener {
            val intent = Intent(this, MilestonesActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Play animation
        binding.rewardAnimation.addLottieOnCompositionLoadedListener {
            binding.rewardAnimation.playAnimation()
        }

        // Delay message and konfetti
        binding.rewardAnimation.postDelayed({
            showCongratsMessage()
            startKonfetti()
            incrementUserRewards()
        }, 1500)
    }

    private fun showCongratsMessage() {
        binding.tvCongrats.visibility = View.VISIBLE
    }

    private fun startKonfetti() {
        val party = Party(
            speed = 0f,
            maxSpeed = 50f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(0xFFE6B422.toInt(), 0xFF2E8B57.toInt(), 0xFFFFA500.toInt()),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(50),
            position = Position.Relative(0.5, 0.0)
        )

        binding.konfettiView.start(party)
    }

    private fun incrementUserRewards() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return
        }

        val rewardsRef = firestore.collection("rewards").document(userId)

        rewardsRef.update("coins", FieldValue.increment(15))
            .addOnSuccessListener {
                playRewardSound()
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("NOT_FOUND") == true || e.message?.contains("No document") == true) {
                    val newReward = hashMapOf("coins" to 15)
                    rewardsRef.set(newReward)
                        .addOnSuccessListener {
                            playRewardSound()
                        }
                }
            }
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
