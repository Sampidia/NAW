package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.network.Game
import com.naijaayo.worldwide.theme.AvatarPreferenceManager

class RoomAdapter(
    private var rooms: List<Game>,
    private val onJoinClick: (Game) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val creatorAvatar: ImageView = itemView.findViewById(R.id.creatorAvatar)
        val roomNameText: TextView = itemView.findViewById(R.id.roomNameText)
        val roomIdText: TextView = itemView.findViewById(R.id.roomIdText)
        val joinButton: Button = itemView.findViewById(R.id.joinButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val game = rooms[position]
        val creator = game.players.values.firstOrNull()
        
        holder.roomNameText.text = creator?.displayName?.let { "$it's Room" } ?: "Unknown Room"
        holder.roomIdText.text = "ID: ${game.roomId}"
        
        val avatarId = creator?.avatarId ?: "ayo"
        holder.creatorAvatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(avatarId))

        // Check if room is joinable
        val isJoinable = game.status == "waiting" && game.players.size < 2
        
        holder.joinButton.isEnabled = isJoinable
        holder.joinButton.alpha = if (isJoinable) 1.0f else 0.5f
        
        // Update button text based on status
        holder.joinButton.text = when {
            isJoinable -> "Join"
            game.status == "playing" -> "Playing"
            else -> "Full"
        }

        holder.joinButton.setOnClickListener {
            if (isJoinable) {
                onJoinClick(game)
            }
        }
    }

    override fun getItemCount() = rooms.size

    fun updateRooms(newRooms: List<Game>) {
        rooms = newRooms
        notifyDataSetChanged()
    }
}
