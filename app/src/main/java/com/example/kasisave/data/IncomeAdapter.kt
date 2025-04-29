package com.example.kasisave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class IncomeAdapter : ListAdapter<Income, IncomeAdapter.IncomeViewHolder>(IncomeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_income, parent, false)
        return IncomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val amountTextView: TextView = itemView.findViewById(R.id.itemIncomeAmount)
        private val categoryTextView: TextView = itemView.findViewById(R.id.itemIncomeCategory)
        private val dateTextView: TextView = itemView.findViewById(R.id.itemIncomeDate)

        fun bind(income: Income) {
            amountTextView.text = "R${income.amount}"
            categoryTextView.text = income.category
            dateTextView.text = income.date
        }
    }
}

class IncomeDiffCallback : DiffUtil.ItemCallback<Income>() {
    override fun areItemsTheSame(oldItem: Income, newItem: Income): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Income, newItem: Income): Boolean = oldItem == newItem
}
