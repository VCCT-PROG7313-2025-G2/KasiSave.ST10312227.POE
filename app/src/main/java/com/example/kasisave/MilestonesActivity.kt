package com.example.kasisave

import android.app.*
import android.content.*
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.data.MilestonesAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class MilestonesActivity : AppCompatActivity() {

    private lateinit var milestoneRecyclerView: RecyclerView
    private lateinit var editTextGoalName: EditText
    private lateinit var editTextTargetAmount: EditText
    private lateinit var editTextDeadline: EditText
    private lateinit var editTextMinSpend: EditText
    private lateinit var editTextMaxSpend: EditText
    private lateinit var buttonAddMilestone: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var micGoalName: ImageButton
    private lateinit var micTargetAmount: ImageButton
    private lateinit var micMinSpend: ImageButton
    private lateinit var micMaxSpend: ImageButton

    private lateinit var milestoneAdapter: MilestonesAdapter
    private val milestoneList = mutableListOf<Milestone>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var milestonesListener: ListenerRegistration? = null
    private var userId: String? = null

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    companion object {
        private const val SPEECH_REQUEST_CODE_GOAL_NAME = 1001
        private const val SPEECH_REQUEST_CODE_TARGET_AMOUNT = 1002
        private const val SPEECH_REQUEST_CODE_MIN_SPEND = 1003
        private const val SPEECH_REQUEST_CODE_MAX_SPEND = 1004
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_milestones)

        userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadMilestones()

        editTextDeadline.setOnClickListener { showDatePickerDialog() }
        editTextDeadline.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showDatePickerDialog()
        }

        buttonAddMilestone.setOnClickListener { addMilestone() }

        micGoalName.setOnClickListener { startSpeechToText(SPEECH_REQUEST_CODE_GOAL_NAME) }
        micTargetAmount.setOnClickListener { startSpeechToText(SPEECH_REQUEST_CODE_TARGET_AMOUNT) }
        micMinSpend.setOnClickListener { startSpeechToText(SPEECH_REQUEST_CODE_MIN_SPEND) }
        micMaxSpend.setOnClickListener { startSpeechToText(SPEECH_REQUEST_CODE_MAX_SPEND) }
    }

    private fun initViews() {
        milestoneRecyclerView = findViewById(R.id.milestoneRecyclerView)
        editTextGoalName = findViewById(R.id.editTextGoalName)
        editTextTargetAmount = findViewById(R.id.editTextTargetAmount)
        editTextDeadline = findViewById(R.id.editTextDeadline)
        editTextMinSpend = findViewById(R.id.minSpendText)
        editTextMaxSpend = findViewById(R.id.maxSpendText)
        buttonAddMilestone = findViewById(R.id.buttonAddMilestone)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        micGoalName = findViewById(R.id.micGoalName)
        micTargetAmount = findViewById(R.id.micTargetAmount)
        micMinSpend = findViewById(R.id.micMinSpend)
        micMaxSpend = findViewById(R.id.micMaxSpend)
    }

    private fun setupRecyclerView() {
        milestoneAdapter = MilestonesAdapter(milestoneList)
        milestoneRecyclerView.layoutManager = LinearLayoutManager(this)
        milestoneRecyclerView.adapter = milestoneAdapter
    }

    private fun loadMilestones() {
        milestonesListener?.remove()
        milestonesListener = db.collection("milestones")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading milestones: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                milestoneList.clear()
                snapshot?.documents?.forEach { doc ->
                    val milestone = doc.toObject(Milestone::class.java)
                    if (milestone != null) {
                        milestoneList.add(milestone.copy(id = doc.id))
                        scheduleMilestoneNotification(milestone)
                    }
                }
                milestoneAdapter.notifyDataSetChanged()
            }
    }

    private fun scheduleMilestoneNotification(milestone: Milestone) {
        try {
            val deadlineDate = dateFormat.parse(milestone.deadline)
            val now = Calendar.getInstance()
            val deadlineCal = Calendar.getInstance().apply { time = deadlineDate!! }

            if (!deadlineCal.before(now)) return

            val intent = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("milestone_name", milestone.name)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                milestone.name.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1000, // Trigger 1 second later
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addMilestone() {
        val goalName = editTextGoalName.text.toString().trim()
        val deadline = editTextDeadline.text.toString().trim()

        val targetAmountStr = editTextTargetAmount.text.toString().trim().replace(",", "")
        val minSpendStr = editTextMinSpend.text.toString().trim().replace(",", "")
        val maxSpendStr = editTextMaxSpend.text.toString().trim().replace(",", "")

        val targetAmount = targetAmountStr.toDoubleOrNull()
        val minSpend = minSpendStr.toDoubleOrNull()
        val maxSpend = maxSpendStr.toDoubleOrNull()

        if (goalName.isEmpty() || targetAmountStr.isEmpty() || deadline.isEmpty() ||
            minSpendStr.isEmpty() || maxSpendStr.isEmpty() ||
            targetAmount == null || minSpend == null || maxSpend == null) {

            Toast.makeText(this, "Please fill in all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val milestone = Milestone(
            id = null,
            userId = userId!!,
            name = goalName,
            targetAmount = targetAmount,
            deadline = deadline,
            minMonthlySpend = minSpend,
            maxMonthlySpend = maxSpend
        )

        db.collection("milestones")
            .add(milestone)
            .addOnSuccessListener {
                Toast.makeText(this, "Milestone added", Toast.LENGTH_SHORT).show()
                resetFields()
                val intent = Intent(this, RewardsMilestonesActivity::class.java)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add milestone: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun resetFields() {
        editTextGoalName.text.clear()
        editTextTargetAmount.text.clear()
        editTextDeadline.text.clear()
        editTextMinSpend.text.clear()
        editTextMaxSpend.text.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        milestonesListener?.remove()
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
                R.id.navigation_categories -> {
                    startActivity(Intent(this, CategoriesActivity::class.java))
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
                    startActivity(Intent(this, IncomeActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_milestones -> true
                else -> false
            }
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val currentDateStr = editTextDeadline.text.toString()
        if (currentDateStr.isNotEmpty()) {
            try {
                val date = dateFormat.parse(currentDateStr)
                if (date != null) calendar.time = date
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                editTextDeadline.setText(dateFormat.format(selectedCalendar.time))
            }, year, month, day)

        datePickerDialog.show()
    }

    private fun startSpeechToText(requestCode: Int) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        try {
            startActivityForResult(intent, requestCode)
        } catch (a: ActivityNotFoundException) {
            Toast.makeText(this, "Speech recognition is not supported on your device", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!result.isNullOrEmpty()) {
                val spokenText = result[0]
                when (requestCode) {
                    SPEECH_REQUEST_CODE_GOAL_NAME -> editTextGoalName.setText(spokenText)
                    SPEECH_REQUEST_CODE_TARGET_AMOUNT -> editTextTargetAmount.setText(spokenText)
                    SPEECH_REQUEST_CODE_MIN_SPEND -> editTextMinSpend.setText(spokenText)
                    SPEECH_REQUEST_CODE_MAX_SPEND -> editTextMaxSpend.setText(spokenText)
                }
            }
        }
    }
}
