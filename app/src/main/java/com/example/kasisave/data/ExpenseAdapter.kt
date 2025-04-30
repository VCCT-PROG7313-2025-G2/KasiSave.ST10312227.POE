package com.example.kasisave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ExpenseAdapter(private val expenses: List<Expense>) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]

        holder.categoryTextView.text = expense.category
        holder.amountTextView.text = "R ${"%.2f".format(expense.amount)}"
        holder.dateTextView.text = expense.date
        holder.timeTextView.text = "From: ${expense.startTime} to ${expense.endTime}"
        holder.descriptionTextView.text = expense.description ?: "No description"

        // If photo is present, show it
        if (expense.photoUri != null) {
            Glide.with(holder.photoImageView.context)
                .load(expense.photoUri)
                .into(holder.photoImageView)
        } else {
            holder.photoImageView.visibility = View.GONE
        }

        // Show recurring status
        holder.recurringTextView.text = if (expense.isRecurring) "Recurring" else "One-time"
    }

    override fun getItemCount(): Int {
        return expenses.size
    }

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryTextView: TextView = view.findViewById(R.id.textViewCategory)
        val amountTextView: TextView = view.findViewById(R.id.textViewAmount)
        val dateTextView: TextView = view.findViewById(R.id.textViewDate)
        val timeTextView: TextView = view.findViewById(R.id.textViewTime)
        val descriptionTextView: TextView = view.findViewById(R.id.textViewDescription)
        val photoImageView: ImageView = view.findViewById(R.id.imageViewPhoto)
        val recurringTextView: TextView = view.findViewById(R.id.textViewRecurring)
    }
}
