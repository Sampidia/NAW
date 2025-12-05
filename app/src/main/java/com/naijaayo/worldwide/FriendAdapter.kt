package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.model.Friend
import com.naijaayo.worldwide.theme.AvatarPreferenceManager

class FriendAdapter(
    private var friends: List<Friend>,
    private val onChatClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        holder.bind(friend, onChatClick)
    }

    override fun getItemCount(): Int = friends.size

    fun updateFriends(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatarImageView: ImageView = itemView.findViewById(R.id.friendAvatar)
        private val nameTextView: TextView = itemView.findViewById(R.id.friendName)
        private val chatButton: ImageButton = itemView.findViewById(R.id.chatButton)

        fun bind(friend: Friend, onChatClick: (Friend) -> Unit) {
            nameTextView.text = friend.name
            avatarImageView.setImageResource(AvatarPreferenceManager.getAvatarPortrait(friend.avatar))
            
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
