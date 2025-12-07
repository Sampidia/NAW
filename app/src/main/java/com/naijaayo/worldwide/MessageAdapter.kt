package com.naijaayo.worldwide

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.naijaayo.worldwide.model.Message

class MessageAdapter(
    private var messages: List<Message> = emptyList(),
    private var onJoinClick: ((Message) -> Unit)? = null
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val currentUserId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    companion object {
        private const val VIEW_TYPE_TEXT = 0
        private const val VIEW_TYPE_GAME_INVITE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isGameInvite) VIEW_TYPE_GAME_INVITE else VIEW_TYPE_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position], currentUserId, onJoinClick)
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun setOnJoinClickListener(listener: (Message) -> Unit) {
        onJoinClick = listener
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sentMessageText: TextView = itemView.findViewById(R.id.sentMessageText)
        private val receivedMessageText: TextView = itemView.findViewById(R.id.receivedMessageText)
        private val gameInviteCard: CardView = itemView.findViewById(R.id.gameInviteCard)
        private val inviteText: TextView = itemView.findViewById(R.id.inviteText)
        private val joinButton: Button = itemView.findViewById(R.id.joinButton)

        fun bind(message: Message, currentUserId: String, onJoinClick: ((Message) -> Unit)?) {
            val isSent = message.senderId == currentUserId

            // Hide all views initially
            sentMessageText.visibility = View.GONE
            receivedMessageText.visibility = View.GONE
            gameInviteCard.visibility = View.GONE

            if (message.isGameInvite) {
                // Show game invite card
                gameInviteCard.visibility = View.VISIBLE
                
                // Format level text
                val levelText = when (message.gameLevel) {
                    "EASY" -> "easy"
                    "HARD" -> "hard"
                    else -> "medium"
                }

                // Set invite text
                inviteText.text = "${message.inviterUsername} invites you for a $levelText game:"

                // Set join button click listener (only for received invites)
                if (!isSent && onJoinClick != null) {
                    joinButton.visibility = View.VISIBLE
                    joinButton.setOnClickListener {
                        onJoinClick.invoke(message)
                    }
                } else {
                    // For sent invites, show as "Invite Sent" or hide join button
                    if (isSent) {
                        joinButton.visibility = View.GONE
                    }
                }
                
                // Align card based on sender
                val layoutParams = gameInviteCard.layoutParams as? android.widget.FrameLayout.LayoutParams
                layoutParams?.gravity = if (isSent) android.view.Gravity.END else android.view.Gravity.START
                gameInviteCard.layoutParams = layoutParams
                
            } else {
                // Regular text message
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
}