package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MilestonesActivity : AppCompatActivity() {

    private lateinit var milestoneRecyclerView: RecyclerView
    private lateinit var editTextGoalName: EditText
    private lateinit var editTextTargetAmount: EditText
    private lateinit var editTextDeadline: EditText
    private lateinit var editTextMinSpend: EditText
    private lateinit var editTextMaxSpend: EditText
    private lateinit var buttonAddMilestone: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var milestoneAdapter: MilestoneAdapter
    private val milestoneList = mutableListOf<Milestone>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var milestonesListener: ListenerRegistration? = null
    private var userId: String? = null  // Firebase user ID is String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_milestones)

        // Get logged-in Firebase user ID
        userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Initialize views
        milestoneRecyclerView = findViewById(R.id.milestoneRecyclerView)
        editTextGoalName = findViewById(R.id.editTextGoalName)
        editTextTargetAmount = findViewById(R.id.editTextTargetAmount)
        editTextDeadline = findViewById(R.id.editTextDeadline)
        editTextMinSpend = findViewById(R.id.minSpendText)
        editTextMaxSpend = findViewById(R.id.maxSpendText)
        buttonAddMilestone = findViewById(R.id.buttonAddMilestone)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        milestoneAdapter = MilestoneAdapter(milestoneList)
        milestoneRecyclerView.layoutManager = LinearLayoutManager(this)
        milestoneRecyclerView.adapter = milestoneAdapter

        loadMilestones()

        buttonAddMilestone.setOnClickListener {
            addMilestone()
        }

        setupBottomNavigation()
    }

    private fun loadMilestones() {
        // Remove previous listener if any
        milestonesListener?.remove()

        // Listen for real-time updates from Firestore
        milestonesListener = db.collection("milestones")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading milestones: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    milestoneList.clear()
                    for (doc in snapshot.documents) {
                        val milestone = doc.toObject(Milestone::class.java)
                        if (milestone != null) {
                            milestoneList.add(milestone.copy(id = doc.id))
                        }
                    }
                    milestoneAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun addMilestone() {
        val goalName = editTextGoalName.text.toString().trim()
        val targetAmount = editTextTargetAmount.text.toString().toDoubleOrNull()
        val deadline = editTextDeadline.text.toString().trim()
        val minSpend = editTextMinSpend.text.toString().toDoubleOrNull()
        val maxSpend = editTextMaxSpend.text.toString().toDoubleOrNull()

        if (goalName.isEmpty() || targetAmount == null || deadline.isEmpty() || minSpend == null || maxSpend == null) {
            Toast.makeText(this, "Fill in all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val milestone = Milestone(
            id = null,  // will be set by Firestore
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
        // Remove Firestore listener when activity is destroyed
        milestonesListener?.remove()
    }

    class MilestoneAdapter(private val items: List<Milestone>) :
        RecyclerView.Adapter<MilestonesActivity.MilestoneAdapter.MilestoneViewHolder>() {

        inner class MilestoneViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val goalName: TextView = view.findViewById(R.id.textGoalName)
            val targetAmount: TextView = view.findViewById(R.id.textTargetAmount)
            val deadline: TextView = view.findViewById(R.id.textDeadline)
            val minSpend: TextView = view.findViewById(R.id.minSpendText)
            val maxSpend: TextView = view.findViewById(R.id.maxSpendText)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MilestoneViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_milestone, parent, false)
            return MilestoneViewHolder(view)
        }

        override fun onBindViewHolder(holder: MilestoneViewHolder, position: Int) {
            val item = items[position]
            holder.goalName.text = item.name
            holder.targetAmount.text = "Target: R %.2f".format(item.targetAmount)
            holder.deadline.text = "Deadline: ${item.deadline}"
            holder.minSpend.text = "Min Monthly Spend: R %.2f".format(item.minMonthlySpend)
            holder.maxSpend.text = "Max Monthly Spend: R %.2f".format(item.maxMonthlySpend)
        }

        override fun getItemCount() = items.size
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
                R.id.navigation_income -> {
                    startActivity(Intent(this, IncomeActivity::class.java).apply {
                        putExtra("userId", userId)
                    })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_milestones -> true
                else -> false
            }
        }
    }
}
