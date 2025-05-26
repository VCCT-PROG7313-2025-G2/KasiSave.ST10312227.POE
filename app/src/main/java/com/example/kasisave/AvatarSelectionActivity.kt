package com.example.kasisave

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AvatarSelectionActivity : AppCompatActivity() {

    private lateinit var avatarViews: List<Pair<String, ImageView>>
    private lateinit var saveButton: Button

    private val db = FirebaseFirestore.getInstance()
    private var userId: String? = null
    private var selectedAvatar: String? = null
    private var selectedImageView: ImageView? = null
    private var isFromSignUp: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_avatar_selection)

        userId = intent.getStringExtra("userId")
        isFromSignUp = intent.getBooleanExtra("isFromSignUp", false)

        if (!isFromSignUp && userId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        avatarViews = listOf(
            "avatar1" to findViewById(R.id.avatar1),
            "avatar2" to findViewById(R.id.avatar2),
            "avatar3" to findViewById(R.id.avatar3),
            "avatar4" to findViewById(R.id.avatar4),
            "avatar5" to findViewById(R.id.avatar5),
            "avatar6" to findViewById(R.id.avatar6),
            "avatar7" to findViewById(R.id.avatar7),
            "avatar8" to findViewById(R.id.avatar8),
            "avatar9" to findViewById(R.id.avatar9)
        )

        avatarViews.forEach { (avatarName, imageView) ->
            imageView.setOnClickListener {
                selectAvatar(avatarName, imageView)
            }
        }

        saveButton = findViewById(R.id.saveAvatarButton)
        saveButton.setOnClickListener {
            if (selectedAvatar == null) {
                Toast.makeText(this, "Please select an avatar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isFromSignUp) {
                // Send selected avatar back to SignupActivity
                val resultIntent = Intent()
                resultIntent.putExtra("selectedAvatar", selectedAvatar)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                // Update Firestore and go to Dashboard
                saveAvatarToFirestore()
            }
        }
    }

    private fun selectAvatar(avatarName: String, imageView: ImageView) {
        // Clear previous selection
        avatarViews.forEach { (_, view) ->
            view.setBackgroundResource(0)
        }

        // Highlight selected avatar
        imageView.setBackgroundResource(R.drawable.avatar_selected_border)
        selectedAvatar = avatarName
        selectedImageView = imageView
    }

    private fun saveAvatarToFirestore() {
        db.collection("users").document(userId!!)
            .update("avatar", selectedAvatar)
            .addOnSuccessListener {
                Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save avatar", Toast.LENGTH_LONG).show()
            }
    }
}
