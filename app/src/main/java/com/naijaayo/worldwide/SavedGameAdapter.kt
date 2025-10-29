package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SavedGameAdapter(
    private val savedGames: List<SavedGame>,
    private val onGameSelected: (SavedGame) -> Unit
) : RecyclerView.Adapter<SavedGameAdapter.SavedGameViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedGameViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_game, parent, false)
        return SavedGameViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedGameViewHolder, position: Int) {
        val savedGame = savedGames[position]
        holder.bind(savedGame)
        holder.itemView.setOnClickListener { onGameSelected(savedGame) }
    }

    override fun getItemCount(): Int = savedGames.size

    class SavedGameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val opponentAvatar: ImageView = itemView.findViewById(R.id.opponentAvatar)
        private val opponentName: TextView = itemView.findViewById(R.id.opponentName)
        private val gameMode: TextView = itemView.findViewById(R.id.gameMode)
        private val saveDate: TextView = itemView.findViewById(R.id.saveDate)
        private val onlineStatus: ImageView = itemView.findViewById(R.id.onlineStatus)
        private val onlineStatusText: TextView = itemView.findViewById(R.id.onlineStatusText)

        fun bind(savedGame: SavedGame) {
            // Set opponent avatar and name
            val avatarResId = when (savedGame.gameMode) {
                "single_player" -> R.drawable.char_ai_portrait // AI avatar for single player
                else -> getAvatarResource(savedGame.opponentAvatarId ?: "ayo")
            }
            opponentAvatar.setImageResource(avatarResId)

            opponentName.text = when (savedGame.gameMode) {
                "single_player" -> "AI Opponent"
                else -> savedGame.opponentUsername ?: "Unknown Player"
            }

            // Set game mode and difficulty
            val difficultyText = when (savedGame.difficulty) {
                GameLevel.EASY -> "Easy"
                GameLevel.MEDIUM -> "Medium"
                GameLevel.HARD -> "Hard"
            }
            val modeText = when (savedGame.gameMode) {
                "single_player" -> "Single Player"
                else -> "Multiplayer"
            }
            gameMode.text = "$modeText • $difficultyText"

            // Set save date (relative time)
            saveDate.text = "Saved: ${getRelativeTime(savedGame.savedAt)}"

            // Set online status (only for multiplayer)
            if (savedGame.gameMode == "multiplayer") {
                val isOnline = savedGame.isOpponentOnline
                onlineStatus.setColorFilter(
                    if (isOnline) android.graphics.Color.GREEN else android.graphics.Color.GRAY
                )
                onlineStatusText.text = if (isOnline) "Online" else "Offline"
                onlineStatusText.setTextColor(
                    if (isOnline) android.graphics.Color.GREEN else android.graphics.Color.GRAY
                )
            } else {
                onlineStatus.visibility = View.GONE
                onlineStatusText.visibility = View.GONE
            }
        }

        private fun getAvatarResource(avatarId: String): Int {
            return when (avatarId) {
                "ayo" -> R.drawable.char_ayo_portrait
                "ada" -> R.drawable.char_ada_portrait
                "fatima" -> R.drawable.char_fatima_portrait
                "ai" -> R.drawable.char_ai_portrait
                else -> R.drawable.char_ayo_portrait
            }
        }

        private fun getRelativeTime(savedAt: String): String {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                val savedDate = sdf.parse(savedAt)
                val now = Date()
                val diffInMillis = now.time - savedDate.time

                val minutes = diffInMillis / (1000 * 60)
                val hours = diffInMillis / (1000 * 60 * 60)
                val days = diffInMillis / (1000 * 60 * 60 * 24)

                when {
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    else -> "${days}d ago"
                }
            } catch (e: Exception) {
                "Recently"
            }
        }
    }
}