package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
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
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var totalBalanceText: TextView
    private lateinit var budgetInfoText: TextView
    private lateinit var actualUsageText: TextView
    private lateinit var remainingBudgetText: TextView
    private lateinit var goalAlertText: TextView
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var bottomNavigationView: BottomNavigationView
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

        // Setup toolbar and navigation drawer
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)

        // Update navigation drawer header with user info
        updateNavigationHeader()

        // Initialize UI components
        totalBalanceText = findViewById(R.id.totalBalanceAmount)
        budgetInfoText = findViewById(R.id.budgetInfoText)
        actualUsageText = findViewById(R.id.actualUsageText)
        remainingBudgetText = findViewById(R.id.remainingBudgetText)
        goalAlertText = findViewById(R.id.goalAlertText)
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        lineChart = findViewById(R.id.lineChart)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Redirect to login if user not authenticated
        if (currentUserUid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupBottomNavigation()
        loadDashboardData()
    }

    private fun updateNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val profileImage = headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val usernameText = headerView.findViewById<TextView>(R.id.textViewUsername)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)
        val user = auth.currentUser ?: return

        firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val firstName = doc.getString("firstName") ?: ""
                    val lastName = doc.getString("lastName") ?: ""
                    val email = doc.getString("email") ?: user.email
                    val avatarName = doc.getString("avatar") ?: "ic_profile"
                    usernameText.text = "$firstName $lastName"
                    emailText.text = email
                    val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                    profileImage.setImageResource(if (resId != 0) resId else R.drawable.ic_profile)
                }
            }
            .addOnFailureListener {
                usernameText.text = "User"
                emailText.text = user.email ?: "user@example.com"
                profileImage.setImageResource(R.drawable.ic_profile)
            }
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

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.navigation_dashboard -> null // current screen
                R.id.navigation_expenses -> Intent(this, ExpensesActivity::class.java)
                R.id.navigation_income -> Intent(this, IncomeActivity::class.java)
                R.id.navigation_milestones -> Intent(this, MilestonesActivity::class.java)
                R.id.navigation_categories -> Intent(this, CategoriesActivity::class.java)
                else -> null
            }
            intent?.let {
                startActivity(it)
                overridePendingTransition(0, 0)
                finish()
            }
            true
        }
        bottomNavigationView.selectedItemId = R.id.navigation_dashboard
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val uid = currentUserUid ?: return@launch

                // Get current date info
                val cal = Calendar.getInstance()
                val currentMonth = cal.get(Calendar.MONTH) + 1 // 1-based month
                val currentYear = cal.get(Calendar.YEAR)

                // Fetch incomes for the user
                val incomes = firestore.collection("incomes")
                    .whereEqualTo("userId", uid)
                    .get().await()
                val totalIncome = incomes.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                // Fetch expenses for the user
                val expensesSnap = firestore.collection("expenses")
                    .whereEqualTo("userId", uid)
                    .get().await()
                val totalExpenses = expensesSnap.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                // Fetch milestones for the user (all)
                val milestoneDocs = firestore.collection("milestones")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                // Find current month milestone (to show min/max goals and alert)
                val milestoneCurrentMonth = milestoneDocs.documents.firstOrNull { doc ->
                    val deadlineStr = doc.getString("deadline") ?: return@firstOrNull false
                    val parts = deadlineStr.split("/")
                    if (parts.size != 3) return@firstOrNull false
                    val month = parts[1].toIntOrNull() ?: return@firstOrNull false
                    val year = parts[2].toIntOrNull() ?: return@firstOrNull false
                    month == currentMonth && year == currentYear
                }

                val minGoalCurrent = milestoneCurrentMonth?.getDouble("minMonthlySpend") ?: 0.0
                val maxGoalCurrent = milestoneCurrentMonth?.getDouble("maxMonthlySpend") ?: 0.0

                // Calculate balance and update UI
                val balance = totalIncome - totalExpenses
                totalBalanceText.text = "R%.2f".format(balance)
                actualUsageText.text = "Actual usage: R%.2f".format(totalExpenses)
                remainingBudgetText.text = "Remaining: R%.2f".format(balance)
                budgetInfoText.text = "Income: R%.2f\nExpenses: R%.2f".format(totalIncome, totalExpenses)

                goalAlertText.text = when {
                    maxGoalCurrent > 0 && totalExpenses > maxGoalCurrent -> "⚠️ Over max limit!"
                    minGoalCurrent > 0 && totalExpenses < minGoalCurrent -> "ℹ️ Below minimum goal"
                    else -> "✅ On track"
                }

                // Setup pie chart: Income vs Expenses
                val pieEntries = listOf(
                    PieEntry(totalIncome.toFloat(), "Income"),
                    PieEntry(totalExpenses.toFloat(), "Expenses")
                )
                val pieDataSet = PieDataSet(pieEntries, "Budget")
                pieDataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                pieChart.data = PieData(pieDataSet)
                pieChart.description = Description().apply { text = "" }
                pieChart.centerText = "Balance"
                pieChart.setUsePercentValues(false)
                pieChart.invalidate()

                // Setup bar chart for expenses by category
                val categoryMap = mutableMapOf<String, Float>()
                for (doc in expensesSnap.documents) {
                    val cat = doc.getString("category") ?: "Other"
                    val amt = (doc.getDouble("amount") ?: 0.0).toFloat()
                    categoryMap[cat] = categoryMap.getOrDefault(cat, 0f) + amt
                }
                val barEntries = categoryMap.entries.toList().mapIndexed { index, entry ->
                    BarEntry(index.toFloat(), entry.value)
                }
                val labels = categoryMap.keys.toList()
                val barDataSet = BarDataSet(barEntries, "Expenses by Category")
                barDataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
                barChart.data = BarData(barDataSet)
                barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                barChart.xAxis.setDrawGridLines(false)
                barChart.axisRight.isEnabled = false
                barChart.description = Description().apply { text = "" }
                barChart.invalidate()

                // --- New Line Chart for milestones (min/max monthly spend) over past 6 months ---
                // We'll gather milestones for last 6 months including current month,
                // and display min and max goals as two line series.

                // Prepare data map keyed by "MM/yyyy"
                val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                val calLine = Calendar.getInstance()
                calLine.set(Calendar.DAY_OF_MONTH, 1) // normalize date to 1st of month

                val monthLabels = mutableListOf<String>()
                val minEntries = mutableListOf<Entry>()
                val maxEntries = mutableListOf<Entry>()

                // Gather milestones for last 6 months (5 past + current)
                for (i in 5 downTo 0) {
                    calLine.add(Calendar.MONTH, -i)
                    val key = sdf.format(calLine.time) // MM/yyyy
                    monthLabels.add(key)
                    calLine.add(Calendar.MONTH, i) // revert back
                }

                // Sort milestones by deadline (month/year)
                val milestonesByMonth = milestoneDocs.documents.groupBy { doc ->
                    val deadlineStr = doc.getString("deadline") ?: ""
                    // Convert "dd/MM/yyyy" to "MM/yyyy"
                    if (deadlineStr.length >= 10) {
                        deadlineStr.substring(3, 10) // from index 3, length 7 => "MM/yyyy"
                    } else {
                        ""
                    }
                }

                // For each month label, find min/max goal or 0
                monthLabels.forEachIndexed { index, monthYear ->
                    val docsForMonth = milestonesByMonth[monthYear]
                    val minGoal = docsForMonth?.mapNotNull { it.getDouble("minMonthlySpend") }?.maxOrNull() ?: 0.0
                    val maxGoal = docsForMonth?.mapNotNull { it.getDouble("maxMonthlySpend") }?.maxOrNull() ?: 0.0

                    minEntries.add(Entry(index.toFloat(), minGoal.toFloat()))
                    maxEntries.add(Entry(index.toFloat(), maxGoal.toFloat()))
                }

                val minDataSet = LineDataSet(minEntries, "Min Monthly Spend")
                minDataSet.color = ColorTemplate.COLORFUL_COLORS[0]
                minDataSet.circleRadius = 4f
                minDataSet.setCircleColor(ColorTemplate.COLORFUL_COLORS[0])
                minDataSet.lineWidth = 2f

                val maxDataSet = LineDataSet(maxEntries, "Max Monthly Spend")
                maxDataSet.color = ColorTemplate.COLORFUL_COLORS[1]
                maxDataSet.circleRadius = 4f
                maxDataSet.setCircleColor(ColorTemplate.COLORFUL_COLORS[1])
                maxDataSet.lineWidth = 2f

                val lineData = LineData(minDataSet, maxDataSet)
                lineChart.data = lineData

                // X axis setup
                val xAxis = lineChart.xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = IndexAxisValueFormatter(monthLabels)

                // Y axis setup
                lineChart.axisRight.isEnabled = false
                lineChart.description = Description().apply { text = "Milestones over months" }
                lineChart.invalidate()

            } catch (e: Exception) {
                e.printStackTrace()
                totalBalanceText.text = "Error loading data"
                budgetInfoText.text = ""
                actualUsageText.text = ""
                remainingBudgetText.text = ""
                goalAlertText.text = ""
            }
        }
    }
}
