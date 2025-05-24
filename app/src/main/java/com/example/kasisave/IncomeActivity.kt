package com.example.kasisave

import android.animation.Animator
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class IncomeActivity : AppCompatActivity() {

    private lateinit var amountEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var selectedDateText: TextView
    private lateinit var categorySpinner: Spinner
    private lateinit var recurringCheckBox: CheckBox
    private lateinit var addIncomeButton: FloatingActionButton
    private lateinit var incomeRecyclerView: RecyclerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var totalIncomeText: TextView
    private lateinit var incomeAnimationView: LottieAnimationView

    private lateinit var adapter: IncomeAdapter
    private var selectedDate: String = ""

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Bind views
        amountEditText = findViewById(R.id.amountEditText)
        dateButton = findViewById(R.id.dateButton)
        selectedDateText = findViewById(R.id.selectedDateText)
        categorySpinner = findViewById(R.id.categorySpinner)
        recurringCheckBox = findViewById(R.id.recurringCheckBox)
        addIncomeButton = findViewById(R.id.addIncomeButton)
        incomeRecyclerView = findViewById(R.id.incomeRecyclerView)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        totalIncomeText = findViewById(R.id.totalIncomeTextView)
        incomeAnimationView = findViewById(R.id.incomeAnimationView)

        setupCategorySpinner()
        setupDatePicker()
        setupRecyclerView()
        setupAddIncomeButton()
        setupBottomNavigation()
        loadIncomeList()

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
            val amountText = amountEditText.text.toString().trim()
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

            val income = hashMapOf(
                "amount" to amount,
                "date" to selectedDate,
                "category" to category,
                "isRecurring" to isRecurring,
                "userId" to userId
            )

            firestore.collection("incomes")
                .add(income)
                .addOnSuccessListener {
                    Toast.makeText(this, "Income added", Toast.LENGTH_SHORT).show()
                    amountEditText.text.clear()
                    recurringCheckBox.isChecked = false
                    selectedDateText.text = "No date selected"
                    selectedDate = ""
                    loadIncomeList()

                    // Play the animation
                    incomeAnimationView.visibility = View.VISIBLE
                    incomeAnimationView.playAnimation()
                    incomeAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            incomeAnimationView.visibility = View.GONE
                        }

                        override fun onAnimationCancel(animation: Animator) {}

                        override fun onAnimationRepeat(animation: Animator) {}
                    })

                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadIncomeList() {
        firestore.collection("incomes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val incomeList = result.map { doc ->
                    Income(
                        id = doc.id,
                        amount = doc.getDouble("amount") ?: 0.0,
                        date = doc.getString("date") ?: "",
                        category = doc.getString("category") ?: "",
                        isRecurring = doc.getBoolean("isRecurring") ?: false,
                        userId = userId
                    )
                }

                adapter.submitList(incomeList)

                val totalIncome = incomeList.sumOf { it.amount }
                totalIncomeText.text = "Total Income: R%.2f".format(totalIncome)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load income: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.navigation_dashboard -> null // stay here
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
        // Ensure Dashboard is selected by default
        bottomNavigationView.selectedItemId = R.id.navigation_dashboard
    }
}
