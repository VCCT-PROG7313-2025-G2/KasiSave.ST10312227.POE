package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import androidx.lifecycle.lifecycleScope

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var totalBalanceText: TextView
    private lateinit var budgetInfoText: TextView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserUid: String?
        get() = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navigationView.setNavigationItemSelectedListener(this)

        // ⬇️ Update Navigation Drawer Header with Username & Email
        val headerView = navigationView.getHeaderView(0)
        val usernameTextView = headerView.findViewById<TextView>(R.id.textViewUsername)
        val emailTextView = headerView.findViewById<TextView>(R.id.textViewEmail)

        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        val fallbackEmail = currentUser?.email ?: "user@example.com"

        if (uid != null) {
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val name = document.getString("name") ?: "User"
                    usernameTextView.text = name
                    emailTextView.text = fallbackEmail
                }
                .addOnFailureListener {
                    usernameTextView.text = "User"
                    emailTextView.text = fallbackEmail
                }
        }

        totalBalanceText = findViewById(R.id.totalBalanceAmount)
        budgetInfoText = findViewById(R.id.budgetInfoText)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        if (currentUserUid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()
        loadDashboardData()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val uid = currentUserUid ?: return@launch
                val calendar = Calendar.getInstance()
                val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val year = calendar.get(Calendar.YEAR)

                val incomeQuery = firestore.collection("incomes")
                    .whereEqualTo("userId", uid)
                val incomeSnapshots = incomeQuery.get().await()
                val totalIncome = incomeSnapshots.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                val expenseQuery = firestore.collection("expenses")
                    .whereEqualTo("userId", uid)
                val expenseSnapshots = expenseQuery.get().await()
                val totalExpenses = expenseSnapshots.documents.sumOf { it.getDouble("amount") ?: 0.0 }

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
                    remainingBudget < 0 -> "⚠️ Over budget by R${"%.2f".format(-remainingBudget)}"
                    remainingBudget == 0.0 -> "⚠️ Reached budget limit."
                    else -> "✅ R${"%.2f".format(remainingBudget)} remaining."
                }

                totalBalanceText.text = "R${"%.2f".format(totalIncome - totalExpenses)}"
                budgetInfoText.text = """
                    Budget Overview:
                    • Target: R${"%.2f".format(budgetTarget)}
                    • Expenses: R${"%.2f".format(totalExpenses)}
                    • $budgetStatus
                """.trimIndent()

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

        barChart.data = BarData(dataSet)
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

        pieChart.description = Description().apply { text = "" }
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
