package com.example.kasisave

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

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

    private lateinit var db: ExpenseDatabase
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_milestones)

        //Correct key to match LoginActivity
        userId = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
            .getInt("user_id", -1)

        if (userId == -1) {
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

        db = ExpenseDatabase.getDatabase(this)

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
        lifecycleScope.launch {
            db.milestoneDao().getAllMilestonesForUser(userId).collect { loaded ->
                milestoneList.clear()
                milestoneList.addAll(loaded)
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
            userId = userId,
            name = goalName,
            targetAmount = targetAmount,
            deadline = deadline,
            minMonthlySpend = minSpend,
            maxMonthlySpend = maxSpend
        )

        lifecycleScope.launch {
            db.milestoneDao().insertMilestone(milestone)
            resetFields()
            loadMilestones()
        }
    }

    private fun resetFields() {
        editTextGoalName.text.clear()
        editTextTargetAmount.text.clear()
        editTextDeadline.text.clear()
        editTextMinSpend.text.clear()
        editTextMaxSpend.text.clear()
    }

    class MilestoneAdapter(private val items: List<Milestone>) :
        RecyclerView.Adapter<MilestoneAdapter.MilestoneViewHolder>() {

        inner class MilestoneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val goalName: TextView = view.findViewById(R.id.textGoalName)
            val targetAmount: TextView = view.findViewById(R.id.textTargetAmount)
            val deadline: TextView = view.findViewById(R.id.textDeadline)
            val minSpend: TextView = view.findViewById(R.id.minSpendText)
            val maxSpend: TextView = view.findViewById(R.id.maxSpendText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MilestoneViewHolder {
            val view = LayoutInflater.from(parent.context)
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
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_expenses -> {
                    val intent = Intent(this, ExpensesActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.navigation_income -> {
                    val intent = Intent(this, IncomeActivity::class.java)
                    intent.putExtra("userId", userId)
                    startActivity(intent)
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
