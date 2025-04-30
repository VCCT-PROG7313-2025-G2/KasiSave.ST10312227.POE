package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.kasisave.model.Goal
import kotlinx.coroutines.launch

class MilestonesActivity : AppCompatActivity() {

    private lateinit var editTextMinGoal: EditText
    private lateinit var editTextMaxGoal: EditText
    private lateinit var buttonSaveGoals: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_milestones)

        editTextMinGoal = findViewById(R.id.editTextMinGoal)
        editTextMaxGoal = findViewById(R.id.editTextMaxGoal)
        buttonSaveGoals = findViewById(R.id.buttonSaveGoals)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        db = ExpenseDatabase.getDatabase(this)

        // Load existing goal data (for the current month and year)
        loadExistingGoal()

        buttonSaveGoals.setOnClickListener {
            val minGoal = editTextMinGoal.text.toString().toFloatOrNull() ?: 0f
            val maxGoal = editTextMaxGoal.text.toString().toFloatOrNull() ?: 0f

            // Get current month and year
            val currentMonth = getCurrentMonth()
            val currentYear = getCurrentYear()

            // Save goals in the database
            saveGoals(currentMonth, currentYear, minGoal, maxGoal)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_expenses -> {
                    startActivity(Intent(this, ExpensesActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_milestones -> true
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.navigation_milestones
    }

    private fun getCurrentMonth(): String {
        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
        return currentMonth.toString()
    }

    private fun getCurrentYear(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }

    private fun loadExistingGoal() {
        lifecycleScope.launch {
            // Fetch the existing goal for the current month and year
            val currentMonth = getCurrentMonth()
            val currentYear = getCurrentYear()

            val existingGoal = db.goalDao().getGoalForMonth(currentMonth, currentYear)
            if (existingGoal != null) {
                editTextMinGoal.setText(existingGoal.minGoal.toString())
                editTextMaxGoal.setText(existingGoal.maxAmount.toString())
            }
        }
    }

    private fun saveGoals(month: String, year: Int, minGoal: Float, maxGoal: Float) {
        lifecycleScope.launch {
            // Create a new Goal object
            val goal = Goal(
                month = month,
                year = year,
                minGoal = minGoal.toDouble(),
                maxAmount = maxGoal,
                minAmount = minGoal // Assuming minAmount is same as minGoal
            )

            // Insert or update the goal in the database
            db.goalDao().insertGoal(goal)

            // Show a toast message
            runOnUiThread {
                Toast.makeText(this@MilestonesActivity, "Spending goals saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

