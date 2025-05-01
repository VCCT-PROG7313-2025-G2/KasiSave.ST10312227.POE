package com.example.kasisave

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchExpenseByDateActivity : AppCompatActivity() {

    private lateinit var btnStartDate: Button
    private lateinit var btnEndDate: Button
    private lateinit var btnFilter: Button
    private lateinit var txtTotalFiltered: TextView
    private lateinit var recyclerFilteredExpenses: RecyclerView

    private var startDate: Long? = null
    private var endDate: Long? = null

    private lateinit var expenseDatabase: ExpenseDatabase
    private lateinit var adapter: ExpenseAdapter
    private val expensesList = mutableListOf<Expense>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_expense_by_date)

        // Initialize views
        btnStartDate = findViewById(R.id.btnStartDate)
        btnEndDate = findViewById(R.id.btnEndDate)
        btnFilter = findViewById(R.id.btnFilter)
        txtTotalFiltered = findViewById(R.id.txtTotalFiltered)
        recyclerFilteredExpenses = findViewById(R.id.recyclerFilteredExpenses)

        // Setup RecyclerView
        adapter = ExpenseAdapter(expensesList)
        recyclerFilteredExpenses.layoutManager = LinearLayoutManager(this)
        recyclerFilteredExpenses.adapter = adapter

        // Initialize database
        expenseDatabase = ExpenseDatabase.getDatabase(this)

        // Set date picker listeners
        btnStartDate.setOnClickListener {
            showDatePicker { millis ->
                startDate = millis
                btnStartDate.text = "Start: ${dateFormat.format(Date(millis))}"
            }
        }

        btnEndDate.setOnClickListener {
            showDatePicker { millis ->
                endDate = millis
                btnEndDate.text = "End: ${dateFormat.format(Date(millis))}"
            }
        }

        // Filter button
        btnFilter.setOnClickListener {
            if (startDate != null && endDate != null) {
                filterExpensesByDate()
            } else {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                onDateSelected(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun filterExpensesByDate() {
        lifecycleScope.launch {
            // Fetch expenses between the selected start and end dates
            val filteredExpenses = expenseDatabase.expenseDao().getExpensesBetween(startDate!!, endDate!!)

            expensesList.clear()
            expensesList.addAll(filteredExpenses)
            adapter.notifyDataSetChanged()

            // Calculate the total amount of the filtered expenses
            val totalAmount = filteredExpenses.sumOf { it.amount }
            txtTotalFiltered.text = "Total: R %.2f".format(totalAmount)

            // Show a message if no expenses were found
            if (filteredExpenses.isEmpty()) {
                Toast.makeText(this@SearchExpenseByDateActivity, "No expenses found in this date range.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
