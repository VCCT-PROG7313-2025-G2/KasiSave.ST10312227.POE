package com.example.kasisave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ExpensesAdapter(
    private val expenses: MutableList<Expense>,
    private val onLongClick: (Expense, Int) -> Unit
) : RecyclerView.Adapter<ExpensesAdapter.ExpenseViewHolder>() {

    // ViewHolder to hold the reference to each item's view elements
    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val expenseNameTextView: TextView = itemView.findViewById(R.id.expenseNameTextView)
        val expenseAmountTextView: TextView = itemView.findViewById(R.id.expenseAmountTextView)
    }

    // Creates and returns a ViewHolder for the given view type
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    // Binds the data (expense) to the ViewHolder at the given position
    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        holder.expenseNameTextView.text = expense.name
        holder.expenseAmountTextView.text = "R ${expense.amount}"

        holder.itemView.setOnLongClickListener {
            onLongClick(expense, position)
            true
        }
    }

    // Returns the total number of items in the list
    override fun getItemCount() = expenses.size
}
