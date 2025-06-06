package com.example.kasisave

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(
    private val context: Context,
    private var expenses: List<Expense>
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val CHANNEL_ID = "expense_notifications"

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategory)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewAmount)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
        val descriptionTextView: TextView = itemView.findViewById(R.id.textViewDescription)
        val timeTextView: TextView = itemView.findViewById(R.id.textViewTime)
        val recurringTextView: TextView = itemView.findViewById(R.id.textViewRecurring)
        val photoImageView: ImageView = itemView.findViewById(R.id.imageViewPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        holder.categoryTextView.text = "Category: ${expense.category}"
        holder.amountTextView.text = "Amount: R %.2f".format(expense.amount)
        holder.dateTextView.text = "Date: ${dateFormatter.format(Date(expense.dateMillis))}"
        holder.descriptionTextView.text = "Description: ${expense.description}"
        holder.timeTextView.text = "Time: ${expense.startTime} - ${expense.endTime}"
        holder.recurringTextView.text = if (expense.isRecurring) "Recurring: Yes" else "Recurring: No"

        if (!expense.photoUri.isNullOrBlank()) {
            holder.photoImageView.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(expense.photoUri)
                .into(holder.photoImageView)
        } else {
            holder.photoImageView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun setExpenses(newExpenses: List<Expense>) {
        val newExpenseAdded = newExpenses.size > expenses.size
        expenses = newExpenses
        notifyDataSetChanged()

        if (newExpenseAdded && newExpenses.isNotEmpty()) {
            val latest = newExpenses.first()
            showNotification("Expense added", "${latest.category}: R${latest.amount}")

            if (latest.amount >= 500) {
                showNotification("Warning", "High expense detected (R${latest.amount})")
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        createNotificationChannel()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Expense Notifications"
            val descriptionText = "Notifications for added and high-value expenses"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun filterExpensesByDate(startDate: Long, endDate: Long) {
        val filteredExpenses = expenses.filter {
            it.dateMillis in startDate..endDate
        }
        expenses = filteredExpenses
        notifyDataSetChanged()
    }
}
