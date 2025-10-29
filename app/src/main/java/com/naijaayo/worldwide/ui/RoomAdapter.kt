package com.naijaayo.worldwide.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.R
import com.naijaayo.worldwide.Room

class RoomAdapter(
    private var rooms: List<Room>,
    private val onRoomClicked: (Room) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.room_list_item, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]
        holder.bind(room)
    }

    override fun getItemCount(): Int = rooms.size

    fun setData(newRooms: List<Room>) {
        this.rooms = newRooms
        notifyDataSetChanged()
    }

    inner class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val hostAvatarImageView: ImageView = itemView.findViewById(R.id.hostAvatarImageView)
        private val hostNameTextView: TextView = itemView.findViewById(R.id.hostNameTextView)
        private val difficultyTextView: TextView = itemView.findViewById(R.id.difficultyTextView)
        private val joinRoomButton: Button = itemView.findViewById(R.id.joinRoomButton)

        fun bind(room: Room) {
            // Set host name
            hostNameTextView.text = room.hostUsername

            // Set difficulty with color coding
            val difficultyText = when (room.difficulty) {
                com.naijaayo.worldwide.GameLevel.EASY -> "Easy"
                com.naijaayo.worldwide.GameLevel.MEDIUM -> "Medium"
                com.naijaayo.worldwide.GameLevel.HARD -> "Hard"
            }
            difficultyTextView.text = difficultyText

            // Set difficulty text color based on level
            val difficultyColor = when (room.difficulty) {
                com.naijaayo.worldwide.GameLevel.EASY -> "#4CAF50" // Green
                com.naijaayo.worldwide.GameLevel.MEDIUM -> "#FF9800" // Orange
                com.naijaayo.worldwide.GameLevel.HARD -> "#F44336" // Red
            }
            difficultyTextView.setTextColor(android.graphics.Color.parseColor(difficultyColor))

            // Set avatar using ImageManager
            val imageManager = ImageManager(itemView.context)
            val avatarResId = getAvatarResource(room.hostAvatarId)
            hostAvatarImageView.setImageResource(avatarResId)

            // Set click listener for join button
            joinRoomButton.setOnClickListener { onRoomClicked(room) }

            // Optional: Set click listener for the entire item
            itemView.setOnClickListener { onRoomClicked(room) }
        }

        private fun getAvatarResource(avatarId: String): Int {
            return when (avatarId) {
                "ayo" -> R.drawable.char_ayo_portrait
                "ada" -> R.drawable.char_ada_portrait
                "fatima" -> R.drawable.char_fatima_portrait
                "ai" -> R.drawable.char_ai_portrait
                else -> R.drawable.char_ayo_portrait // Default fallback
            }
        }
    }
}
