package com.naijaayo.worldwide.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R

class LeaderboardAdapter(private val leaderboard: List<LeaderboardEntry>) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankTextView: TextView = view.findViewById(R.id.rankTextView)
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
        val nameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val scoreTextView: TextView = view.findViewById(R.id.ratingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = leaderboard[position]

        holder.rankTextView.text = (position + 1).toString()
        holder.nameTextView.text = entry.displayName
        holder.scoreTextView.text = entry.score.toString()

        val avatarResId = when (entry.avatarId) {
            "ayo" -> R.drawable.char_ayo_portrait
            "ada" -> R.drawable.char_ada_portrait
            "fatima" -> R.drawable.char_fatima_portrait
            else -> R.mipmap.ic_launcher_foreground
        }
        holder.avatarImageView.setImageResource(avatarResId)
    }

    override fun getItemCount() = leaderboard.size
}
