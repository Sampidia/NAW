package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendRequestAdapter(
    private var requests: List<FriendRequest>,
    private val onResponseClick: (FriendRequest, Boolean) -> Unit
) : RecyclerView.Adapter<FriendRequestAdapter.FriendRequestViewHolder>() {

    class FriendRequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val requesterAvatar: ImageView = view.findViewById(R.id.requesterAvatar)
        val requesterName: TextView = view.findViewById(R.id.requesterName)
        val requesterEmail: TextView = view.findViewById(R.id.requesterEmail)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val declineButton: Button = view.findViewById(R.id.declineButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val request = requests[position]

        // Set avatar
        val avatarRes = getAvatarResource(request.fromAvatarId)
        holder.requesterAvatar.setImageResource(avatarRes)

        // Set requester info
        holder.requesterName.text = request.fromUsername
        holder.requesterEmail.text = request.fromEmail

        // Set button click listeners
        holder.acceptButton.setOnClickListener {
            onResponseClick(request, true)
            // Update buttons after response
            holder.acceptButton.isEnabled = false
            holder.declineButton.isEnabled = false
            holder.acceptButton.text = "Accepted"
        }

        holder.declineButton.setOnClickListener {
            onResponseClick(request, false)
            // Update buttons after response
            holder.acceptButton.isEnabled = false
            holder.declineButton.isEnabled = false
            holder.declineButton.text = "Declined"
        }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newRequests: List<FriendRequest>) {
        requests = newRequests
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