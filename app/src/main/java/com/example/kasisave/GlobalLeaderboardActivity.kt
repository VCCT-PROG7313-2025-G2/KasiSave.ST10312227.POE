package com.example.kasisave

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.data.LeaderboardAdapter
import com.google.firebase.firestore.FirebaseFirestore

class GlobalLeaderboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: LeaderboardAdapter
    private val userList = mutableListOf<LeaderboardUser>()

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_leaderboard)

        recyclerView = findViewById(R.id.rvGlobalLeaderboard)
        progressBar = findViewById(R.id.progressBarGlobalLeaderboard)

        adapter = LeaderboardAdapter(userList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("rewards")
            .get()
            .addOnSuccessListener { rewardsSnapshot ->
                val tempList = mutableListOf<LeaderboardUser>()
                val totalUsers = rewardsSnapshot.documents.size
                if (totalUsers == 0) {
                    progressBar.visibility = View.GONE
                    return@addOnSuccessListener
                }

                var usersProcessed = 0
                for (rewardDoc in rewardsSnapshot.documents) {
                    val userId = rewardDoc.id
                    val coins = rewardDoc.getLong("coins") ?: 0

                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("firstName") ?: "Unnamed"
                            val avatarName = userDoc.getString("avatar") ?: "default_avatar"

                            tempList.add(
                                LeaderboardUser(
                                    username = username,
                                    coins = coins,
                                    avatarName = avatarName
                                )
                            )

                            usersProcessed++
                            if (usersProcessed == totalUsers) {
                                updateLeaderboard(tempList)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("GlobalLeaderboard", "Failed to get user data for $userId", it)
                            usersProcessed++
                            if (usersProcessed == totalUsers) {
                                updateLeaderboard(tempList)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GlobalLeaderboard", "Error loading rewards", e)
                Toast.makeText(this, "Failed to load leaderboard.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
            }
    }

    private fun updateLeaderboard(tempList: List<LeaderboardUser>) {
        userList.clear()
        userList.addAll(tempList.sortedByDescending { it.coins })
        adapter.notifyDataSetChanged()
        progressBar.visibility = View.GONE
    }
}
