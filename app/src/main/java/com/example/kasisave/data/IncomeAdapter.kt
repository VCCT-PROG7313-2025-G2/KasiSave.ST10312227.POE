package com.example.kasisave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IncomeAdapter(
    private val incomes: List<Income>,
    private val onDeleteClick: (Income, Int) -> Unit
) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    inner class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sourceTextView: TextView = itemView.findViewById(R.id.incomeSourceTextView)
        val amountTextView: TextView = itemView.findViewById(R.id.incomeAmountTextView)
        val categoryTextView: TextView = itemView.findViewById(R.id.incomeCategoryTextView)

        init {
            itemView.setOnLongClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onDeleteClick(incomes[pos], pos)
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_income, parent, false)
        return IncomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = incomes[position]
        holder.sourceTextView.text = income.source
        holder.amountTextView.text = "R ${"%.2f".format(income.amount)}"
        holder.categoryTextView.text = income.category
    }

    override fun getItemCount(): Int = incomes.size
}
