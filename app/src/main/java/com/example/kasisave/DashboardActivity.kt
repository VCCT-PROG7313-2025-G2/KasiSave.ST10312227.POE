package com.example.kasisave

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var expenseProgressBar: ProgressBar
    private lateinit var milestoneProgressBar: ProgressBar

    private lateinit var budgetPercentText: TextView
    private lateinit var expensePercentText: TextView
    private lateinit var milestonePercentText: TextView
    private lateinit var totalBalanceText: TextView

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        expenseProgressBar = findViewById(R.id.expenseProgressBar)
        milestoneProgressBar = findViewById(R.id.milestoneProgressBar)

        budgetPercentText = findViewById(R.id.budgetPercentText)
        expensePercentText = findViewById(R.id.expensePercentText)
        milestonePercentText = findViewById(R.id.milestonePercentText)

        totalBalanceText = findViewById(R.id.totalBalanceAmount)

        // Example dynamic values
        val totalBalance = 25000.0
        val budgetGoal = 20000.0
        val currentBudget = 15000.0

        val expenseLimit = 15000.0
        val currentExpense = 5000.0

        val milestoneTarget = 500.0
        val currentMilestone = 300.0

        totalBalanceText.text = "R$totalBalance"

        // Calculate percentages
        val budgetPercent = ((currentBudget / budgetGoal) * 100).toInt()
        val expensePercent = ((currentExpense / expenseLimit) * 100).toInt()
        val milestonePercent = ((currentMilestone / milestoneTarget) * 100).toInt()

        animateProgress(budgetProgressBar, budgetPercent, budgetPercentText)
        animateProgress(expenseProgressBar, expensePercent, expensePercentText)
        animateProgress(milestoneProgressBar, milestonePercent, milestonePercentText)
    }

    private fun animateProgress(progressBar: ProgressBar, targetProgress: Int, percentText: TextView) {
        Thread {
            var progress = 0
            while (progress <= targetProgress) {
                val current = progress
                handler.post {
                    progressBar.progress = current
                    percentText.text = "$current%"
                }
                progress++
                Thread.sleep(15) // control speed of animation
            }
        }.start()
    }
}
