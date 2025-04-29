package com.example.kasisave

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.bottomnavigation.BottomNavigationView

class IncomeActivity : AppCompatActivity() {

    private lateinit var amountEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var categorySpinner: Spinner
    private lateinit var recurringCheckBox: CheckBox
    private lateinit var addIncomeButton: FloatingActionButton
    private lateinit var incomeRecyclerView: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var db: ExpenseDatabase
    private lateinit var adapter: IncomeAdapter
    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        db = ExpenseDatabase.getDatabase(this)

        // View initialization
        amountEditText = findViewById(R.id.amountEditText)
        dateButton = findViewById(R.id.dateButton)
        selectedDateText = findViewById(R.id.selectedDateText)
        categorySpinner = findViewById(R.id.categorySpinner)
        recurringCheckBox = findViewById(R.id.recurringCheckBox)
        addIncomeButton = findViewById(R.id.addIncomeButton)
        incomeRecyclerView = findViewById(R.id.incomeRecyclerView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        setupCategorySpinner()
        setupDatePicker()
        setupRecyclerView()
        setupAddIncomeButton()
        setupBottomNavigation()
        loadIncomeList()

        // Highlight the current nav item
        bottomNavigationView.selectedItemId = R.id.navigation_income
    }

    private fun setupCategorySpinner() {
        val categories = listOf("Salary", "Gift", "Freelance", "Bonus", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun setupDatePicker() {
        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selected = Calendar.getInstance()
                    selected.set(year, month, dayOfMonth)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    selectedDate = sdf.format(selected.time)
                    selectedDateText.text = selectedDate
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun setupRecyclerView() {
        adapter = IncomeAdapter()
        incomeRecyclerView.layoutManager = LinearLayoutManager(this)
        incomeRecyclerView.adapter = adapter
    }

    private fun setupAddIncomeButton() {
        addIncomeButton.setOnClickListener {
            val amountText = amountEditText.text.toString()
            val category = categorySpinner.selectedItem.toString()
            val isRecurring = recurringCheckBox.isChecked

            if (amountText.isEmpty() || selectedDate.isEmpty()) {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val income = Income(
                amount = amount,
                date = selectedDate,
                category = category,
                isRecurring = isRecurring
            )

            lifecycleScope.launch {
                try {
                    db.incomeDao().insertIncome(income)
                    Toast.makeText(this@IncomeActivity, "Income added", Toast.LENGTH_SHORT).show()
                    amountEditText.text.clear()
                    selectedDateText.text = ""
                    selectedDate = ""
                    loadIncomeList()
                } catch (e: Exception) {
                    Toast.makeText(this@IncomeActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadIncomeList() {
        lifecycleScope.launch {
            try {
                val incomeList = db.incomeDao().getAllIncomes()
                adapter.submitList(incomeList)
            } catch (e: Exception) {
                Toast.makeText(this@IncomeActivity, "Failed to load income: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java)) // Replace with your actual dashboard activity
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
                    // Already in IncomeActivity, no need to do anything
                    true
                }
                else -> false
            }
        }
    }
}
