package com.example.kasisave.data

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kasisave.LeaderboardUser
import com.example.kasisave.R
import com.google.firebase.auth.FirebaseAuth

class LeaderboardAdapter(private val userList: List<LeaderboardUser>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        val tvCoins: TextView = itemView.findViewById(R.id.tvCoins)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        val context = holder.itemView.context

        holder.tvRank.text = (position + 1).toString()
        holder.tvUsername.text = user.username
        holder.tvCoins.text = "${user.coins} 🪙"

        // Load avatar from drawable by name
        val avatarResId = context.resources.getIdentifier(
            user.avatarName,
            "drawable",
            context.packageName
        )

        if (avatarResId != 0) {
            holder.ivAvatar.setImageResource(avatarResId)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_avatar) // fallback
        }

        // Color top 3 ranks
        val rankColor = when (position) {
            0 -> ContextCompat.getColor(context, R.color.gold)
            1 -> ContextCompat.getColor(context, R.color.silver)
            2 -> ContextCompat.getColor(context, R.color.bronze)
            else -> ContextCompat.getColor(context, R.color.black)
        }

        holder.tvRank.setTextColor(rankColor)
        holder.tvUsername.setTextColor(rankColor)
        holder.tvCoins.setTextColor(rankColor)
    }

    override fun getItemCount(): Int = userList.size
}
