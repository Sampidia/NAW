package com.naijaayo.worldwide

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.network.WebSocketManager
import kotlinx.coroutines.launch

class ChatDialog(private val friend: Friend) : DialogFragment() {

    private val friendsViewModel: FriendsViewModel by activityViewModels()
    private lateinit var messagesAdapter: MessageAdapter
    private val webSocketManager = WebSocketManager()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_chat)

        setupViews(dialog)
        observeViewModel()
        loadMessages()
        connectWebSocket()

        return dialog
    }

    private fun setupViews(dialog: Dialog) {
        // Set friend name
        val friendNameText = dialog.findViewById<TextView>(R.id.friendNameText)
        friendNameText.text = friend.friendUsername

        // Setup messages recycler view
        val messagesRecyclerView = dialog.findViewById<RecyclerView>(R.id.messagesRecyclerView)
        messagesAdapter = MessageAdapter(emptyList()) { gameInvitation ->
            // Handle game invitation join
            joinGame(gameInvitation)
        }
        messagesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        messagesRecyclerView.adapter = messagesAdapter

        // Setup send message
        val messageEditText = dialog.findViewById<EditText>(R.id.messageEditText)
        val sendButton = dialog.findViewById<Button>(R.id.sendButton)
        sendButton.setOnClickListener {
            val messageText = messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageEditText.text.clear()
            }
        }

        // Setup invite to game
        val inviteButton = dialog.findViewById<Button>(R.id.inviteButton)
        inviteButton.setOnClickListener {
            sendGameInvitation()
        }

        // Setup close button
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)
        closeButton.setOnClickListener {
            disconnectWebSocket()
            dismiss()
        }
    }

    private fun observeViewModel() {
        friendsViewModel.messages.observe(this, Observer { messages ->
            messagesAdapter.updateMessages(messages)
        })
    }

    private fun loadMessages() {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            friendsViewModel.loadMessages(it.id, friend.friendId)
        }
    }

    private fun sendMessage(messageText: String) {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            val message = Message(
                id = generateId(),
                fromUserId = it.id,
                toUserId = friend.friendId,
                fromUsername = it.username,
                content = messageText
            )
            friendsViewModel.sendMessage(message)
        }
    }

    private fun sendGameInvitation() {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            val gameInvitation = GameInvitation(
                roomId = generateRoomId(),
                hostUsername = it.username,
                gameType = "multiplayer",
                difficulty = "MEDIUM"
            )
            val message = Message(
                id = generateId(),
                fromUserId = it.id,
                toUserId = friend.friendId,
                fromUsername = it.username,
                content = "I've invited you to play a game!",
                type = MessageType.GAME_INVITATION,
                gameInvitation = gameInvitation
            )
            friendsViewModel.sendMessage(message)
        }
    }

    private fun joinGame(gameInvitation: GameInvitation) {
        // Navigate to the specific room
        val intent = Intent(requireContext(), WaitingRoomActivity::class.java).apply {
            putExtra("roomId", gameInvitation.roomId)
            putExtra("hostUsername", gameInvitation.hostUsername)
            putExtra("difficulty", gameInvitation.difficulty)
            putExtra("isHost", false)
        }
        startActivity(intent)
        dismiss()
    }

    private fun connectWebSocket() {
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        currentUser?.let {
            webSocketManager.connect(it.id)

            // Listen for incoming messages
            lifecycleScope.launch {
                webSocketManager.messageFlow.collect { message ->
                    // Only show messages from the current chat friend
                    if (message.fromUserId == friend.friendId || message.toUserId == friend.friendId) {
                        // Add message to adapter
                        messagesAdapter.addMessage(message)
                    }
                }
            }
        }
    }

    private fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    private fun generateId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    private fun generateRoomId(): String {
        return "room_" + java.util.UUID.randomUUID().toString().substring(0, 8)
    }
}