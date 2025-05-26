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

class DashboardActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {

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

        ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        ).also {
            drawerLayout.addDrawerListener(it)
            it.syncState()
        }

        navigationView.setNavigationItemSelectedListener(this)

        updateNavigationHeader()

        // Views
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

    private fun updateNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val profileImage = headerView.findViewById<ImageView>(R.id.imageViewProfile)
        val usernameText = headerView.findViewById<TextView>(R.id.textViewUsername)
        val emailText = headerView.findViewById<TextView>(R.id.textViewEmail)

        val user = FirebaseAuth.getInstance().currentUser
        val firestore = FirebaseFirestore.getInstance()

        if (user != null) {
            val userId = user.uid

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: "user@example.com"
                        val avatarName = document.getString("avatar") ?: "ic_profile"

                        usernameText.text = "$firstName $lastName"
                        emailText.text = email

                        val resId = resources.getIdentifier(avatarName, "drawable", packageName)
                        if (resId != 0) {
                            profileImage.setImageResource(resId)
                        } else {
                            profileImage.setImageResource(R.drawable.ic_profile)
                        }
                    }
                }
                .addOnFailureListener {
                    usernameText.text = "User"
                    emailText.text = user.email ?: "user@example.com"
                    profileImage.setImageResource(R.drawable.ic_profile)
                }
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
                R.id.navigation_dashboard -> null
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
                val cal = Calendar.getInstance()
                val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val year = cal.get(Calendar.YEAR)

                val incomes = firestore.collection("incomes")
                    .whereEqualTo("userId", uid).get().await()
                val totalIncome = incomes.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                val expensesSnap = firestore.collection("expenses")
                    .whereEqualTo("userId", uid).get().await()
                val totalExpenses = expensesSnap.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                val milestoneSnap = firestore.collection("milestones")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("month", month)
                    .whereEqualTo("year", year)
                    .limit(1).get().await()
                val milestone = milestoneSnap.documents.firstOrNull()
                val target = milestone?.getDouble("targetAmount") ?: totalIncome

                val remaining = target - totalExpenses
                val status = when {
                    remaining < 0 -> "⚠️ Over budget by R${"%.2f".format(-remaining)}"
                    remaining == 0.0 -> "⚠️ Reached budget limit."
                    else -> "✅ R${"%.2f".format(remaining)} remaining."
                }

                totalBalanceText.text = "R${"%.2f".format(totalIncome - totalExpenses)}"
                budgetInfoText.text = """
                    Budget Overview:
                    • Target: R${"%.2f".format(target)}
                    • Expenses: R${"%.2f".format(totalExpenses)}
                    • $status
                """.trimIndent()

                setupCategoryPieChart(
                    expensesSnap.documents
                        .groupBy { it.getString("category") ?: "Other" }
                        .map { it.key to it.value.sumOf { d -> d.getDouble("amount") ?: 0.0 } }
                )

                setupIncomeVsExpenseBarChart(totalIncome, totalExpenses)

            } catch (e: Exception) {
                e.printStackTrace()
                totalBalanceText.text = "Error loading data"
                budgetInfoText.text = "Could not load budget overview."
            }
        }
    }

    private fun setupIncomeVsExpenseBarChart(income: Double, expenses: Double) {
        val entries = listOf(BarEntry(0f, income.toFloat()), BarEntry(1f, expenses.toFloat()))
        val labels = listOf("Income", "Expenses")
        val ds = BarDataSet(entries, "Income vs Expenses").apply {
            colors = listOf(
                resources.getColor(R.color.teal_700, null),
                resources.getColor(R.color.purple_500, null)
            )
            valueTextSize = 14f
        }
        barChart.apply {
            data = BarData(ds)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = true
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }

    private fun setupCategoryPieChart(categoryTotals: List<Pair<String, Double>>) {
        val entries = categoryTotals.map { PieEntry(it.second.toFloat(), it.first) }
        val ds = PieDataSet(entries, "Expense Breakdown").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 14f
            valueTextColor = resources.getColor(android.R.color.white, null)
        }
        pieChart.apply {
            data = PieData(ds)
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            holeRadius = 40f
            setTransparentCircleRadius(45f)
            setEntryLabelColor(resources.getColor(android.R.color.white, null))
            setEntryLabelTextSize(12f)
            setDrawEntryLabels(true)
            description = Description().apply { text = "" }
            legend.isEnabled = true
            animateY(1000)
            invalidate()
        }
    }
}
