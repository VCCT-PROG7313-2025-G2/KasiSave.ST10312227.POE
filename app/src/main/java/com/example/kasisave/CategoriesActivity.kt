package com.example.kasisave

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kasisave.databinding.ActivityCategoriesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: CategoriesAdapter
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Prepare adapter & RecyclerView
        adapter = CategoriesAdapter(mutableListOf())
        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        // 2) Load defaults and start listening for user categories
        val defaults = resources.getStringArray(R.array.expense_categories).toList()
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        Log.d("CategoriesDebug", "User UID: $uid")

        listener = firestore.collection("users")
            .document(uid)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CategoriesDebug", "Error loading categories: ${error.message}", error)
                    Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    Log.w("CategoriesDebug", "No category documents found.")
                } else {
                    Log.d("CategoriesDebug", "Fetched ${snapshot.documents.size} category documents.")
                }

                // Extract firestore names
                val userNames = snapshot
                    ?.documents
                    .orEmpty()
                    .mapNotNull { doc ->
                        val name = doc.getString("name")
                        Log.d("CategoriesDebug", "Document: ${doc.id}, name: $name")
                        name
                    }

                Log.d("CategoriesDebug", "userNames from Firestore: $userNames")
                Log.d("CategoriesDebug", "defaults from XML: $defaults")

                // Merge: defaults first, then any user adds that aren’t in defaults
                val combined = defaults + userNames.filter { it !in defaults }

                Log.d("CategoriesDebug", "Final combined list: $combined")

                // Update the adapter
                adapter.update(combined)
            }

        // 3) Add new category
        binding.btnAddCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CategoriesDebug", "Adding category: $name")

            firestore.collection("users")
                .document(uid)
                .collection("categories")
                .add(mapOf("name" to name))
                .addOnSuccessListener {
                    binding.etCategoryName.text?.clear()
                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                    Log.d("CategoriesDebug", "Successfully added category: $name")
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Add failed", Toast.LENGTH_SHORT).show()
                    Log.e("CategoriesDebug", "Failed to add category", it)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }
}
