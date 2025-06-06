package com.example.kasisave.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.NotificationCompat
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
        // Check for context from any existing view if available
        val context = if (milestones.isNotEmpty()) {
            try {
                (milestones as? List<View>)?.firstOrNull()?.context
            } catch (e: Exception) {
                null
            }
        } else null

        // Update list and notify UI
        milestones = newMilestones
        notifyDataSetChanged()

        // Fallback: use context from any attached view
        val fallbackContext = try {
            (milestones as? List<*>)?.firstOrNull()?.let {
                (it as? MilestoneViewHolder)?.itemView?.context
            }
        } catch (e: Exception) {
            null
        }

        (context ?: fallbackContext ?: return).let { ctx ->
            notifyNewMilestones(ctx, newMilestones)
        }
    }

    private fun notifyNewMilestones(context: Context, newMilestones: List<Milestone>) {
        val prefs = context.getSharedPreferences("kasisave_prefs", Context.MODE_PRIVATE)
        val lastSeenCount = prefs.getInt("last_milestone_count", 0)

        if (newMilestones.size > lastSeenCount) {
            val newOnes = newMilestones.take(newMilestones.size - lastSeenCount)

            newOnes.forEach { milestone ->
                sendMilestoneNotification(context, milestone)
            }

            prefs.edit().putInt("last_milestone_count", newMilestones.size).apply()
        }
    }

    private fun sendMilestoneNotification(context: Context, milestone: Milestone) {
        val channelId = "milestone_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Milestone Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val title = "🎯 New Milestone Added"
        val message = "Goal: ${milestone.name}, Target: R${milestone.targetAmount}"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Ensure you have this icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(milestone.name.hashCode(), notification)
    }
}
