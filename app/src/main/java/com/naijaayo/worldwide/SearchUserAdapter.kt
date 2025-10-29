package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchUserAdapter(
    private var users: List<User>,
    private val onRequestClick: (User) -> Unit
) : RecyclerView.Adapter<SearchUserAdapter.SearchUserViewHolder>() {

    class SearchUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: ImageView = view.findViewById(R.id.userAvatar)
        val userName: TextView = view.findViewById(R.id.userName)
        val userEmail: TextView = view.findViewById(R.id.userEmail)
        val requestButton: Button = view.findViewById(R.id.requestButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user, parent, false)
        return SearchUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchUserViewHolder, position: Int) {
        val user = users[position]

        // Set avatar
        val avatarRes = getAvatarResource(user.avatarId)
        holder.userAvatar.setImageResource(avatarRes)

        // Set user info
        holder.userName.text = user.username
        holder.userEmail.text = user.email

        // Set request button click listener
        holder.requestButton.setOnClickListener {
            onRequestClick(user)
            // Update button text to show request sent
            holder.requestButton.text = "Sent"
            holder.requestButton.isEnabled = false
        }
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
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