package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var expenseProgressBar: ProgressBar
    private lateinit var budgetPercentText: TextView
    private lateinit var expensePercentText: TextView
    private lateinit var totalBalanceText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize views
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        expenseProgressBar = findViewById(R.id.expenseProgressBar)
        budgetPercentText = findViewById(R.id.budgetPercentText)
        expensePercentText = findViewById(R.id.expensePercentText)
        totalBalanceText = findViewById(R.id.totalBalanceAmount)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        db = ExpenseDatabase.getDatabase(this)

        setupBottomNavigation()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val totalIncome = db.incomeDao().getTotalIncome() ?: 0.0
                val totalExpenses = db.expenseDao().getTotalExpenses() ?: 0.0

                val totalBalance = totalIncome - totalExpenses
                totalBalanceText.text = "R${"%.2f".format(totalBalance)}"

                val budgetPercent = if (totalIncome > 0) 100 else 0
                val expensePercent = if (totalIncome > 0)
                    ((totalExpenses / totalIncome) * 100).coerceAtMost(100.0).toInt()
                else 0

                animateProgress(budgetProgressBar, budgetPercent, budgetPercentText)
                animateProgress(expenseProgressBar, expensePercent, expensePercentText)
            } catch (e: Exception) {
                e.printStackTrace()
                totalBalanceText.text = "Error loading data"
            }
        }
    }


    private suspend fun animateProgress(
        progressBar: ProgressBar,
        targetProgress: Int,
        percentText: TextView
    ) {
        for (progress in 0..targetProgress) {
            progressBar.progress = progress
            percentText.text = "$progress%"
            delay(15)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> true
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
                else -> false
            }
        }
    }
}
