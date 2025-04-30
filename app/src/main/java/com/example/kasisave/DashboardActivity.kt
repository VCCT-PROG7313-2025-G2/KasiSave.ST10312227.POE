package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var expenseProgressBar: ProgressBar
    private lateinit var budgetPercentText: TextView
    private lateinit var expensePercentText: TextView
    private lateinit var totalBalanceText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pieChart: PieChart
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
        pieChart = findViewById(R.id.pieChart)

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

                val calendar = Calendar.getInstance()
                val month = (calendar.get(Calendar.MONTH) + 1).toString()
                val year = calendar.get(Calendar.YEAR)

                // Fetching the milestone data for the current month and year
                val currentMilestone = db.milestoneDao().getMilestoneForMonth(month, year)

                val baseBudget = currentMilestone?.targetAmount ?: totalIncome

                val budgetPercent = if (baseBudget > 0) 100 else 0
                val expensePercent = if (baseBudget > 0) {
                    ((totalExpenses / baseBudget) * 100).coerceAtMost(100.0).toInt()
                } else {
                    0
                }

                animateProgress(budgetProgressBar, budgetPercent, budgetPercentText)
                animateProgress(expenseProgressBar, expensePercent, expensePercentText)

                setupPieChart(totalIncome, totalExpenses)

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

    private fun setupPieChart(income: Double, expenses: Double) {
        val entries = ArrayList<PieEntry>()

        if (income > 0) entries.add(PieEntry(income.toFloat(), "Income"))
        if (expenses > 0) entries.add(PieEntry(expenses.toFloat(), "Expenses"))

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = resources.getColor(android.R.color.white, null)

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.setDrawHoleEnabled(true)
        pieChart.holeRadius = 40f
        pieChart.setTransparentCircleRadius(45f)
        pieChart.setEntryLabelColor(resources.getColor(android.R.color.white, null))
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setDrawEntryLabels(true)

        val desc = Description()
        desc.text = ""
        pieChart.description = desc
        pieChart.legend.isEnabled = true

        pieChart.animateY(1000)
        pieChart.invalidate()
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
