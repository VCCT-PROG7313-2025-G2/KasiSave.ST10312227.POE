package com.example.kasisave

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class SearchExpenseByDateActivity : AppCompatActivity() {

    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnFilter: Button
    private lateinit var txtTotalFiltered: TextView
    private lateinit var recyclerFilteredExpenses: RecyclerView
    private lateinit var categorySpinner: Spinner

    private var startDate: Long? = null
    private var endDate: Long? = null

    private lateinit var adapter: ExpenseAdapter
    private val expensesList = mutableListOf<Expense>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_expense_by_date)

        // Initialize views
        btnStartDate = findViewById(R.id.btnStartDate)
        btnEndDate = findViewById(R.id.btnEndDate)
        btnFilter = findViewById(R.id.btnFilter)
        txtTotalFiltered = findViewById(R.id.txtTotalFiltered)
        recyclerFilteredExpenses = findViewById(R.id.recyclerFilteredExpenses)
        categorySpinner = findViewById(R.id.categorySpinner)

        // Setup Spinner with "All" option
        val categories = mutableListOf("All")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

// Fetch custom categories from Firestore
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .addOnSuccessListener { snapshot ->
                    val custom = snapshot.documents.mapNotNull { it.getString("name") }
                    val defaults = resources.getStringArray(R.array.expense_categories)
                    val combined = (defaults.toList() + custom).distinct()
                    categories.addAll(combined)
                    spinnerAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
        }


        // Setup RecyclerView with fixed adapter constructor
        adapter = ExpenseAdapter(this, expensesList)
        recyclerFilteredExpenses.layoutManager = LinearLayoutManager(this)
        recyclerFilteredExpenses.adapter = adapter

        // Set date picker listeners
        btnStartDate.setOnClickListener {
            showDatePicker { millis ->
                startDate = millis
                btnStartDate.text = "Start: ${dateFormat.format(Date(millis))}"
            }
        }

        btnEndDate.setOnClickListener {
            showDatePicker { millis ->
                endDate = millis
                btnEndDate.text = "End: ${dateFormat.format(Date(millis))}"
            }
        }

        // Filter button
        btnFilter.setOnClickListener {
            if (startDate != null && endDate != null) {
                val selectedCategory = categorySpinner.selectedItem.toString()
                filterExpensesByDateAndCategory(selectedCategory)
            } else {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterExpensesByDateAndCategory(category: String) {
        val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", null)

        if (userId == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        var query: Query = firestore.collection("expenses")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("dateMillis", startDate!!)
            .whereLessThanOrEqualTo("dateMillis", endDate!!)

        if (category != "All") {
            query = query.whereEqualTo("category", category)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val filtered = snapshot.documents.mapNotNull { it.toObject(Expense::class.java) }
                val total = filtered.sumOf { it.amount }

                expensesList.clear()
                expensesList.addAll(filtered)
                adapter.notifyDataSetChanged()

                txtTotalFiltered.text = "Total: R %.2f".format(total)

                if (expensesList.isEmpty()) {
                    Toast.makeText(this, "No expenses found for this selection.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching expenses: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
