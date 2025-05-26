package com.example.kasisave

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kasisave.databinding.ActivityCategoriesBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class CategoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoriesBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: CategoriesAdapter
    private var listener: ListenerRegistration? = null
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var userId: String

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Nullable Longs for the selected date range
    private var startMillis: Long? = null
    private var endMillis: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNavigationView = binding.bottomNavigation
        setupBottomNavigation()

        userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize adapter with date range and firebase instances
        adapter = CategoriesAdapter(
            items = mutableListOf(),
            startDate = startMillis,
            endDate = endMillis,
            firestore = firestore,
            auth = auth
        ) { categoryName ->
            if (startMillis != null && endMillis != null) {
                val total = adapter.getTotalForCategory(categoryName)
                showTotalDialog(categoryName, total, startMillis!!, endMillis!!)
            } else {
                Toast.makeText(this, "Please select a date range first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvCategories.layoutManager = LinearLayoutManager(this)
        binding.rvCategories.adapter = adapter

        val defaults = resources.getStringArray(R.array.expense_categories).toList()

        // Listen for user categories and update adapter
        listener = firestore.collection("users")
            .document(userId)
            .collection("categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CategoriesDebug", "Error loading categories: ${error.message}", error)
                    Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val userNames = snapshot?.documents.orEmpty().mapNotNull { it.getString("name") }
                val combined = defaults + userNames.filter { it !in defaults }
                adapter.update(combined)
            }

        // Add new category on button click
        binding.btnAddCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firestore.collection("users")
                .document(userId)
                .collection("categories")
                .add(mapOf("name" to name))
                .addOnSuccessListener {
                    binding.etCategoryName.text?.clear()
                    Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Add failed", Toast.LENGTH_SHORT).show()
                    Log.e("CategoriesDebug", "Failed to add category", it)
                }
        }

        // Date range selection button
        binding.btnSelectDateRange.setOnClickListener {
            pickStartDate()
        }
    }

    private fun pickStartDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val startCal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            startMillis = startCal.timeInMillis

            // Update adapter's startDate
            adapter.startDate = startMillis

            pickEndDate()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickEndDate() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val endCal = Calendar.getInstance().apply {
                set(year, month, day, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
            endMillis = endCal.timeInMillis

            // Update adapter's endDate
            adapter.endDate = endMillis

            val start = dateFormat.format(startMillis)
            val end = dateFormat.format(endMillis)
            Toast.makeText(this, "Date Range: $start to $end", Toast.LENGTH_SHORT).show()

            // Clear cached totals safely via adapter method
            adapter.clearTotals()
            adapter.notifyDataSetChanged()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTotalDialog(category: String, total: Double, startMillis: Long, endMillis: Long) {
        val startStr = dateFormat.format(startMillis)
        val endStr = dateFormat.format(endMillis)

        AlertDialog.Builder(this)
            .setTitle("Total Spent on $category")
            .setMessage("From $startStr to $endStr:\nR %.2f".format(total))
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_categories -> true
                R.id.navigation_expenses -> {
                    startActivity(Intent(this, ExpensesActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_income -> {
                    startActivity(Intent(this, IncomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_milestones -> {
                    startActivity(Intent(this, MilestonesActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
