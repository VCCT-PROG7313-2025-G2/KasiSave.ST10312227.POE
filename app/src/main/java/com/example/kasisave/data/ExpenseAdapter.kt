package com.example.kasisave

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

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
        holder.categoryTextView.text = "Category: ${expense.category}"
        holder.amountTextView.text = "Amount: R %.2f".format(expense.amount)
        holder.dateTextView.text = "Date: ${expense.date}"
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

    fun updateData(newExpenses: List<Expense>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}
