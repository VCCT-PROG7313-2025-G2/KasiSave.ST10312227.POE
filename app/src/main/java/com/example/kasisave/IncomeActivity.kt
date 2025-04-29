package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class IncomeActivity : AppCompatActivity() {

    private lateinit var incomeRecyclerView: RecyclerView
    private lateinit var addIncomeButton: FloatingActionButton
    private lateinit var incomeSourceEditText: EditText
    private lateinit var incomeAmountEditText: EditText
    private lateinit var totalIncomeTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var incomeAdapter: IncomeAdapter
    private val incomeList = mutableListOf<Income>()

    private var totalIncome = 0.0

    private lateinit var db: ExpenseDatabase  // Reference to your database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        incomeRecyclerView = findViewById(R.id.incomeRecyclerView)
        addIncomeButton = findViewById(R.id.addIncomeButton)
        incomeSourceEditText = findViewById(R.id.incomeSourceEditText)
        incomeAmountEditText = findViewById(R.id.incomeAmountEditText)
        totalIncomeTextView = findViewById(R.id.totalIncomeTextView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        db = ExpenseDatabase.getDatabase(this)

        incomeAdapter = IncomeAdapter(incomeList) { income, position ->
            showDeleteConfirmationDialog(income, position)
        }

        incomeRecyclerView.adapter = incomeAdapter
        incomeRecyclerView.layoutManager = LinearLayoutManager(this)

        addIncomeButton.setOnClickListener {
            addIncome()
        }

        setupBottomNavigation()

        loadIncomes()
    }

    private fun addIncome() {
        val source = incomeSourceEditText.text.toString()
        val amountText = incomeAmountEditText.text.toString()

        if (source.isNotBlank() && amountText.isNotBlank()) {
            val amount = amountText.toDoubleOrNull()
            if (amount != null) {
                val income = Income(source = source, amount = amount)

                lifecycleScope.launch {
                    db.incomeDao().insertIncome(income)
                    incomeList.add(income)
                    incomeAdapter.notifyItemInserted(incomeList.size - 1)

                    totalIncome += amount
                    updateTotalIncome()

                    incomeSourceEditText.text.clear()
                    incomeAmountEditText.text.clear()
                }
            } else {
                incomeAmountEditText.error = "Please enter a valid amount"
            }
        } else {
            if (source.isBlank()) incomeSourceEditText.error = "Enter source"
            if (amountText.isBlank()) incomeAmountEditText.error = "Enter amount"
        }
    }

    private fun loadIncomes() {
        lifecycleScope.launch {
            val incomes = db.incomeDao().getAllIncomes()
            incomeList.clear()
            incomeList.addAll(incomes)
            incomeAdapter.notifyDataSetChanged()

            totalIncome = incomes.sumOf { it.amount }
            updateTotalIncome()
        }
    }

    private fun updateTotalIncome() {
        totalIncomeTextView.text = "Total Income: R${"%.2f".format(totalIncome)}"
    }

    private fun showDeleteConfirmationDialog(income: Income, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Income")
            .setMessage("Are you sure you want to delete ${income.source}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.incomeDao().deleteIncome(income)
                    totalIncome -= income.amount
                    incomeList.removeAt(position)
                    incomeAdapter.notifyItemRemoved(position)
                    updateTotalIncome()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupBottomNavigation() {
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
                R.id.navigation_income -> {
                    true
                }
                else -> false
            }
        }
    }
}
