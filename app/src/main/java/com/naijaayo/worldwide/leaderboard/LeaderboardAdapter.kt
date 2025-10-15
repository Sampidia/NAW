package com.naijaayo.worldwide.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.User

class LeaderboardAdapter(private val users: List<User>) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val ratingTextView: TextView = view.findViewById(R.id.ratingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.usernameTextView.text = user.username
        holder.ratingTextView.text = user.rating.toString()

        // Set avatar based on user's avatarId
        val avatarResId = when (user.avatarId) {
            "ayo" -> R.drawable.char_ayo_portrait
            "ada" -> R.drawable.char_ada_portrait
            "fatima" -> R.drawable.char_fatima_portrait
            else -> R.mipmap.ic_launcher
        }
        holder.avatarImageView.setImageResource(avatarResId)
    }

    override fun getItemCount() = users.size
}
