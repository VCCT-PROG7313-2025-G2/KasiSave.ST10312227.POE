package com.example.kasisave

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
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
    private lateinit var editTextDescription: EditText
    private lateinit var textViewStartTime: TextView
    private lateinit var textViewEndTime: TextView
    private lateinit var imageViewPhoto: ImageView
    private lateinit var buttonAttachPhoto: Button

    private lateinit var expenseAdapter: ExpenseAdapter
    private val expensesList = mutableListOf<Expense>()

    private lateinit var db: ExpenseDatabase
    private var selectedDate: String = ""
    private var startTime: String = ""
    private var endTime: String = ""
    private var photoUri: String? = null
    private var photoFile: File? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
    }


    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoUri = Uri.fromFile(photoFile).toString()
            imageViewPhoto.setImageURI(Uri.fromFile(photoFile))
        }
    }

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
        editTextDescription = findViewById(R.id.editTextDescription)
        textViewStartTime = findViewById(R.id.textViewStartTime)
        textViewEndTime = findViewById(R.id.textViewEndTime)
        imageViewPhoto = findViewById(R.id.imageViewReceipt)
        buttonAttachPhoto = findViewById(R.id.buttonTakePhoto)

        db = ExpenseDatabase.getDatabase(this)

        setupBottomNavigation()
        setupCategorySpinner()
        setupDatePicker()
        setupTimePickers()

        expenseAdapter = ExpenseAdapter(expensesList)
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expenseAdapter

        loadExpenses()

        buttonAttachPhoto.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        }


        addExpenseButton.setOnClickListener {
            addExpense()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            // Create the temporary file in external cache
            photoFile = File.createTempFile("expense_photo_", ".jpg", externalCacheDir)

            // Get URI using the correct authority (must match manifest)
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "com.example.kasisave.fileprovider",
                photoFile!!
            )

            photoUri = photoURI.toString() // Optional: store as string
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
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

    private fun setupTimePickers() {
        textViewStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                startTime = String.format("%02d:%02d", hour, minute)
                textViewStartTime.text = "Start: $startTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        textViewEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                endTime = String.format("%02d:%02d", hour, minute)
                textViewEndTime.text = "End: $endTime"
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }
    }

    private fun addExpense() {
        val amountText = editTextExpenseAmount.text.toString().trim()
        val description = editTextDescription.text.toString().trim()

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
            isRecurring = isRecurring,
            description = description,
            startTime = startTime,
            endTime = endTime,
            photoUri = photoUri
        )

        lifecycleScope.launch {
            db.expenseDao().insertExpense(expense)
            expensesList.add(expense)
            expenseAdapter.notifyItemInserted(expensesList.size - 1)
            updateTotalExpenses()
            resetFields()
        }
    }

    private fun resetFields() {
        editTextExpenseAmount.text.clear()
        editTextDescription.text.clear()
        textViewStartTime.text = "Start time"
        textViewEndTime.text = "End time"
        startTime = ""
        endTime = ""
        val calendar = Calendar.getInstance()
        selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        textViewExpenseDate.text = "Date: $selectedDate"
        imageViewPhoto.setImageResource(R.drawable.placeholder_icon)
        photoUri = null
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val loadedExpenses = db.expenseDao().getAllExpenses()
            expensesList.addAll(loadedExpenses)
            expenseAdapter.notifyDataSetChanged()
            updateTotalExpenses()
        }
    }

    private fun updateTotalExpenses() {
        val total = expensesList.sumOf { it.amount }
        totalExpensesTextView.text = "Total: R %.2f".format(total)
    }

    private fun deleteExpense(expense: Expense, position: Int) {
        lifecycleScope.launch {
            db.expenseDao().deleteExpense(expense)
            expensesList.removeAt(position)
            expenseAdapter.notifyItemRemoved(position)
            updateTotalExpenses()
        }
    }
}

