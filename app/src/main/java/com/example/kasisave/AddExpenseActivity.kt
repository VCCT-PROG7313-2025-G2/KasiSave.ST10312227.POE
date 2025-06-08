package com.example.kasisave

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
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
    private lateinit var recurringCheckBox: CheckBox
    private lateinit var attachPhotoButton: Button
    private lateinit var saveButton: Button
    private lateinit var expenseImageView: ImageView
    private lateinit var voiceInputDescriptionButton: ImageButton
    private lateinit var voiceInputAmountButton: ImageButton
    private lateinit var recognizeTextButton: ImageButton
    private lateinit var expenseAnimationView: LottieAnimationView

    private var photoUri: Uri? = null
    private var photoFile: File? = null

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val AUDIO_PERMISSION_REQUEST_CODE = 1002
    private val SPEECH_DESCRIPTION_REQUEST_CODE = 2000
    private val SPEECH_AMOUNT_REQUEST_CODE = 2001

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            expenseImageView.setImageURI(photoUri)
            recognizeTextButton.isEnabled = true
        } else {
            Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show()
            photoUri = null
            photoFile = null
            recognizeTextButton.isEnabled = false
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
        recurringCheckBox = findViewById(R.id.recurringCheckBox)
        attachPhotoButton = findViewById(R.id.attachPhotoButton)
        saveButton = findViewById(R.id.saveButton)
        expenseImageView = findViewById(R.id.expenseImageView)
        voiceInputDescriptionButton = findViewById(R.id.btnMicDescription)
        voiceInputAmountButton = findViewById(R.id.btnMicAmount)
        recognizeTextButton = findViewById(R.id.btnRecognizeText)
        expenseAnimationView = findViewById(R.id.expenseAnimationView)

        recognizeTextButton.isEnabled = false

        setupDatePicker()
        setupTimePickers()
        setupCategorySpinner()

        attachPhotoButton.setOnClickListener { checkCameraPermissionAndLaunch() }
        saveButton.setOnClickListener { saveExpense() }
        recognizeTextButton.setOnClickListener {
            photoUri?.let { uri -> recognizeTextFromPhoto(uri) }
                ?: Toast.makeText(this, "No image to analyze", Toast.LENGTH_SHORT).show()
        }
        voiceInputDescriptionButton.setOnClickListener {
            checkAudioPermissionAndStartVoiceInput(SPEECH_DESCRIPTION_REQUEST_CODE)
        }
        voiceInputAmountButton.setOnClickListener {
            checkAudioPermissionAndStartVoiceInput(SPEECH_AMOUNT_REQUEST_CODE)
        }
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
            { _, hour, minute -> target.setText(String.format("%02d:%02d", hour, minute)) },
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

    private fun checkAudioPermissionAndStartVoiceInput(requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST_CODE)
        } else {
            startVoiceInput(requestCode)
        }
    }

    private fun startVoiceInput(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (requestCode == SPEECH_AMOUNT_REQUEST_CODE) "Speak the amount" else "Speak the description"
            )
        }

        try {
            startActivityForResult(intent, requestCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                when (requestCode) {
                    SPEECH_DESCRIPTION_REQUEST_CODE -> descriptionEditText.setText(result[0])
                    SPEECH_AMOUNT_REQUEST_CODE -> {
                        val spokenText = result[0]
                        val amount = spokenText.replace("[^0-9.]".toRegex(), "").toDoubleOrNull()
                        if (amount != null) {
                            amountEditText.setText(String.format("%.2f", amount))
                        } else {
                            Toast.makeText(this, "Could not recognize amount", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    launchCamera()
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                }
            }
            AUDIO_PERMISSION_REQUEST_CODE -> {
                Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show()
            }
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
        val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)

        // Compress the image to JPEG format with 50% quality
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 20, baos)
        val compressedData = baos.toByteArray()

        val storageRef = storage.reference.child("expense_images/$userId/${photoFile!!.name}")

        val uploadTask = storageRef.putBytes(compressedData)
        uploadTask
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveExpenseToFirestore(userId, dateMillis, amount, category, uri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                expenseAnimationView.visibility = View.GONE
                saveButton.isEnabled = true
            }
    }


    private fun saveExpenseToFirestore(userId: String, dateMillis: Long, amount: Double, category: String, downloadUrl: String?) {
        val expense = Expense(
            userId = userId,
            dateMillis = dateMillis,
            startTime = startTimeEditText.text.toString(),
            endTime = endTimeEditText.text.toString(),
            description = descriptionEditText.text.toString(),
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
                val intent = Intent(this, RewardsExpensesActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving expense: ${it.message}", Toast.LENGTH_LONG).show()
                expenseAnimationView.visibility = View.GONE
                saveButton.isEnabled = true
            }
    }

    private fun recognizeTextFromPhoto(imageUri: Uri) {
        val image: InputImage
        try {
            image = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedText = visionText.text

                    // Description (use first non-empty line)
                    val lines = detectedText.lines().filter { it.isNotBlank() }
                    if (lines.isNotEmpty()) {
                        descriptionEditText.setText(lines[0])
                    }

                    // Amount detection with keyword context
                    val keywords = listOf("total", "amount", "sum", "balance")
                    val amountRegex = Regex("""\d{1,3}(,\d{3})*(\.\d{2})|\d+\.\d{2}""")

                    var detectedAmount: Double? = null

                    // Search lines for keywords and amounts
                    for (line in detectedText.lines()) {
                        val lowerLine = line.lowercase()
                        if (keywords.any { it in lowerLine }) {
                            val match = amountRegex.find(line)
                            if (match != null) {
                                detectedAmount = match.value.toDouble()
                                break
                            }
                        }
                    }

                    // Fallback: If no amount near keywords found, take the highest amount in whole text
                    if (detectedAmount == null) {
                        val allMatches = amountRegex.findAll(detectedText).map { it.value.toDouble() }
                        detectedAmount = allMatches.maxOrNull()
                    }

                    detectedAmount?.let {
                        amountEditText.setText(String.format("%.2f", it))
                    }

                    // Date detection
                    val dateRegexes = listOf(
                        Regex("""\b\d{4}[-/]\d{2}[-/]\d{2}\b"""), // yyyy-MM-dd or yyyy/MM/dd
                        Regex("""\b\d{2}[-/]\d{2}[-/]\d{4}\b"""), // dd-MM-yyyy or dd/MM/yyyy
                        Regex("""\b\d{2}[.]\d{2}[.]\d{2,4}\b"""), // dd.MM.yy or dd.MM.yyyy
                        Regex("""\b\d{1,2} (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \d{4}\b""", RegexOption.IGNORE_CASE),
                        Regex("""\b\d{1,2} (January|February|March|April|May|June|July|August|September|October|November|December) \d{4}\b""", RegexOption.IGNORE_CASE),
                        Regex("""\b\d{2}/\d{2}/\d{2}\b""") // added dd/MM/yy
                    )

                    var detectedDate: String? = null
                    for (regex in dateRegexes) {
                        val match = regex.find(detectedText)
                        if (match != null) {
                            detectedDate = match.value
                            break
                        }
                    }

                    if (detectedDate != null) {
                        val normalized = normalizeDate(detectedDate)
                        if (normalized != null) {
                            dateEditText.setText(normalized)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to recognize text: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizeDate(input: String): String? {
        val formats = listOf(
            "dd/MM/yy", "dd-MM-yy",
            "yyyy-MM-dd", "yyyy/MM/dd",
            "dd-MM-yyyy", "dd/MM/yyyy",
            "dd.MM.yyyy", "dd.MM.yy",
            "d MMM yyyy", "d MMMM yyyy"
        )

        for (format in formats) {
            try {
                val parser = SimpleDateFormat(format, Locale.ENGLISH)
                parser.isLenient = false
                val parsedDate = parser.parse(input) ?: continue

                val calendar = Calendar.getInstance()
                calendar.time = parsedDate

                // Fix 2-digit years to correct century
                if (format.contains("yy") && !format.contains("yyyy")) {
                    val year = calendar.get(Calendar.YEAR)
                    if (year < 100) {
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val century = currentYear / 100
                        val adjustedYear = if (year < 50) century * 100 + year else (century - 1) * 100 + year
                        calendar.set(Calendar.YEAR, adjustedYear)
                    }
                }

                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                return formatter.format(calendar.time)
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

}
