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
    private lateinit var buttonAddMilestone: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var milestoneAdapter: MilestoneAdapter
    private val milestoneList = mutableListOf<Milestone>()

    private lateinit var db: ExpenseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_milestones)

        milestoneRecyclerView = findViewById(R.id.milestoneRecyclerView)
        editTextGoalName = findViewById(R.id.editTextGoalName)
        editTextTargetAmount = findViewById(R.id.editTextTargetAmount)
        editTextDeadline = findViewById(R.id.editTextDeadline)
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
            db.milestoneDao().getAllMilestones().collect { loaded ->
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

        if (goalName.isEmpty() || targetAmount == null || deadline.isEmpty()) {
            Toast.makeText(this, "Fill in all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        val milestone = Milestone(
            name = goalName,
            targetAmount = targetAmount,
            deadline = deadline
        )

        lifecycleScope.launch {
            db.milestoneDao().insertMilestone(milestone)
            milestoneList.add(milestone)
            milestoneAdapter.notifyItemInserted(milestoneList.size - 1)
            resetFields()
        }
    }

    private fun resetFields() {
        editTextGoalName.text.clear()
        editTextTargetAmount.text.clear()
        editTextDeadline.text.clear()
    }

    // Simple adapter for milestones
    class MilestoneAdapter(private val items: List<Milestone>) :
        RecyclerView.Adapter<MilestoneAdapter.MilestoneViewHolder>() {

        inner class MilestoneViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val goalName: TextView = view.findViewById(R.id.textGoalName)
            val targetAmount: TextView = view.findViewById(R.id.textTargetAmount)
            val deadline: TextView = view.findViewById(R.id.textDeadline)
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
        }

        override fun getItemCount() = items.size
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
                R.id.navigation_milestones -> {

                    true
                }
                else -> false
            }
        }
    }

}

