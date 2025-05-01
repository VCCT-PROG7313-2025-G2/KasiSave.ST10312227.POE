package com.example.kasisave.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.Expense
import com.example.kasisave.ExpenseAdapter
import com.example.kasisave.ExpenseDatabase
import com.example.kasisave.R
import kotlinx.coroutines.launch

class FindExpenseByCategoryActivity : AppCompatActivity() {

    private lateinit var categorySpinner: Spinner
    private lateinit var findButton: Button
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var adapter: ExpenseAdapter
    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_expense_by_category)

        categorySpinner = findViewById(R.id.categorySpinner)
        findButton = findViewById(R.id.findButton)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)

        db = ExpenseDatabase.getDatabase(this)

        setupCategorySpinner()
        setupRecyclerView()

        findButton.setOnClickListener {
            val selectedCategory = categorySpinner.selectedItem?.toString()
            if (selectedCategory != null) {
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
        lifecycleScope.launch {
            try {
                val expenses = db.expenseDao().getExpensesByCategory(category)
                adapter.updateData(expenses)
                if (expenses.isEmpty()) {
                    Toast.makeText(this@FindExpenseByCategoryActivity, "No expenses found for this category.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@FindExpenseByCategoryActivity, "Error fetching expenses: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
