package com.example.kasisave

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ExpensesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var noExpensesTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var addExpenseButton: FloatingActionButton
    private lateinit var searchByDateButton: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var adapter: ExpenseAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String = ""

    private val NOTIFICATION_CHANNEL_ID = "expense_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        recyclerView = findViewById(R.id.expensesRecyclerView)
        noExpensesTextView = findViewById(R.id.noExpensesTextView)
        totalExpensesTextView = findViewById(R.id.totalExpensesTextView)
        addExpenseButton = findViewById(R.id.addExpenseButton)
        searchByDateButton = findViewById(R.id.btnSearchByDate)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(this, emptyList())
        recyclerView.adapter = adapter

        val sharedPrefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
        userId = sharedPrefs.getString("user_id", "") ?: ""

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        createNotificationChannel()
        loadExpenses()

        addExpenseButton.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        searchByDateButton.setOnClickListener {
            startActivity(Intent(this, SearchExpenseByDateActivity::class.java))
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.navigation_dashboard -> Intent(this, DashboardActivity::class.java)
                R.id.navigation_expenses -> null
                R.id.navigation_income -> Intent(this, IncomeActivity::class.java)
                R.id.navigation_milestones -> Intent(this, MilestonesActivity::class.java)
                R.id.navigation_categories -> Intent(this, CategoriesActivity::class.java)
                else -> null
            }
            intent?.let {
                startActivity(it)
                overridePendingTransition(0, 0)
                finish()
            }
            true
        }
        bottomNavigationView.selectedItemId = R.id.navigation_expenses
    }

    private fun loadExpenses() {
        firestore.collection("expenses")
            .whereEqualTo("userId", userId)
            .orderBy("dateMillis", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val expenses = result.map { doc ->
                    doc.toObject(Expense::class.java).copy(id = doc.id)
                }

                if (expenses.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    noExpensesTextView.visibility = View.VISIBLE
                    totalExpensesTextView.text = "Total: R 0.00"
                } else {
                    recyclerView.visibility = View.VISIBLE
                    noExpensesTextView.visibility = View.GONE
                    adapter.setExpenses(expenses)

                    val total = expenses.sumOf { it.amount }
                    totalExpensesTextView.text = "Total: R %.2f".format(total)

                    // 🔔 Notify only for new expenses
                    val prefs = getSharedPreferences("kasisave_prefs", MODE_PRIVATE)
                    val lastSeenTime = prefs.getLong("last_seen_expense_time", 0L)

                    val newExpenses = expenses.filter { it.dateMillis > lastSeenTime }

                    newExpenses.forEach {
                        sendExpenseNotification(it)
                    }


                    if (expenses.isNotEmpty()) {
                        val latestTime = expenses.maxOf { it.dateMillis }
                        prefs.edit().putLong("last_seen_expense_time", latestTime).apply()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading expenses: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Expense Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendExpenseNotification(expense: Expense) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this@ExpensesActivity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val message = "R %.2f spent on ${expense.category}".format(expense.amount)
        val title = if (expense.amount > 500) "⚠️ High Expense" else "Expense Added"

        val notification = NotificationCompat.Builder(this@ExpensesActivity,NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Ensure this exists or replace it
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(expense.dateMillis.toInt(), notification)
    }

}
