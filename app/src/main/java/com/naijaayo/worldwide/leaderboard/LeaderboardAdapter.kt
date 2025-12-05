package com.naijaayo.worldwide.leaderboard

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.theme.AvatarPreferenceManager

class LeaderboardAdapter(private val leaderboard: List<LeaderboardEntry>) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankTextView: TextView = view.findViewById(R.id.rankTextView)
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val gamesTextView: TextView = view.findViewById(R.id.gamesTextView)
        val winRateTextView: TextView = view.findViewById(R.id.winRateTextView)
        val pointsLabel: TextView = view.findViewById(R.id.pointsLabel)
        val winBadge: TextView = view.findViewById(R.id.winBadge)
        val drawBadge: TextView = view.findViewById(R.id.drawBadge)
        val lostBadge: TextView = view.findViewById(R.id.lostBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = leaderboard[position]
        val context = holder.itemView.context

        // Rank with colored background based on position
        holder.rankTextView.text = (position + 1).toString()
        val rankBackground = holder.rankTextView.background as? GradientDrawable
        when (position) {
            0 -> rankBackground?.setColor(ContextCompat.getColor(context, R.color.rank_gold))
            1 -> rankBackground?.setColor(ContextCompat.getColor(context, R.color.rank_silver))
            2 -> rankBackground?.setColor(ContextCompat.getColor(context, R.color.rank_bronze))
            else -> rankBackground?.setColor(ContextCompat.getColor(context, R.color.rank_default))
        }

        // Avatar
        holder.avatarImageView.setImageResource(AvatarPreferenceManager.getAvatarPortrait(entry.avatarId))

        // Username
        holder.usernameTextView.text = entry.displayName.ifEmpty { entry.username }

        // Games (separate line)
        holder.gamesTextView.text = "Games: ${entry.games}"

        // Win Rate (separate line)
        holder.winRateTextView.text = "Win rate: ${entry.winRate}%"

        // Points
        holder.pointsLabel.text = "Points: ${entry.totalPoints}"

        // Win/Draw/Lost Badges
        holder.winBadge.text = "Win: ${entry.wins}"
        holder.drawBadge.text = "Draw: ${entry.draws}"
        holder.lostBadge.text = "Lost: ${entry.losses}"
    }

    override fun getItemCount() = leaderboard.size
}
