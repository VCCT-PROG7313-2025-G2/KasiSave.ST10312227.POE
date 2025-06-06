package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RewardsActivity : AppCompatActivity() {

    private lateinit var tvCoinCount: TextView
    private lateinit var ivCoinIcon: ImageView
    private lateinit var btnInvitedLeaderboard: Button
    private lateinit var btnGlobalLeaderboard: Button

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rewards)

        tvCoinCount = findViewById(R.id.tvCoinCount)
        ivCoinIcon = findViewById(R.id.ivCoinIcon)
        btnInvitedLeaderboard = findViewById(R.id.btnInvitedLeaderboard)
        btnGlobalLeaderboard = findViewById(R.id.btnGlobalLeaderboard)
        loadUserRewards()

        btnInvitedLeaderboard.setOnClickListener {
            val intent = Intent(this, LeaderboardActivity::class.java)
            startActivity(intent)
        }
        btnGlobalLeaderboard.setOnClickListener {
            val intent = Intent(this, GlobalLeaderboardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserRewards() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("RewardsActivity", "User not logged in")
            return
        }

        val rewardsRef = firestore.collection("rewards").document(userId)
        rewardsRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val coins = document.getLong("coins") ?: 0
                    tvCoinCount.text = coins.toString()
                } else {
                    tvCoinCount.text = "0"
                }
            }
            .addOnFailureListener { e ->
                Log.e("RewardsActivity", "Failed to load rewards: ${e.message}")
                tvCoinCount.text = "0"
            }
    }
}
