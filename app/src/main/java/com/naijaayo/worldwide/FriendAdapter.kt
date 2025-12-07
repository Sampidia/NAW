package com.naijaayo.worldwide

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.model.Friend
import com.naijaayo.worldwide.theme.AvatarPreferenceManager

class FriendAdapter(
    private var friends: List<Friend>,
    private val onChatClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    // Map to track online status by friend ID
    private val onlineStatusMap = mutableMapOf<String, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        // Use the online status from map if available, otherwise use friend's isOnline
        val isOnline = onlineStatusMap[friend.id] ?: friend.isOnline
        holder.bind(friend, isOnline, onChatClick)
    }

    override fun getItemCount(): Int = friends.size

    fun updateFriends(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }

    fun updateOnlineStatus(friendId: String, isOnline: Boolean) {
        onlineStatusMap[friendId] = isOnline
        // Find the position of this friend and notify item changed
        val position = friends.indexOfFirst { it.id == friendId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImageView: ImageView = itemView.findViewById(R.id.friendAvatar)
        private val nameTextView: TextView = itemView.findViewById(R.id.friendName)
        private val onlineStatusTextView: TextView = itemView.findViewById(R.id.onlineStatus)
        private val chatButton: ImageButton = itemView.findViewById(R.id.chatButton)

        fun bind(friend: Friend, isOnline: Boolean, onChatClick: (Friend) -> Unit) {
            nameTextView.text = friend.name
            avatarImageView.setImageResource(AvatarPreferenceManager.getAvatarPortrait(friend.avatar))
            
            // Set online status text and color
            if (isOnline) {
                onlineStatusTextView.text = "Online"
                onlineStatusTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.online_green))
            } else {
                onlineStatusTextView.text = "Offline"
                onlineStatusTextView.setTextColor(Color.parseColor("#4d311e"))
            }
            
            // Set click listener on the chat button specifically
            chatButton.setOnClickListener { 
                onChatClick(friend) 
            }
            
            // Also allow clicking the whole card to open chat
            itemView.setOnClickListener { 
                onChatClick(friend) 
            }
        }
    }
}
