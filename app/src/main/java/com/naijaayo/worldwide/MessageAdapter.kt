package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.naijaayo.worldwide.model.Message

class MessageAdapter(
    private var messages: List<Message> = emptyList()
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], currentUserId)
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sentMessageText: TextView = itemView.findViewById(R.id.sentMessageText)
        private val receivedMessageText: TextView = itemView.findViewById(R.id.receivedMessageText)

        fun bind(message: Message, currentUserId: String) {
            val isSent = message.senderId == currentUserId

            if (isSent) {
                sentMessageText.visibility = View.VISIBLE
                receivedMessageText.visibility = View.GONE
                sentMessageText.text = message.text
            } else {
                sentMessageText.visibility = View.GONE
                receivedMessageText.visibility = View.VISIBLE
                receivedMessageText.text = message.text
            }
        }
    }
}