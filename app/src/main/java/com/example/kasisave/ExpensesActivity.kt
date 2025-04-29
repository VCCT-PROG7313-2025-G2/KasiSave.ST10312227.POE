package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class ExpensesActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var editTextExpenseName: EditText
    private lateinit var editTextExpenseAmount: EditText
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var totalExpensesTextView: TextView

    private lateinit var expensesAdapter: ExpensesAdapter
    private val expensesList = mutableListOf<Expense>()

    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        editTextExpenseName = findViewById(R.id.editTextExpenseName)
        editTextExpenseAmount = findViewById(R.id.editTextExpenseAmount)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)

        db = ExpenseDatabase.getDatabase(this)

        setupBottomNavigation()

        expensesAdapter = ExpensesAdapter(expensesList) { expense, position ->
            deleteExpense(expense, position)
        }

        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expensesAdapter

        // Load expenses from database
        loadExpenses()

        addExpenseButton.setOnClickListener {
            addExpense()
        }
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
                R.id.navigation_expenses -> true
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.navigation_expenses
    }

    private fun addExpense() {
        val name = editTextExpenseName.text.toString().trim()
        val amountText = editTextExpenseAmount.text.toString().trim()

        if (name.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(this, "Please enter both name and amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = Expense(name = name, amount = amount)

        // Save to Room database
        lifecycleScope.launch {
            db.expenseDao().insertExpense(expense)
            expensesList.add(expense)
            expensesAdapter.notifyItemInserted(expensesList.size - 1)
            updateTotalExpenses()

            // Clear input fields
            editTextExpenseName.text.clear()
            editTextExpenseAmount.text.clear()
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val loadedExpenses = db.expenseDao().getAllExpenses()
            expensesList.addAll(loadedExpenses)
            expensesAdapter.notifyDataSetChanged()
            updateTotalExpenses()
        }
    }



    private fun deleteExpense(expense: Expense, position: Int) {
        lifecycleScope.launch {
            db.expenseDao().deleteExpense(expense)
            expensesList.removeAt(position)
            expensesAdapter.notifyItemRemoved(position)
            updateTotalExpenses()
        }
    }

    private fun updateTotalExpenses() {
        val total = expensesList.sumOf { it.amount }
        totalExpensesTextView.text = "Total: R %.2f".format(total)
    }
}
