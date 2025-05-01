package com.example.kasisave

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ExpenseAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    // Date formatter for displaying the timestamp
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        // Set the formatted date
        val formattedDate = dateFormatter.format(Date(expense.dateMillis))
        holder.categoryTextView.text = "Category: ${expense.category}"
        holder.amountTextView.text = "Amount: R %.2f".format(expense.amount)
        holder.dateTextView.text = "Date: $formattedDate"
        holder.descriptionTextView.text = "Description: ${expense.description}"
        holder.timeTextView.text = "Time: ${expense.startTime} - ${expense.endTime}"
        holder.recurringTextView.text = if (expense.isRecurring) "Recurring: Yes" else "Recurring: No"

        // Show photo if available
        if (!expense.photoUri.isNullOrEmpty()) {
            holder.photoImageView.visibility = View.VISIBLE
            holder.photoImageView.setImageURI(Uri.parse(expense.photoUri))
        } else {
            holder.photoImageView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = expenses.size

    // Update the list of expenses in the adapter
    fun updateData(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }

    // Method to filter expenses based on a date range (startDate and endDate in milliseconds)
    fun filterExpensesByDate(startDate: Long, endDate: Long) {
        val filteredExpenses = expenses.filter {
            val expenseDate = it.dateMillis
            expenseDate >= startDate && expenseDate <= endDate
        }
        expenses = filteredExpenses
        notifyDataSetChanged()
    }

    // Optionally, set expenses directly if needed
    fun setExpenses(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
