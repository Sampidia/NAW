package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private var messages: List<Message>,
    private val onJoinGameClick: (GameInvitation) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val messageTime: TextView = view.findViewById(R.id.messageTime)
        val joinGameButton: Button = view.findViewById(R.id.joinGameButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        // Set message content
        holder.messageText.text = message.content

        // Set timestamp
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.messageTime.text = sdf.format(Date(message.timestamp))

        // Handle game invitations
        if (message.type == MessageType.GAME_INVITATION && message.gameInvitation != null) {
            holder.joinGameButton.visibility = View.VISIBLE
            holder.joinGameButton.setOnClickListener {
                val gameInvitation = message.gameInvitation
                if (gameInvitation != null) {
                    onJoinGameClick(gameInvitation)
                }
            }
        } else {
            holder.joinGameButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages.sortedBy { it.timestamp }
        notifyDataSetChanged()
    }

    fun addMessage(message: Message) {
        messages = messages + message
        messages = messages.sortedBy { it.timestamp }
        notifyDataSetChanged()
    }
}