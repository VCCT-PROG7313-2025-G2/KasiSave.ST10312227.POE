package com.example.kasisave.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.Milestone
import com.example.kasisave.R


class MilestonesAdapter(private var milestones: List<Milestone>) :
    RecyclerView.Adapter<MilestonesAdapter.MilestoneViewHolder>() {

    class MilestoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val goalNameText: TextView = itemView.findViewById(R.id.textGoalName)
        val targetAmountText: TextView = itemView.findViewById(R.id.textTargetAmount)
        val deadlineText: TextView = itemView.findViewById(R.id.textDeadline)
        val minSpendText: TextView = itemView.findViewById(R.id.minSpendText)
        val maxSpendText: TextView = itemView.findViewById(R.id.maxSpendText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MilestoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_milestone, parent, false)
        return MilestoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: MilestoneViewHolder, position: Int) {
        val milestone = milestones[position]
        holder.goalNameText.text = "Goal: ${milestone.name}"
        holder.targetAmountText.text = "Target: R${milestone.targetAmount}"
        holder.deadlineText.text = "Deadline: ${milestone.deadline}"
        holder.minSpendText.text = "Min Monthly Spend: R${milestone.minMonthlySpend}"
        holder.maxSpendText.text = "Max Monthly Spend: R${milestone.maxMonthlySpend}"
    }

    override fun getItemCount(): Int = milestones.size

    fun updateData(newMilestones: List<Milestone>) {
        milestones = newMilestones
        notifyDataSetChanged()
    }
}
