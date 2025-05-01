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
import com.example.kasisave.activities.AddExpenseActivity
import com.example.kasisave.activities.FindExpenseByCategoryActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ExpensesActivity : AppCompatActivity() {

    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var totalExpensesTextView: TextView
    private lateinit var noExpensesTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnSearchByCategory: Button

    private lateinit var expenseAdapter: ExpenseAdapter
    private val expensesList = mutableListOf<Expense>()
    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        noExpensesTextView = findViewById(R.id.noExpensesTextView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        btnSearchByCategory = findViewById(R.id.btnSearchByCategory)

        db = ExpenseDatabase.getDatabase(this)

        expenseAdapter = ExpenseAdapter(expensesList)
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expenseAdapter

        loadExpenses()

        addExpenseButton.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        btnSearchByCategory.setOnClickListener {
            startActivity(Intent(this, FindExpenseByCategoryActivity::class.java))
        }

        setupBottomNavigation()
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val loadedExpenses = db.expenseDao().getAllExpenses()
            expensesList.clear()
            expensesList.addAll(loadedExpenses)
            expenseAdapter.notifyDataSetChanged()
            updateTotalExpenses()
            noExpensesTextView.visibility = if (expensesList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun updateTotalExpenses() {
        val total = expensesList.sumOf { it.amount }
        totalExpensesTextView.text = "Total: R %.2f".format(total)
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
                R.id.navigation_expenses -> true
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
        bottomNavigationView.selectedItemId = R.id.navigation_expenses
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }
}
