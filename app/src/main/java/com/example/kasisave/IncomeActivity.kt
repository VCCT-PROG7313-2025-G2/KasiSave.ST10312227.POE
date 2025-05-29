package com.example.kasisave

import android.animation.Animator
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var micButton: ImageButton

    private lateinit var adapter: IncomeAdapter
    private var selectedDate: String = ""

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    private val categories = listOf("Salary", "Gift", "Freelance", "Bonus", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_LONG).show()
            finish()
            return
        }

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
        micButton = findViewById(R.id.micButton)

        setupCategorySpinner()
        setupDatePicker()
        setupRecyclerView()
        setupAddIncomeButton()
        setupMicButton()
        setupBottomNavigation()
        loadIncomeList()

        bottomNavigationView.selectedItemId = R.id.navigation_income
    }

    private fun setupCategorySpinner() {
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

    private fun setupMicButton() {
        micButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the income amount and category")
            }
            try {
                speechRecognizerLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            spokenText?.let {
                parseVoiceInput(it)
            }
        }
    }

    private fun parseVoiceInput(input: String) {
        val lowerInput = input.lowercase(Locale.getDefault())

        val digitMatch = Regex("""(?:r\s*)?([\d,]+(?:\.\d+)?)""").find(lowerInput)
        val amountStr = digitMatch?.groupValues?.get(1)?.replace(",", "")
        val amount = amountStr?.toDoubleOrNull() ?: wordsToNumber(lowerInput) // fallback to words


        if (amount != null) {
            amountEditText.setText(amount.toString())
        } else {
            Toast.makeText(this, "Could not detect amount. Please try again.", Toast.LENGTH_SHORT).show()
        }

        // Detect category from spoken words
        val matchedCategory = categories.find { lowerInput.contains(it.lowercase()) }
        matchedCategory?.let {
            val position = categories.indexOf(it)
            if (position >= 0) {
                categorySpinner.setSelection(position)
            }
        }
    }

    private fun wordsToNumber(input: String): Double? {
        val numberWords = mapOf(
            "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
            "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
            "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14,
            "fifteen" to 15, "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
            "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
        )

        var total = 0
        var current = 0
        var lastMultiplier = 1

        val words = input.lowercase(Locale.getDefault()).replace("-", " ").split(" ")

        for (word in words) {
            when {
                numberWords.containsKey(word) -> {
                    current += numberWords[word]!!
                }
                word == "hundred" -> {
                    if (current == 0) current = 1
                    current *= 100
                }
                word == "thousand" -> {
                    if (current == 0) current = 1
                    total += current * 1000
                    current = 0
                }
                word == "and" -> {
                    // skip 'and'
                }
                else -> {
                    // Unknown word, ignore or break
                }
            }
        }

        total += current
        return if (total > 0) total.toDouble() else null
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
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        putExtra("userId", userId)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java).apply {
                        putExtra("userId", userId)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_expenses -> {
                    startActivity(Intent(this, ExpensesActivity::class.java).apply {
                        putExtra("userId", userId)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_income -> true
                R.id.navigation_milestones -> {
                    startActivity(Intent(this, MilestonesActivity::class.java).apply {
                        putExtra("userId", userId)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
