package com.example.kasisave

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ExpensesActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var expensesRecyclerView: RecyclerView
    private lateinit var editTextExpenseAmount: EditText
    private lateinit var spinnerExpenseCategory: Spinner
    private lateinit var textViewExpenseDate: TextView
    private lateinit var switchRecurring: SwitchCompat
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var totalExpensesTextView: TextView

    private lateinit var expenseAdapter: ExpenseAdapter
    private val expensesList = mutableListOf<Expense>()

    private lateinit var db: ExpenseDatabase
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        // Initialize views
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)
        editTextExpenseAmount = findViewById(R.id.editTextExpenseAmount)
        spinnerExpenseCategory = findViewById(R.id.spinnerExpenseCategory)
        textViewExpenseDate = findViewById(R.id.textViewExpenseDate)
        switchRecurring = findViewById(R.id.switchRecurring)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)

        db = ExpenseDatabase.getDatabase(this)

        setupBottomNavigation()
        setupCategorySpinner()
        setupDatePicker()

        expenseAdapter = ExpenseAdapter(expensesList)

        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expenseAdapter

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

    private fun setupCategorySpinner() {
        val categories = arrayOf("Food", "Transport", "Bills", "Shopping", "Entertainment", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExpenseCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        textViewExpenseDate.text = "Date: $selectedDate"

        textViewExpenseDate.setOnClickListener {
            DatePickerDialog(this,
                { _, year, month, dayOfMonth ->
                    val pickedDate = Calendar.getInstance()
                    pickedDate.set(year, month, dayOfMonth)
                    selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pickedDate.time)
                    textViewExpenseDate.text = "Date: $selectedDate"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun addExpense() {
        val amountText = editTextExpenseAmount.text.toString().trim()

        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val category = spinnerExpenseCategory.selectedItem.toString()
        val isRecurring = switchRecurring.isChecked

        val expense = Expense(
            category = category,
            amount = amount,
            date = selectedDate,
            isRecurring = isRecurring
        )

        lifecycleScope.launch {
            db.expenseDao().insertExpense(expense)
            expensesList.add(expense)
            expenseAdapter.notifyItemInserted(expensesList.size - 1)
            updateTotalExpenses()
            editTextExpenseAmount.text.clear()
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val loadedExpenses = db.expenseDao().getAllExpenses()
            expensesList.addAll(loadedExpenses)
            expenseAdapter.notifyDataSetChanged()
            updateTotalExpenses()
        }
    }

    private fun deleteExpense(expense: Expense, position: Int) {
        lifecycleScope.launch {
            db.expenseDao().deleteExpense(expense)
            expensesList.removeAt(position)
            expenseAdapter.notifyItemRemoved(position)
            updateTotalExpenses()
        }
    }

    private fun updateTotalExpenses() {
        val total = expensesList.sumOf { it.amount }
        totalExpensesTextView.text = "Total Expenses: R %.2f".format(total)
    }
}

