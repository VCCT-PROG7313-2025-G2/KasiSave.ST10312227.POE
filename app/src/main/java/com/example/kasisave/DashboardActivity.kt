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
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate



class DashboardActivity : AppCompatActivity() {

    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var expenseProgressBar: ProgressBar
    private lateinit var budgetPercentText: TextView
    private lateinit var expensePercentText: TextView
    private lateinit var totalBalanceText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var db: ExpenseDatabase
    private lateinit var pieChart: PieChart


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        pieChart = findViewById(R.id.pieChart)


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

    private fun updatePieChart(income: Double, expenses: Double) {
        val entries = ArrayList<PieEntry>()

        if (income > 0) entries.add(PieEntry(income.toFloat(), "Income"))
        if (expenses > 0) entries.add(PieEntry(expenses.toFloat(), "Expenses"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            getColor(R.color.green_primary),
            getColor(R.color.red) // Add red to colors.xml if not defined
        )

        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = getColor(R.color.white)

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(android.R.color.transparent)
        pieChart.setUsePercentValues(true)
        pieChart.setEntryLabelColor(getColor(R.color.white))
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
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
                updatePieChart(totalIncome, totalExpenses)
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
