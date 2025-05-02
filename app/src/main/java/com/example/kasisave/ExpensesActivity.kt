package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.activities.AddExpenseActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noExpensesTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var searchByDateButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var adapter: ExpenseAdapter
    private lateinit var db: ExpenseDatabase
    private var userId: Int = -1

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

        // Retrieve user ID
        val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
        userId = sharedPrefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Load expenses
        loadExpenses()

        // Add Expense button click
        addExpenseButton.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        // Search by date button click
        searchByDateButton.setOnClickListener {
            startActivity(Intent(this, SearchExpenseByDateActivity::class.java))
        }

        // Bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_income -> {
                    val intent = Intent(this, IncomeActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_expenses -> true
                R.id.navigation_milestones -> {
                    val intent = Intent(this, MilestonesActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
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
            if (userId == -1) {
                Toast.makeText(this@ExpensesActivity, "Please log in again.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ExpensesActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            val expenses = db.expenseDao().getExpensesForUser(userId)

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
