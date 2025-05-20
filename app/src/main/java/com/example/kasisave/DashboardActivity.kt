package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var totalBalanceText: TextView
    private lateinit var budgetInfoText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var logOutButton: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        totalBalanceText = findViewById(R.id.totalBalanceAmount)
        budgetInfoText = findViewById(R.id.budgetInfoText)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        logOutButton = findViewById(R.id.logoutButton)

        if (currentUserUid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()
        loadDashboardData()

        logOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val uid = currentUserUid ?: return@launch

                val calendar = Calendar.getInstance()
                val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val year = calendar.get(Calendar.YEAR)

                // Fetch total income
                val incomeQuery = firestore.collection("incomes")
                    .whereEqualTo("userId", uid)
                val incomeSnapshots = incomeQuery.get().await()
                Log.d("DashboardActivity", "Fetched ${incomeSnapshots.size()} income records for UID: $uid")

                val totalIncome = incomeSnapshots.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                // Fetch total expenses
                val expenseQuery = firestore.collection("expenses")
                    .whereEqualTo("userId", uid)
                val expenseSnapshots = expenseQuery.get().await()
                val totalExpenses = expenseSnapshots.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                // Fetch current milestone
                val milestoneQuery = firestore.collection("milestones")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("month", month)
                    .whereEqualTo("year", year)
                    .limit(1)
                val milestoneSnapshot = milestoneQuery.get().await()
                val currentMilestone = milestoneSnapshot.documents.firstOrNull()
                val budgetTarget = currentMilestone?.getDouble("targetAmount") ?: totalIncome

                val remainingBudget = budgetTarget - totalExpenses
                val budgetStatus = when {
                    remainingBudget < 0 -> "⚠️ You are over budget by R${"%.2f".format(-remainingBudget)}"
                    remainingBudget == 0.0 -> "⚠️ You have reached your budget limit."
                    else -> "✅ You have R${"%.2f".format(remainingBudget)} remaining in your budget."
                }

                totalBalanceText.text = "R${"%.2f".format(totalIncome - totalExpenses)}"
                budgetInfoText.text = """
                    Budget Overview:
                    • Budget Target: R${"%.2f".format(budgetTarget)}
                    • Total Expenses: R${"%.2f".format(totalExpenses)}
                    • $budgetStatus
                """.trimIndent()

                // Expenses grouped by category
                val categories = expenseSnapshots.documents.groupBy {
                    it.getString("category") ?: "Other"
                }.map { (category, docs) ->
                    category to docs.sumOf { it.getDouble("amount") ?: 0.0 }
                }

                setupIncomeVsExpenseBarChart(totalIncome, totalExpenses)
                setupCategoryPieChart(categories)

            } catch (e: Exception) {
                e.printStackTrace()
                totalBalanceText.text = "Error loading data"
                budgetInfoText.text = "Could not load budget overview."
            }
        }
    }

    private fun setupIncomeVsExpenseBarChart(income: Double, expenses: Double) {
        val entries = listOf(
            BarEntry(0f, income.toFloat()),
            BarEntry(1f, expenses.toFloat())
        )
        val labels = listOf("Income", "Expenses")

        val dataSet = BarDataSet(entries, "Income vs Expenses").apply {
            colors = listOf(
                resources.getColor(R.color.teal_700, null),
                resources.getColor(R.color.purple_500, null)
            )
            valueTextSize = 14f
        }

        val barData = BarData(dataSet)
        barChart.data = barData

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
        }

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.setFitBars(true)
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupCategoryPieChart(categoryTotals: List<Pair<String, Double>>) {
        val entries = categoryTotals.map { (category, total) ->
            PieEntry(total.toFloat(), category)
        }

        val dataSet = PieDataSet(entries, "Expense Breakdown").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 14f
            valueTextColor = resources.getColor(android.R.color.white, null)
        }

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
            val intent = when (item.itemId) {
                R.id.navigation_dashboard -> return@setOnItemSelectedListener true
                R.id.navigation_expenses -> Intent(this, ExpensesActivity::class.java)
                R.id.navigation_income -> Intent(this, IncomeActivity::class.java)
                R.id.navigation_milestones -> Intent(this, MilestonesActivity::class.java)
                else -> null
            }
            intent?.let {
                startActivity(it)
                overridePendingTransition(0, 0)
                finish()
            }
            true
        }
    }
}
