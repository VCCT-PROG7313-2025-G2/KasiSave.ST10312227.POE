package com.example.kasisave

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.kasisave.databinding.ActivityProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Load profile data from Firestore
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.firstNameInput.setText(document.getString("firstName") ?: "")
                        binding.lastNameInput.setText(document.getString("lastName") ?: "")
                        binding.contactInput.setText(document.getString("contactNumber") ?: "")

                        // Load avatar image from drawable using stored avatar name
                        val avatarName = document.getString("avatar") ?: "ic_profile"
                        val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                        if (resId != 0) {
                            binding.profileImageView.setImageResource(resId)
                        } else {
                            binding.profileImageView.setImageResource(R.drawable.ic_profile) // fallback
                        }
                    } else {
                        Toast.makeText(this, "Profile not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show()
                }

            // Update profile in Firestore
            binding.updateProfileButton.setOnClickListener {
                val firstName = binding.firstNameInput.text.toString().trim()
                val lastName = binding.lastNameInput.text.toString().trim()
                val contact = binding.contactInput.text.toString().trim()

                if (firstName.isEmpty() || lastName.isEmpty() || contact.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else {
                    val updatedData = mapOf(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "contactNumber" to contact
                        // avatar is not updated here — assumed set during sign-up or avatar change
                    )

                    firestore.collection("users").document(userId)
                        .update(updatedData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Update failed.", Toast.LENGTH_SHORT).show()
                        }
                }
            }

        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
