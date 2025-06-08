package com.example.kasisave

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var budgetInfoText: TextView
    private lateinit var actualUsageText: TextView
    private lateinit var remainingBudgetText: TextView
    private lateinit var goalAlertText: TextView
    private lateinit var spendingGoalStatus: TextView
    private lateinit var pieChartCard: MaterialCardView
    private lateinit var barChartCard: MaterialCardView
    private lateinit var lineChartCard: MaterialCardView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Chart data variables
    private var totalIncome: Double = 0.0
    private var totalExpenses: Double = 0.0
    private var expensesByCategory: Map<String, Float> = emptyMap()
    private var minSpendingGoal: Double = 0.0
    private var maxSpendingGoal: Double = 0.0

    // Period selection properties
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    private val firestoreDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

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

        // Initialize UI components
        budgetInfoText = findViewById(R.id.budgetInfoText)
        actualUsageText = findViewById(R.id.actualUsageText)
        remainingBudgetText = findViewById(R.id.remainingBudgetText)
        goalAlertText = findViewById(R.id.goalAlertText)
        spendingGoalStatus = findViewById(R.id.spendingGoalStatus)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Initialize chart cards
        pieChartCard = findViewById(R.id.pieChartCard)
        barChartCard = findViewById(R.id.barChartCard)
        lineChartCard = findViewById(R.id.lineChartCard)

        // Redirect to login if user not authenticated
        if (currentUserUid == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize dates
        selectedStartDate = getStartOfCurrentMonth()
        selectedEndDate = Date()

        updateNavigationHeader()
        setupBottomNavigation()
        setupChartCardListeners()
        loadDashboardData()
    }

    private fun setupChartCardListeners() {
        pieChartCard.setOnClickListener { showFullscreenChart(ChartType.PIE) }
        barChartCard.setOnClickListener { showFullscreenChart(ChartType.BAR) }
        lineChartCard.setOnClickListener { showFullscreenChart(ChartType.LINE) }
    }

    private fun showFullscreenChart(chartType: ChartType) {
        // Create full-screen dialog
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.chart_dialog_fullscreen)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        val titleText = dialog.findViewById<TextView>(R.id.chartTitle)
        val pieChart = dialog.findViewById<PieChart>(R.id.dialogPieChart)
        val barChart = dialog.findViewById<BarChart>(R.id.dialogBarChart)
        val combinedChart = dialog.findViewById<CombinedChart>(R.id.dialogCombinedChart)
        val closeButton = dialog.findViewById<MaterialButton>(R.id.btnClose)
        val periodLayout = dialog.findViewById<LinearLayout>(R.id.periodLayout)
        val periodSpinner = dialog.findViewById<Spinner>(R.id.periodSpinner)
        val customPeriodLayout = dialog.findViewById<LinearLayout>(R.id.customPeriodLayout)
        val startDateBtn = dialog.findViewById<Button>(R.id.startDateBtn)
        val endDateBtn = dialog.findViewById<Button>(R.id.endDateBtn)
        val applyPeriodBtn = dialog.findViewById<Button>(R.id.applyPeriodBtn)

        // Hide all charts initially
        pieChart.visibility = View.GONE
        barChart.visibility = View.GONE
        combinedChart.visibility = View.GONE
        periodLayout.visibility = View.GONE
        customPeriodLayout.visibility = View.GONE

        when (chartType) {
            ChartType.PIE -> {
                titleText.text = "Expenses by Category"
                pieChart.visibility = View.VISIBLE
                setupPieChart(pieChart)
            }
            ChartType.BAR -> {
                titleText.text = "Income vs Expenses"
                barChart.visibility = View.VISIBLE
                setupBarChart(barChart)
            }
            ChartType.LINE -> {
                titleText.text = "Spending by Category with Goals"
                combinedChart.visibility = View.VISIBLE
                periodLayout.visibility = View.VISIBLE

                // Setup period spinner
                val periods = arrayOf("Current Month", "Last 3 Months", "Last 6 Months", "Custom")
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, periods)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                periodSpinner.adapter = adapter
                periodSpinner.setSelection(0) // Default to current month

                // Update button texts with formatted dates
                startDateBtn.text = displayDateFormat.format(selectedStartDate)
                endDateBtn.text = displayDateFormat.format(selectedEndDate)

                periodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        when (position) {
                            0 -> { // Current Month
                                customPeriodLayout.visibility = View.GONE
                                selectedStartDate = getStartOfCurrentMonth()
                                selectedEndDate = Date()
                                setupCategoryBarChartWithGoals(combinedChart)
                            }
                            1 -> { // Last 3 Months
                                customPeriodLayout.visibility = View.GONE
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.MONTH, -2)
                                cal.set(Calendar.DAY_OF_MONTH, 1)
                                selectedStartDate = cal.time
                                selectedEndDate = Date()
                                setupCategoryBarChartWithGoals(combinedChart)
                            }
                            2 -> { // Last 6 Months
                                customPeriodLayout.visibility = View.GONE
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.MONTH, -5)
                                cal.set(Calendar.DAY_OF_MONTH, 1)
                                selectedStartDate = cal.time
                                selectedEndDate = Date()
                                setupCategoryBarChartWithGoals(combinedChart)
                            }
                            3 -> { // Custom
                                customPeriodLayout.visibility = View.VISIBLE
                                // Update button texts with formatted dates
                                startDateBtn.text = displayDateFormat.format(selectedStartDate)
                                endDateBtn.text = displayDateFormat.format(selectedEndDate)
                            }
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

                // Date pickers
                startDateBtn.setOnClickListener {
                    showDatePickerDialog(startDateBtn, true)
                }

                endDateBtn.setOnClickListener {
                    showDatePickerDialog(endDateBtn, false)
                }

                applyPeriodBtn.setOnClickListener {
                    if (selectedStartDate != null && selectedEndDate != null) {
                        if (selectedStartDate!!.after(selectedEndDate)) {
                            Toast.makeText(this, "Start date must be before end date", Toast.LENGTH_SHORT).show()
                        } else {
                            setupCategoryBarChartWithGoals(combinedChart)
                        }
                    }
                }

                // Load initial data
                setupCategoryBarChartWithGoals(combinedChart)
            }
        }

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDatePickerDialog(button: Button, isStartDate: Boolean) {
        val cal = Calendar.getInstance()
        val date = if (isStartDate) selectedStartDate else selectedEndDate
        if (date != null) cal.time = date

        val picker = DatePickerDialog(this, { _, year, month, day ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(year, month, day)

            if (isStartDate) {
                selectedStartDate = selectedCal.time
            } else {
                selectedEndDate = selectedCal.time
            }

            button.text = displayDateFormat.format(selectedCal.time)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

        picker.show()
    }

    private fun getStartOfCurrentMonth(): Date {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun setupPieChart(chart: PieChart) {
        if (expensesByCategory.isEmpty()) {
            chart.visibility = View.GONE
            return
        }

        // Create pie chart for expenses by category
        val totalExpensesValue = expensesByCategory.values.sum()
        val pieEntries = expensesByCategory.entries.map {
            val percentage = (it.value / totalExpensesValue) * 100
            PieEntry(it.value, it.key)
        }

        val pieDataSet = PieDataSet(pieEntries, "")
        pieDataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()

        pieDataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val percentage = (value / totalExpensesValue) * 100
                return "%.1f%%".format(percentage)
            }
        }

        pieDataSet.valueTextSize = 14f
        pieDataSet.valueTextColor = Color.BLACK
        pieDataSet.valueLinePart1Length = 0.5f
        pieDataSet.valueLinePart2Length = 0.4f
        pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

        val pieData = PieData(pieDataSet)
        chart.data = pieData

        chart.description = Description().apply {
            text = ""
            textSize = 16f
        }
        chart.setUsePercentValues(false)
        chart.setExtraOffsets(20f, 20f, 20f, 20f)
        chart.setEntryLabelTextSize(14f)
        chart.setEntryLabelColor(Color.DKGRAY)
        chart.legend.isEnabled = false
        chart.setDrawEntryLabels(true)
        chart.setDrawCenterText(true)
        chart.centerText = "Expenses\nby Category"
        chart.setCenterTextSize(16f)
        chart.setCenterTextColor(Color.DKGRAY)
        chart.setHoleRadius(30f)
        chart.setTransparentCircleRadius(35f)
        chart.setTransparentCircleAlpha(100)
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun setupBarChart(chart: BarChart) {
        // Create bar chart for income vs expenses
        val barEntries = listOf(
            BarEntry(0f, totalIncome.toFloat(), "Income"),
            BarEntry(1f, totalExpenses.toFloat(), "Expenses")
        )

        val barDataSet = BarDataSet(barEntries, "Income vs Expenses")
        barDataSet.colors = intArrayOf(
            ColorTemplate.rgb("#4CAF50"),  // Green for income
            ColorTemplate.rgb("#F44336")    // Red for expenses
        ).toList()
        barDataSet.valueTextSize = 16f

        chart.data = BarData(barDataSet)

        // Chart configuration
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Income", "Expenses"))
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.textSize = 14f
        chart.xAxis.granularity = 1f

        chart.axisRight.isEnabled = false
        chart.axisLeft.textSize = 14f
        chart.legend.textSize = 16f

        chart.description = Description().apply {
            text = ""
            textSize = 16f
        }

        chart.setExtraOffsets(20f, 20f, 20f, 20f)
        chart.setFitBars(true)
        chart.animateY(1000)
        chart.invalidate()
    }

    private fun setupCategoryBarChartWithGoals(chart: CombinedChart) {
        lifecycleScope.launch {
            try {
                val uid = currentUserUid ?: return@launch

                // Show loading indicator
                val loading = ProgressDialog.show(
                    this@DashboardActivity,
                    "Loading Data",
                    "Please wait while we load your spending data",
                    true
                )

                Log.d("ChartDebug", "Loading expenses from ${selectedStartDate} to ${selectedEndDate}")

                // Fetch all expenses for the user
                val expensesSnap = firestore.collection("expenses")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                // Process expenses - filter by date range
                val categoryMap = mutableMapOf<String, Float>()
                var expenseCount = 0

                for (doc in expensesSnap.documents) {
                    val dateStr = doc.getString("date") ?: ""

                    try {
                        val expenseDate = firestoreDateFormat.parse(dateStr)

                        if (expenseDate != null &&
                            expenseDate >= selectedStartDate!! &&
                            expenseDate <= selectedEndDate!!) {

                            val cat = doc.getString("category") ?: "Other"
                            val amt = (doc.getDouble("amount") ?: 0.0).toFloat()
                            categoryMap[cat] = categoryMap.getOrDefault(cat, 0f) + amt
                            expenseCount++
                        }
                    } catch (e: ParseException) {
                        Log.e("DateError", "Failed to parse date: $dateStr", e)
                    }
                }

                // Dismiss loading
                loading.dismiss()

                Log.d("ChartDebug", "Found $expenseCount expenses in date range")

                if (categoryMap.isEmpty()) {
                    Toast.makeText(
                        this@DashboardActivity,
                        "No expenses found for selected period",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Prepare category data
                val categories = categoryMap.keys.toList()
                val expenses = categoryMap.values.toList()

                // Create bar entries
                val barEntries = mutableListOf<BarEntry>()
                categories.forEachIndexed { index, _ ->
                    barEntries.add(BarEntry(index.toFloat(), expenses[index]))
                }

                val barDataSet = BarDataSet(barEntries, "Amount Spent by Category").apply {
                    color = Color.parseColor("#FFA726") // Orange
                    valueTextSize = 12f
                    valueTextColor = Color.BLACK
                }

                val barData = BarData(barDataSet)

                // Create dummy line data (required for CombinedChart)
                val lineDataSet = LineDataSet(mutableListOf<Entry>(), "").apply {
                    setDrawValues(false)
                    setDrawCircles(false)
                    setDrawIcons(false)
                    color = Color.TRANSPARENT
                }
                val lineData = LineData(lineDataSet)

                val combinedData = CombinedData()
                combinedData.setData(barData)
                combinedData.setData(lineData)

                chart.data = combinedData

                // Configure chart
                with(chart) {
                    description = Description().apply {
                        text = "Spending by Category with Goals"
                        textSize = 14f
                    }

                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        valueFormatter = IndexAxisValueFormatter(categories)
                        granularity = 1f
                        setDrawGridLines(false)
                        textSize = 12f
                        labelRotationAngle = -45f
                        setCenterAxisLabels(false)
                        axisMinimum = -0.5f
                        axisMaximum = categories.size.toFloat() - 0.5f
                    }

                    axisLeft.apply {
                        textSize = 12f
                        axisMinimum = 0f
                        granularity = 100f

                        // Calculate max value for Y-axis
                        val maxExpense = expenses.maxOrNull() ?: 0f
                        val maxGoal = maxSpendingGoal.toFloat().coerceAtLeast(minSpendingGoal.toFloat())
                        val maxValue = maxExpense.coerceAtLeast(maxGoal) * 1.2f  // Add 20% padding
                        axisMaximum = maxValue

                        // Add min/max goal lines
                        if (minSpendingGoal > 0) {
                            val minLimit = LimitLine(minSpendingGoal.toFloat(), "Min Goal")
                            minLimit.lineColor = Color.GREEN
                            minLimit.lineWidth = 2f
                            minLimit.textColor = Color.BLACK
                            minLimit.textSize = 12f
                            minLimit.enableDashedLine(10f, 10f, 0f)  // Make dashed line
                            addLimitLine(minLimit)
                        }

                        if (maxSpendingGoal > 0) {
                            val maxLimit = LimitLine(maxSpendingGoal.toFloat(), "Max Goal")
                            maxLimit.lineColor = Color.RED
                            maxLimit.lineWidth = 2f
                            maxLimit.textColor = Color.BLACK
                            maxLimit.textSize = 12f
                            maxLimit.enableDashedLine(10f, 10f, 0f)  // Make dashed line
                            addLimitLine(maxLimit)
                        }
                    }

                    axisRight.isEnabled = false

                    legend.apply {
                        isEnabled = true
                        textSize = 14f
                        formSize = 12f
                    }

                    setTouchEnabled(true)
                    setPinchZoom(true)
                    setExtraOffsets(20f, 30f, 20f, 20f)  // More top padding for labels

                    // Important settings for combined chart
                    setDrawBarShadow(false)
                    setDrawValueAboveBar(true)
                    drawOrder = arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE)

                    animateY(1000)
                    invalidate()
                }

            } catch (e: Exception) {
                Log.e("ChartError", "Failed to load chart data", e)
                Toast.makeText(
                    this@DashboardActivity,
                    "Error loading spending data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
            R.id.nav_rewards -> startActivity(Intent(this, RewardsActivity::class.java))
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
                R.id.navigation_dashboard -> null // already here
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
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)

                // Fetch incomes
                val incomes = firestore.collection("incomes")
                    .whereEqualTo("userId", uid)
                    .get().await()
                totalIncome = incomes.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                // Fetch expenses
                val expensesSnap = firestore.collection("expenses")
                    .whereEqualTo("userId", uid)
                    .get().await()
                totalExpenses = expensesSnap.documents.sumOf { it.getDouble("amount") ?: 0.0 }

                // Store expenses by category
                val categoryMap = mutableMapOf<String, Float>()
                for (doc in expensesSnap.documents) {
                    val cat = doc.getString("category") ?: "Other"
                    val amt = (doc.getDouble("amount") ?: 0.0).toFloat()
                    categoryMap[cat] = categoryMap.getOrDefault(cat, 0f) + amt
                }
                expensesByCategory = categoryMap

                // Fetch milestones
                val milestoneDocs = firestore.collection("milestones")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                // Find current month milestone
                val milestoneCurrentMonth = milestoneDocs.documents.firstOrNull { doc ->
                    val deadlineStr = doc.getString("deadline") ?: return@firstOrNull false
                    val parts = deadlineStr.split("/")
                    if (parts.size != 3) return@firstOrNull false
                    val month = parts[1].toIntOrNull() ?: return@firstOrNull false
                    val year = parts[2].toIntOrNull() ?: return@firstOrNull false
                    month == currentMonth && year == currentYear
                }

                minSpendingGoal = milestoneCurrentMonth?.getDouble("minMonthlySpend") ?: 0.0
                maxSpendingGoal = milestoneCurrentMonth?.getDouble("maxMonthlySpend") ?: 0.0

                // Calculate balance
                val balance = totalIncome - totalExpenses
                actualUsageText.text = "Actual usage: R%.2f".format(totalExpenses)
                remainingBudgetText.text = "Remaining: R%.2f".format(balance)
                budgetInfoText.text = "Income: R%.2f\nExpenses: R%.2f".format(totalIncome, totalExpenses)

                goalAlertText.text = when {
                    maxSpendingGoal > 0 && totalExpenses > maxSpendingGoal -> "⚠️ Over max limit!"
                    minSpendingGoal > 0 && totalExpenses < minSpendingGoal -> "ℹ️ Below minimum goal"
                    else -> "✅ On track"
                }

            } catch (e: Exception) {
                e.printStackTrace()
                budgetInfoText.text = ""
                actualUsageText.text = ""
                remainingBudgetText.text = ""
                goalAlertText.text = ""
                spendingGoalStatus.text = "Error loading data"
            }
        }
    }

    // Enum to represent chart types
    private enum class ChartType {
        PIE, BAR, LINE
    }
}