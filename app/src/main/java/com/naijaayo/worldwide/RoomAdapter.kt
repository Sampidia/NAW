package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.naijaayo.worldwide.theme.AvatarPreferenceManager

class RoomAdapter(
    private var rooms: List<PlayNowGame>,
    private val onJoinClick: (PlayNowGame) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val p1Avatar: ImageView = itemView.findViewById(R.id.p1Avatar)
        val p1Name: TextView = itemView.findViewById(R.id.p1Name)
        val p2Avatar: ImageView = itemView.findViewById(R.id.p2Avatar)
        val p2Name: TextView = itemView.findViewById(R.id.p2Name)
        val joinButton: Button = itemView.findViewById(R.id.joinButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val game = rooms[position]
        
        // Player 1 (Creator) - usually the first player in the map
        // We need to be careful as map order isn't guaranteed, but usually creator is first.
        // Better strategy: Sort by join time or just take the first one found.
        val players = game.players.values.toList()
        val player1 = players.firstOrNull()
        val player2 = if (players.size > 1) players[1] else null

        // Bind Player 1
        if (player1 != null) {
            holder.p1Name.text = player1.displayName
            holder.p1Avatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(player1.avatarId ?: "ayo"))
        } else {
            holder.p1Name.text = "Unknown"
            holder.p1Avatar.setImageResource(R.drawable.char_ayo_portrait)
        }

        // Bind Player 2
        if (player2 != null) {
            holder.p2Name.text = player2.displayName
            holder.p2Avatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(player2.avatarId ?: "ayo"))
            holder.p2Avatar.alpha = 1.0f
            holder.p2Name.alpha = 1.0f
        } else {
            holder.p2Name.text = "Waiting..."
            holder.p2Avatar.setImageResource(R.drawable.char_ayo_portrait) // Default 'ayo' avatar
            holder.p2Avatar.alpha = 0.4f // "Blur" / Ghost effect
            holder.p2Name.alpha = 0.5f
        }

        // Check if room is joinable
        val isJoinable = game.status == "waiting" && game.players.size < 2
        
        holder.joinButton.isEnabled = isJoinable
        
        if (isJoinable) {
            holder.joinButton.text = "Join Game"
            holder.joinButton.alpha = 1.0f
        } else {
            holder.joinButton.text = "Full"
            holder.joinButton.alpha = 0.5f
        }

        holder.joinButton.setOnClickListener {
            if (isJoinable) {
                onJoinClick(game)
            }
        }

        // Animation
        setFadeAnimation(holder.itemView)
    }

    private fun setFadeAnimation(view: View) {
        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 500
        view.startAnimation(anim)
    }

    override fun getItemCount() = rooms.size

    fun updateRooms(newRooms: List<PlayNowGame>) {
        rooms = newRooms
        notifyDataSetChanged()
    }
}
