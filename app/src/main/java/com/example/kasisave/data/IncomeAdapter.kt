package com.example.kasisave

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class IncomeAdapter(
    private val incomes: MutableList<Income>,
    private val onLongClick: (Income, Int) -> Unit
) : RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder>() {

    inner class IncomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val incomeSource: TextView = itemView.findViewById(R.id.incomeSourceTextView)
        val incomeAmount: TextView = itemView.findViewById(R.id.incomeAmountTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_income, parent, false)
        return IncomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = incomes[position]
        holder.incomeSource.text = income.source
        holder.incomeAmount.text = "R ${income.amount}"

        holder.itemView.setOnLongClickListener {
            onLongClick(income, position)
            true
        }
    }

    override fun getItemCount() = incomes.size
}
