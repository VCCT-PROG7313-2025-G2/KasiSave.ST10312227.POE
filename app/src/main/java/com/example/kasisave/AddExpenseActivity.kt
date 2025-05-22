package com.example.kasisave.activities

import android.Manifest
import android.animation.Animator
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.kasisave.Expense
import com.example.kasisave.R
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var dateEditText: EditText
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var attachPhotoButton: Button
    private lateinit var saveButton: Button
    private lateinit var expenseImageView: ImageView
    private lateinit var recurringCheckBox: CheckBox
    private lateinit var expenseAnimationView: LottieAnimationView

    private var photoUri: Uri? = null
    private var photoFile: File? = null
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            expenseImageView.setImageURI(photoUri)
        } else {
            photoUri = null
            photoFile = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Bind Views
        dateEditText = findViewById(R.id.dateEditText)
        startTimeEditText = findViewById(R.id.startTimeEditText)
        endTimeEditText = findViewById(R.id.endTimeEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        amountEditText = findViewById(R.id.amountEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        attachPhotoButton = findViewById(R.id.attachPhotoButton)
        saveButton = findViewById(R.id.saveButton)
        expenseImageView = findViewById(R.id.expenseImageView)
        recurringCheckBox = findViewById(R.id.recurringCheckBox)
        expenseAnimationView = findViewById(R.id.expenseAnimationView)

        setupDatePicker()
        setupTimePickers()
        setupCategorySpinner()

        attachPhotoButton.setOnClickListener { checkCameraPermissionAndLaunch() }
        saveButton.setOnClickListener { saveExpense() }
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        dateEditText.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                dateEditText.setText(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupTimePickers() {
        startTimeEditText.setOnClickListener { showTimePickerDialog(startTimeEditText) }
        endTimeEditText.setOnClickListener { showTimePickerDialog(endTimeEditText) }
    }

    private fun showTimePickerDialog(target: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            target.setText(String.format("%02d:%02d", hour, minute))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.expense_categories)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        try {
            val photoDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            photoFile = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", photoDir)
            photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile!!)
            takePicture.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExpense() {
        val dateString = dateEditText.text.toString()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val parsedDate = try {
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }

        if (parsedDate == null) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }

        val dateMillis = parsedDate.time
        val startTime = startTimeEditText.text.toString().takeIf { it.isNotBlank() }
        val endTime = endTimeEditText.text.toString().takeIf { it.isNotBlank() }
        val description = descriptionEditText.text.toString().takeIf { it.isNotBlank() }
        val amountText = amountEditText.text.toString()
        val amount = amountText.toDoubleOrNull()
        val category = categorySpinner.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: ""
        val isRecurring = recurringCheckBox.isChecked
        val photoUriString = photoUri?.toString()

        if (amount == null) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = getSharedPreferences("kasisave_prefs", MODE_PRIVATE).getString("user_id", null)
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_LONG).show()
            return
        }

        val expense = Expense(
            userId = userId,
            dateMillis = dateMillis,
            startTime = startTime,
            endTime = endTime,
            description = description,
            amount = amount,
            category = category,
            photoUri = photoUriString,
            isRecurring = isRecurring
        )

        // Play animation first
        expenseAnimationView.setSpeed(0.75f)
        expenseAnimationView.visibility = View.VISIBLE
        expenseAnimationView.playAnimation()

        expenseAnimationView.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                // Proceed with saving to Firestore after animation
                FirebaseFirestore.getInstance()
                    .collection("expenses")
                    .add(expense)
                    .addOnSuccessListener {
                        Toast.makeText(this@AddExpenseActivity, "Expense saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this@AddExpenseActivity, "Error saving expense: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }

            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }
}
