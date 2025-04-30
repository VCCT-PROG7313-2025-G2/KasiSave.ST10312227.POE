package com.example.kasisave

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
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
    private lateinit var buttonCapturePhoto: Button
    private lateinit var imageViewPhotoPreview: ImageView

    private lateinit var expenseAdapter: ExpenseAdapter
    private val expensesList = mutableListOf<Expense>()

    private lateinit var db: ExpenseDatabase
    private var selectedDate: String = ""
    private var startTime: String = ""
    private var endTime: String = ""
    private var photoUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            imageViewPhotoPreview.setImageURI(photoUri)
            imageViewPhotoPreview.visibility = ImageView.VISIBLE
        } else {
            photoUri = null
            Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show()
            imageViewPhotoPreview.visibility = ImageView.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

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
        buttonCapturePhoto = findViewById(R.id.buttonTakePhoto)
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview)

        db = ExpenseDatabase.getDatabase(this)

        setupBottomNavigation()
        setupCategorySpinner()
        setupDatePicker()
        setupTimePickers()

        buttonCapturePhoto.setOnClickListener {
            launchCamera()
        }

        expenseAdapter = ExpenseAdapter(expensesList)
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        expensesRecyclerView.adapter = expenseAdapter

        loadExpenses()

        addExpenseButton.setOnClickListener {
            addExpense()
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile() ?: return
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(photoUri)
    }

    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            File.createTempFile(fileName, ".jpg", storageDir)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
            photoUri = photoUri?.toString()
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
        photoUri = null
        imageViewPhotoPreview.setImageDrawable(null)
        imageViewPhotoPreview.visibility = ImageView.GONE
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
}
