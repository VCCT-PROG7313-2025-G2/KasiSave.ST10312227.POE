package com.example.kasisave.activities

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            expenseImageView.setImageURI(photoUri)
        } else {
            Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show()
            photoUri = null
            photoFile = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

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
            DatePickerDialog(
                this,
                { _, year, month, day -> dateEditText.setText("%04d-%02d-%02d".format(year, month + 1, day)) },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePickers() {
        startTimeEditText.setOnClickListener { showTimePickerDialog(startTimeEditText) }
        endTimeEditText.setOnClickListener { showTimePickerDialog(endTimeEditText) }
    }

    private fun showTimePickerDialog(target: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute -> target.setText("%02d:%02d".format(hour, minute)) },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun setupCategorySpinner() {
        val defaultCategories = resources.getStringArray(R.array.expense_categories).toMutableList()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("users").document(uid).collection("categories")
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        doc.getString("name")?.let { name ->
                            if (!defaultCategories.contains(name)) defaultCategories.add(name)
                        }
                    }
                    populateSpinner(defaultCategories)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load custom categories", Toast.LENGTH_SHORT).show()
                    populateSpinner(defaultCategories)
                }
        } else {
            populateSpinner(defaultCategories)
        }
    }

    private fun populateSpinner(categories: List<String>) {
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
            Log.e("AddExpense", "launchCamera error: ", e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExpense() {
        val dateMillis = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateEditText.text.toString())!!.time
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountEditText.text.toString().toDoubleOrNull() ?: run {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val category = categorySpinner.selectedItem?.toString()?.takeIf { it.isNotBlank() } ?: run {
            Toast.makeText(this, "Select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        expenseAnimationView.speed = 1.0f
        expenseAnimationView.visibility = View.VISIBLE
        expenseAnimationView.playAnimation()
        saveButton.isEnabled = false

        if (photoFile != null && photoFile!!.exists()) {
            uploadImageToFirebase(userId, dateMillis, amount, category)
        } else {
            saveExpenseToFirestore(userId, dateMillis, amount, category, null)
        }
    }

    private fun uploadImageToFirebase(userId: String, dateMillis: Long, amount: Double, category: String) {
        val storageRef = storage.reference.child("expense_images/$userId/${photoFile!!.name}")
        val uploadTask = storageRef.putFile(Uri.fromFile(photoFile!!))

        uploadTask
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveExpenseToFirestore(userId, dateMillis, amount, category, uri.toString())
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                    expenseAnimationView.visibility = View.GONE
                    saveButton.isEnabled = true
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                expenseAnimationView.visibility = View.GONE
                saveButton.isEnabled = true
            }
    }

    private fun saveExpenseToFirestore(
        userId: String,
        dateMillis: Long,
        amount: Double,
        category: String,
        downloadUrl: String?
    ) {
        val expense = Expense(
            userId = userId,
            dateMillis = dateMillis,
            startTime = startTimeEditText.text.toString().takeIf { it.isNotBlank() },
            endTime = endTimeEditText.text.toString().takeIf { it.isNotBlank() },
            description = descriptionEditText.text.toString().takeIf { it.isNotBlank() },
            amount = amount,
            category = category,
            photoUri = downloadUrl,
            isRecurring = recurringCheckBox.isChecked
        )

        firestore.collection("expenses")
            .add(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show()
                expenseAnimationView.visibility = View.GONE
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving expense: ${it.message}", Toast.LENGTH_LONG).show()
                expenseAnimationView.visibility = View.GONE
                saveButton.isEnabled = true
            }
    }
}
