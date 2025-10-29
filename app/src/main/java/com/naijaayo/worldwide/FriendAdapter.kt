package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendAdapter(
    private var friends: List<Friend>,
    private val onChatClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val friendAvatar: ImageView = view.findViewById(R.id.friendAvatar)
        val friendName: TextView = view.findViewById(R.id.friendName)
        val onlineStatus: TextView = view.findViewById(R.id.onlineStatus)
        val onlineIndicator: ImageView = view.findViewById(R.id.onlineIndicator)
        val chatButton: ImageButton = view.findViewById(R.id.chatButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]

        // Set avatar
        val avatarRes = getAvatarResource(friend.friendAvatarId)
        holder.friendAvatar.setImageResource(avatarRes)

        // Set name
        holder.friendName.text = friend.friendUsername

        // Set online status
        if (friend.isOnline) {
            holder.onlineStatus.text = "Online"
            holder.onlineIndicator.setBackgroundResource(R.color.online_green)
        } else {
            holder.onlineStatus.text = "Offline"
            holder.onlineIndicator.setBackgroundResource(R.color.offline_gray)
        }

        // Set chat button click listener
        holder.chatButton.setOnClickListener {
            onChatClick(friend)
        }
    }

    override fun getItemCount() = friends.size

    fun updateFriends(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
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
}