package com.example.kasisave.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.Expense
import com.example.kasisave.ExpenseAdapter
import com.example.kasisave.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class FindExpenseByCategoryActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var findButton: Button
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var totalCostTextView: TextView
    private lateinit var adapter: ExpenseAdapter

    private val firestore = FirebaseFirestore.getInstance()

    // If you need to filter by user, retrieve userId from intent or auth
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_expense_by_category)

        categorySpinner = findViewById(R.id.categorySpinner)
        findButton = findViewById(R.id.findButton)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        totalCostTextView = findViewById(R.id.totalCostTextView)

        userId = intent.getStringExtra("userId")

        setupCategorySpinner()
        setupRecyclerView()

        findButton.setOnClickListener {
            val selectedCategory = categorySpinner.selectedItem?.toString()
            if (!selectedCategory.isNullOrEmpty()) {
                fetchExpensesByCategory(selectedCategory)
            } else {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.expense_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun setupRecyclerView() {
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(emptyList())
        expensesRecyclerView.adapter = adapter
    }

    private fun fetchExpensesByCategory(category: String) {
        var query = firestore.collection("expenses").whereEqualTo("category", category)

        // Optional: filter by user
        userId?.let {
            query = query.whereEqualTo("userId", it)
        }

        query.get()
            .addOnSuccessListener { documents ->
                handleExpensesQueryResult(documents)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching expenses: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleExpensesQueryResult(documents: QuerySnapshot) {
        val expenses = documents.documents.mapNotNull { doc ->
            try {
                val id = doc.id
                val description = doc.getString("description") ?: ""
                val amount = doc.getDouble("amount") ?: 0.0
                val category = doc.getString("category") ?: ""
                val dateMillis = doc.getLong("dateMillis") ?: 0L

                Expense(
                    id = id,
                    description = description,
                    amount = amount,
                    category = category,
                    dateMillis = dateMillis
                )
            } catch (ex: Exception) {
                null
            }
        }

        adapter.updateData(expenses)

        val total = expenses.sumOf { it.amount }
        totalCostTextView.text = "Total: R${"%.2f".format(total)}"

        if (expenses.isEmpty()) {
            Toast.makeText(this, "No expenses found for this category.", Toast.LENGTH_SHORT).show()
        }
    }
}
