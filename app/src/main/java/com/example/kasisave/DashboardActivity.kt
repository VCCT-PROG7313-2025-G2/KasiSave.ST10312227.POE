package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var totalBalanceText: TextView
    private lateinit var budgetInfoText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var db: ExpenseDatabase
    private lateinit var logOutButton: Button
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        userId = intent.getIntExtra("userId", -1)
        if (userId == -1) {
            finish()
            return
        }

        totalBalanceText = findViewById(R.id.totalBalanceAmount)
        budgetInfoText = findViewById(R.id.budgetInfoText)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        logOutButton = findViewById(R.id.logoutButton)  // Button reference

        db = ExpenseDatabase.getDatabase(this)

        setupBottomNavigation()
        loadDashboardData()

        // Log-out button click listener
        logOutButton.setOnClickListener {
            logOut()
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val month = (calendar.get(Calendar.MONTH) + 1).toString()
                val year = calendar.get(Calendar.YEAR)

                val totalIncome: Double
                val totalExpenses: Double
                val currentMilestone: Milestone?
                val categoryTotals: List<ExpenseDao.CategoryTotal>

                withContext(Dispatchers.IO) {
                    totalIncome = db.incomeDao().getTotalIncomeForUser(userId) ?: 0.0
                    totalExpenses = db.expenseDao().getTotalExpensesForUser(userId) ?: 0.0
                    currentMilestone = db.milestoneDao().getMilestoneForMonthAndUser(month, year, userId)
                    categoryTotals = db.expenseDao().getTotalAmountByCategoryForUser(userId)
                }

                val totalBalance = totalIncome - totalExpenses
                totalBalanceText.text = "R${"%.2f".format(totalBalance)}"

                val budgetTarget = currentMilestone?.targetAmount ?: totalIncome
                val remainingBudget = budgetTarget - totalExpenses

                val budgetStatus = when {
                    remainingBudget < 0 -> "⚠️ You are over budget by R${"%.2f".format(-remainingBudget)}"
                    remainingBudget == 0.0 -> "⚠️ You have reached your budget limit."
                    else -> "✅ You have R${"%.2f".format(remainingBudget)} remaining in your budget."
                }

                budgetInfoText.text = """
                    Budget Overview:
                    • Budget Target: R${"%.2f".format(budgetTarget)}
                    • Total Expenses: R${"%.2f".format(totalExpenses)}
                    • $budgetStatus
                """.trimIndent()

                setupPieChart(totalIncome, totalExpenses)
                setupBarChart(categoryTotals)

            } catch (e: Exception) {
                e.printStackTrace()
                totalBalanceText.text = "Error loading data"
                budgetInfoText.text = "Could not load budget overview."
            }
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

    private fun setupBarChart(categoryTotals: List<ExpenseDao.CategoryTotal>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        categoryTotals.forEachIndexed { index, item ->
            entries.add(BarEntry(index.toFloat(), item.total.toFloat()))
            labels.add(item.category)
        }

        val dataSet = BarDataSet(entries, "Expenses by Category").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            valueTextSize = 12f
        }

        val barData = BarData(dataSet)
        barChart.data = barData

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            labelRotationAngle = -45f
        }

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.navigation_dashboard -> return@setOnItemSelectedListener true
                R.id.navigation_expenses -> Intent(this, ExpensesActivity::class.java)
                R.id.navigation_income -> Intent(this, IncomeActivity::class.java)
                R.id.navigation_milestones -> Intent(this, MilestonesActivity::class.java)
                else -> null
            }
            intent?.putExtra("userId", userId)
            intent?.let {
                startActivity(it)
                overridePendingTransition(0, 0)
                finish()
            }
            true
        }
    }

    // Log out method
    private fun logOut() {
        // Clear user session data or preferences
        val sharedPreferences = getSharedPreferences("KasiSavePrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            remove("userId")
            apply()
        }

        // Redirect to login screen
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
