package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class IncomeActivity : AppCompatActivity() {

    private lateinit var incomeSourceEditText: EditText
    private lateinit var incomeAmountEditText: EditText
    private lateinit var incomeCategorySpinner: Spinner
    private lateinit var recurringSwitch: SwitchMaterial
    private lateinit var addIncomeButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var incomeRecyclerView: RecyclerView
    private lateinit var totalIncomeTextView: TextView
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var db: ExpenseDatabase
    private lateinit var incomeAdapter: IncomeAdapter
    private val incomeList = mutableListOf<Income>()
    private var totalIncome = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_income)

            // 1) bind views
            incomeSourceEditText   = findViewById(R.id.incomeSourceEditText)
            incomeAmountEditText   = findViewById(R.id.incomeAmountEditText)
            incomeCategorySpinner  = findViewById(R.id.incomeCategorySpinner)
            recurringSwitch        = findViewById(R.id.recurringSwitch)
            addIncomeButton        = findViewById(R.id.addIncomeButton)
            incomeRecyclerView     = findViewById(R.id.incomeRecyclerView)
            totalIncomeTextView    = findViewById(R.id.totalIncomeTextView)
            bottomNavigationView   = findViewById(R.id.bottomNavigationView)

            // 2) setup category spinner
            val categories = listOf("Salary", "Freelance", "Gift", "Investment", "Other")
            ArrayAdapter(this, android.R.layout.simple_spinner_item, categories).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                incomeCategorySpinner.adapter = adapter
            }

            // 3) get database
            db = ExpenseDatabase.getDatabase(this)

            // 4) setup RecyclerView + adapter with delete callback
            incomeAdapter = IncomeAdapter(incomeList) { income, pos ->
                showDeleteConfirmationDialog(income, pos)
            }
            incomeRecyclerView.layoutManager = LinearLayoutManager(this)
            incomeRecyclerView.adapter = incomeAdapter

            // 5) load existing incomes
            loadIncomes()

            // 6) handle add button
            addIncomeButton.setOnClickListener { addIncome() }

            // 7) bottom navigation
            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_dashboard -> {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        overridePendingTransition(0, 0); finish(); true
                    }
                    R.id.navigation_expenses -> {
                        startActivity(Intent(this, ExpensesActivity::class.java))
                        overridePendingTransition(0, 0); finish(); true
                    }
                    R.id.navigation_income -> true
                    else -> false
                }
            }
            bottomNavigationView.selectedItemId = R.id.navigation_income

        } catch (e: Exception) {
            // Catch any exception during onCreate setup
            Log.e("IncomeActivity", "Crash during onCreate", e)
            Toast.makeText(this, "Error initializing Income screen:\n${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addIncome() {
        try {
            val source   = incomeSourceEditText.text.toString()
            val amountTxt= incomeAmountEditText.text.toString()
            val category = incomeCategorySpinner.selectedItem.toString()
            val isRec    = recurringSwitch.isChecked

            if (source.isBlank()) {
                incomeSourceEditText.error = "Enter source"; return
            }
            if (amountTxt.isBlank()) {
                incomeAmountEditText.error = "Enter amount"; return
            }
            val amount = amountTxt.toDoubleOrNull()
            if (amount == null) {
                incomeAmountEditText.error = "Invalid number"; return
            }

            val income = Income(source=source, amount=amount, category=category, isRecurring=isRec)
            lifecycleScope.launch {
                try {
                    db.incomeDao().insertIncome(income)
                    incomeList.add(0, income)
                    incomeAdapter.notifyItemInserted(0)
                    totalIncome += amount
                    updateTotalIncome()
                    incomeSourceEditText.text.clear()
                    incomeAmountEditText.text.clear()
                    recurringSwitch.isChecked = false
                    incomeCategorySpinner.setSelection(0)
                    Toast.makeText(this@IncomeActivity, "Income saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("IncomeActivity", "Insert failed", e)
                    Toast.makeText(this@IncomeActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("IncomeActivity", "Error in addIncome()", e)
            Toast.makeText(this, "Error adding income: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadIncomes() {
        lifecycleScope.launch {
            try {
                val incomes = db.incomeDao().getAllIncomes()
                incomeList.clear()
                incomeList.addAll(incomes)
                incomeAdapter.notifyDataSetChanged()
                totalIncome = incomes.sumOf { it.amount }
                updateTotalIncome()
                Log.d("IncomeActivity","Loaded ${incomes.size} incomes")
            } catch (e: Exception) {
                Log.e("IncomeActivity","Load failed", e)
                Toast.makeText(this@IncomeActivity, "Error loading incomes: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateTotalIncome() {
        totalIncomeTextView.text = "Total Income: R${"%.2f".format(totalIncome)}"
    }

    private fun showDeleteConfirmationDialog(income: Income, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Income")
            .setMessage("Delete ${income.source}?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.incomeDao().deleteIncome(income)
                        incomeList.removeAt(position)
                        incomeAdapter.notifyItemRemoved(position)
                        totalIncome -= income.amount
                        updateTotalIncome()
                    } catch (e: Exception) {
                        Log.e("IncomeActivity","Delete failed", e)
                        Toast.makeText(this@IncomeActivity, "Error deleting: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
