package com.example.kasisave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpenseAdapter(private val expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.expenseNameText)
        val amountText: TextView = itemView.findViewById(R.id.expenseAmountText)
        val categoryText: TextView = itemView.findViewById(R.id.expenseCategoryText)
        val dateText: TextView = itemView.findViewById(R.id.expenseDateText)
        val recurringText: TextView = itemView.findViewById(R.id.expenseRecurringText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.amountText.text = "R %.2f".format(expense.amount)
        holder.categoryText.text = "Category: ${expense.category}"
        holder.dateText.text = "Date: ${expense.date}"
        holder.recurringText.text = if (expense.isRecurring) "Recurring" else "One-time"
    }

    override fun getItemCount() = expenses.size
}
