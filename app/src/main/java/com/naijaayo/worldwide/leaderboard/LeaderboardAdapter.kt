package com.naijaayo.worldwide.leaderboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.User

class LeaderboardAdapter(
    private val users: List<User>,
    private val isSinglePlayer: Boolean = true
) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankTextView: TextView = view.findViewById(R.id.rankTextView)
        val avatarImageView: ImageView = view.findViewById(R.id.avatarImageView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val ratingTextView: TextView = view.findViewById(R.id.ratingTextView)
        val winsLossesTextView: TextView = view.findViewById(R.id.winsLossesTextView)
        val singlePlayerStats: LinearLayout = view.findViewById(R.id.singlePlayerStats)
        val multiplayerStats: LinearLayout = view.findViewById(R.id.multiplayerStats)
        val gamesPlayedTextView: TextView = view.findViewById(R.id.gamesPlayedTextView)
        val winRateTextView: TextView = view.findViewById(R.id.winRateTextView)
        val multiplayerRatingTextView: TextView = view.findViewById(R.id.multiplayerRatingTextView)
        val tournamentRankTextView: TextView = view.findViewById(R.id.tournamentRankTextView)
        val achievementLayout: LinearLayout = view.findViewById(R.id.achievementLayout)
        val streakIcon: ImageView = view.findViewById(R.id.streakIcon)
        val tournamentIcon: ImageView = view.findViewById(R.id.tournamentIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        val rank = position + 1

        // Set rank with special styling for top 3
        holder.rankTextView.text = rank.toString()
        when (rank) {
            1 -> holder.rankTextView.background = holder.itemView.context.getDrawable(R.drawable.circle_rank_background)
            2 -> {
                val secondPlaceDrawable = holder.itemView.context.getDrawable(R.drawable.circle_rank_background)?.mutate()
                secondPlaceDrawable?.setTint(android.graphics.Color.GRAY)
                holder.rankTextView.background = secondPlaceDrawable
            }
            3 -> {
                val thirdPlaceDrawable = holder.itemView.context.getDrawable(R.drawable.circle_rank_background)?.mutate()
                thirdPlaceDrawable?.setTint(android.graphics.Color.parseColor("#CD7F32"))
                holder.rankTextView.background = thirdPlaceDrawable
            }
            else -> {
                val defaultDrawable = holder.itemView.context.getDrawable(R.drawable.circle_rank_background)?.mutate()
                defaultDrawable?.setTint(android.graphics.Color.parseColor("#666666"))
                holder.rankTextView.background = defaultDrawable
            }
        }

        // Set user info
        holder.usernameTextView.text = user.username
        holder.ratingTextView.text = user.rating.toString()
        holder.winsLossesTextView.text = "${user.wins}W - ${user.losses}L"

        // Set avatar based on user's avatarId
        val avatarResId = when (user.avatarId) {
            "ayo" -> R.drawable.char_ayo_portrait
            "ada" -> R.drawable.char_ada_portrait
            "fatima" -> R.drawable.char_fatima_portrait
            else -> R.mipmap.ic_launcher
        }
        holder.avatarImageView.setImageResource(avatarResId)

        // Show/hide stats based on leaderboard type
        if (isSinglePlayer) {
            holder.singlePlayerStats.visibility = View.VISIBLE
            holder.multiplayerStats.visibility = View.GONE

            // Calculate and display single player stats
            val totalGames = user.wins + user.losses
            val winRate = if (totalGames > 0) (user.wins * 100) / totalGames else 0

            holder.gamesPlayedTextView.text = "Games: $totalGames"
            holder.winRateTextView.text = "Win Rate: $winRate%"

            // Show achievement icons for single player
            showSinglePlayerAchievements(holder, user)

        } else {
            holder.singlePlayerStats.visibility = View.GONE
            holder.multiplayerStats.visibility = View.VISIBLE

            // Display multiplayer-specific stats
            holder.multiplayerRatingTextView.text = "MP Rating: ${user.rating}"
            holder.tournamentRankTextView.text = "Tournament: #$rank"

            // Show achievement icons for multiplayer
            showMultiplayerAchievements(holder, user, rank)
        }
    }

    private fun showSinglePlayerAchievements(holder: ViewHolder, user: User) {
        holder.achievementLayout.visibility = View.VISIBLE

        // Show streak icon if user has a good win rate
        val totalGames = user.wins + user.losses
        val winRate = if (totalGames > 0) (user.wins * 100) / totalGames else 0

        holder.streakIcon.visibility = if (winRate >= 70 && totalGames >= 10) View.VISIBLE else View.GONE
        holder.tournamentIcon.visibility = View.GONE // Not applicable for single player
    }

    private fun showMultiplayerAchievements(holder: ViewHolder, user: User, rank: Int) {
        holder.achievementLayout.visibility = View.VISIBLE

        // Show tournament icon for top players
        holder.streakIcon.visibility = View.GONE // Could be used for consecutive wins
        holder.tournamentIcon.visibility = if (rank <= 3) View.VISIBLE else View.GONE
    }

    override fun getItemCount() = users.size
}
