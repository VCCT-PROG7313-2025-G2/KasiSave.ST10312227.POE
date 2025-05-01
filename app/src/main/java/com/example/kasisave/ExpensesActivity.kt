package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import com.example.kasisave.activities.AddExpenseActivity
import com.example.kasisave.activities.FindExpenseByCategoryActivity


class ExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noExpensesTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var searchByDateButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnSearchByCategory: Button

    private lateinit var adapter: ExpenseAdapter
    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        // Initialize views
        recyclerView = findViewById(R.id.expensesRecyclerView)
        noExpensesTextView = findViewById(R.id.noExpensesTextView)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        searchByDateButton = findViewById(R.id.btnSearchByDate)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)


        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(mutableListOf())
        recyclerView.adapter = adapter

        // Initialize database
        db = ExpenseDatabase.getDatabase(this)

        // Load expenses
        loadExpenses()

        // Add Expense button click
        addExpenseButton.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            startActivity(intent)
        }

        // Search by date button click
        searchByDateButton.setOnClickListener {
            val intent = Intent(this, SearchExpenseByDateActivity::class.java)
            startActivity(intent)
        }

        // Bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }
                R.id.navigation_income -> {
                    startActivity(Intent(this, IncomeActivity::class.java))
                    true
                }
                R.id.navigation_expenses -> true
                R.id.navigation_milestones -> {
                    startActivity(Intent(this, MilestonesActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = db.expenseDao().getAllExpenses()
            if (expenses.isEmpty()) {
                recyclerView.visibility = View.GONE
                noExpensesTextView.visibility = View.VISIBLE
                totalExpensesTextView.text = "Total: R 0.00"
            } else {
                recyclerView.visibility = View.VISIBLE
                noExpensesTextView.visibility = View.GONE
                adapter.setExpenses(expenses)
                val total = expenses.sumOf { it.amount }
                totalExpensesTextView.text = "Total: R %.2f".format(total)
            }
        }
    }
}
