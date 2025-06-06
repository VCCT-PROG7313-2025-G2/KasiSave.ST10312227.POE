package com.example.kasisave

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.data.LeaderboardAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class LeaderboardUser(
    val username: String = "",
    val coins: Long = 0,
    val avatarName: String = ""
)

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: LeaderboardAdapter
    private lateinit var etInviteUsername: EditText
    private lateinit var btnInvite: Button

    private val userList = mutableListOf<LeaderboardUser>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        recyclerView = findViewById(R.id.rvLeaderboard)
        progressBar = findViewById(R.id.progressBarLeaderboard)
        etInviteUsername = findViewById(R.id.etInviteUsername)
        btnInvite = findViewById(R.id.btnInvite)

        adapter = LeaderboardAdapter(userList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnInvite.setOnClickListener {
            val usernameToInvite = etInviteUsername.text.toString().trim()
            if (usernameToInvite.isNotEmpty()) {
                inviteUserToLeaderboard(usernameToInvite)
            } else {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
            }
        }

        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        progressBar.visibility = View.VISIBLE
        val currentUserId = auth.currentUser?.uid ?: return

        val tempList = mutableListOf<LeaderboardUser>()
        var tasksToComplete = 1 // Start with 1 for current user

        // Step 1: Add current user
        firestore.collection("rewards").document(currentUserId).get()
            .addOnSuccessListener { rewardDoc ->
                val coins = rewardDoc.getLong("coins") ?: 0

                firestore.collection("users").document(currentUserId).get()
                    .addOnSuccessListener { userDoc ->
                        val name = userDoc.getString("firstName") ?: "You"
                        val avatar = userDoc.getString("avatar") ?: "default_avatar"
                        tempList.add(LeaderboardUser(username = name, coins = coins, avatarName = avatar))
                        checkIfAllTasksDone(--tasksToComplete, tempList)
                    }
                    .addOnFailureListener {
                        Log.e("LeaderboardActivity", "Failed to load current user info")
                        checkIfAllTasksDone(--tasksToComplete, tempList)
                    }
            }
            .addOnFailureListener {
                Log.e("LeaderboardActivity", "Failed to load current user rewards")
                checkIfAllTasksDone(--tasksToComplete, tempList)
            }

        // Step 2: Add invited users
        firestore.collection("users")
            .whereEqualTo("invitedBy", currentUserId)
            .get()
            .addOnSuccessListener { invitedUsersSnapshot ->
                val invitedUserIds = invitedUsersSnapshot.documents.map { it.id }
                tasksToComplete += invitedUserIds.size

                if (invitedUserIds.isEmpty()) {
                    checkIfAllTasksDone(tasksToComplete, tempList) // triggers check from current user
                }

                for (userId in invitedUserIds) {
                    firestore.collection("rewards").document(userId).get()
                        .addOnSuccessListener { rewardDoc ->
                            val coins = rewardDoc.getLong("coins") ?: 0

                            firestore.collection("users").document(userId).get()
                                .addOnSuccessListener { userDoc ->
                                    val name = userDoc.getString("firstName") ?: "Unnamed"
                                    val avatar = userDoc.getString("avatar") ?: "default_avatar"
                                    tempList.add(LeaderboardUser(username = name, coins = coins, avatarName = avatar))
                                    checkIfAllTasksDone(--tasksToComplete, tempList)
                                }
                                .addOnFailureListener {
                                    Log.e("LeaderboardActivity", "Failed to load invited user $userId info")
                                    checkIfAllTasksDone(--tasksToComplete, tempList)
                                }
                        }
                        .addOnFailureListener {
                            Log.e("LeaderboardActivity", "Failed to load invited user $userId rewards")
                            checkIfAllTasksDone(--tasksToComplete, tempList)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LeaderboardActivity", "Failed to load invited users: ${e.message}")
                checkIfAllTasksDone(--tasksToComplete, tempList)
            }
    }

    private fun checkIfAllTasksDone(remaining: Int, users: List<LeaderboardUser>) {
        if (remaining <= 0) {
            userList.clear()
            userList.addAll(users.sortedByDescending { it.coins })
            adapter.notifyDataSetChanged()
            progressBar.visibility = View.GONE
        }
    }

    private fun inviteUserToLeaderboard(username: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .whereEqualTo("email", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val invitedUserId = querySnapshot.documents.first().id

                    firestore.collection("users").document(invitedUserId)
                        .update("invitedBy", currentUserId)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User invited!", Toast.LENGTH_SHORT).show()
                            etInviteUsername.text.clear()
                            loadLeaderboard()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to invite user: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error searching for user: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
