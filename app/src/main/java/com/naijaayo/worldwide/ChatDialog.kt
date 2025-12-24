package com.naijaayo.worldwide

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.model.Friend
import com.naijaayo.worldwide.model.Message
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import kotlinx.coroutines.launch

class ChatDialog(private val friend: Friend) : DialogFragment() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var closeButton: ImageView
    private lateinit var currentUserAvatar: ImageView
    private lateinit var currentUserName: TextView
    private lateinit var friendAvatar: ImageView
    private lateinit var friendName: TextView
    private lateinit var messageAdapter: MessageAdapter
    private var messagesListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView)
        messageEditText = view.findViewById(R.id.messageEditText)
        sendButton = view.findViewById(R.id.sendButton)
        closeButton = view.findViewById(R.id.closeButton)
        currentUserAvatar = view.findViewById(R.id.currentUserAvatar)
        currentUserName = view.findViewById(R.id.currentUserName)
        friendAvatar = view.findViewById(R.id.friendAvatar)
        friendName = view.findViewById(R.id.friendName)

        // Setup header with user info
        setupHeader()

        // Setup RecyclerView with JOIN button handler
        messageAdapter = MessageAdapter()
        messageAdapter.setOnJoinClickListener { message ->
            handleJoinGame(message)
        }
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true // Start from bottom
            }
            adapter = messageAdapter
        }

        // Close button
        closeButton.setOnClickListener {
            dismiss()
        }

        // Send button
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Start listening for messages
        startMessagesListener()
    }

    private fun setupHeader() {
        // Set friend info
        friendName.text = friend.name
        friendAvatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(friend.avatar))

        // Get current user info from Firebase
        lifecycleScope.launch {
            val currentUid = FirebaseManager.auth.currentUser?.uid
            if (currentUid != null) {
                val profile = FirebaseManager.getUserProfile(currentUid)
                if (profile != null) {
                    currentUserName.text = profile["username"] as? String ?: "You"
                    val avatarId = profile["avatarId"] as? String ?: "ayo"
                    currentUserAvatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(avatarId))
                }
            }
        }
    }

    private fun startMessagesListener() {
        messagesListener = FirebaseManager.listenForMessages(friend.id) { messages ->
            messageAdapter.updateMessages(messages)
            // Scroll to bottom when new messages arrive
            if (messages.isNotEmpty()) {
                messagesRecyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun sendMessage() {
        val text = messageEditText.text.toString().trim()
        if (text.isEmpty()) return

        // Clear input immediately for better UX
        messageEditText.text.clear()

        lifecycleScope.launch {
            val success = FirebaseManager.sendMessage(friend.id, text)
            if (!success) {
                // Optionally show error toast
                android.util.Log.e("ChatDialog", "Failed to send message")
            }
        }
    }

    private fun handleJoinGame(message: Message) {
        if (!message.isGameInvite || message.roomId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid game invite", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // Get current user info
                val currentUid = FirebaseManager.auth.currentUser?.uid
                if (currentUid == null) {
                    Toast.makeText(requireContext(), "Please login first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val profile = FirebaseManager.getUserProfile(currentUid)
                val displayName = profile?.get("username") as? String ?: "Player"
                val avatarId = profile?.get("avatarId") as? String ?: "ayo"

                // Create player object
                val player = PlayNowPlayer(
                    uid = currentUid,
                    displayName = displayName,
                    avatarId = avatarId
                )

                // Try to join the room
                val success = FirebaseManager.joinPrivateRoom(message.roomId, player)

                if (success) {
                    Toast.makeText(requireContext(), "Joined game!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to WaitingRoomActivity
                    val intent = Intent(requireContext(), WaitingRoomActivity::class.java).apply {
                        putExtra("ROOM_ID", message.roomId)
                        putExtra("ROOM_CODE", message.roomCode)
                    }
                    startActivity(intent)
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Could not join game. Room may be full or closed.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatDialog", "Error joining game", e)
                Toast.makeText(requireContext(), "Error joining game", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up listener
        messagesListener?.let {
            FirebaseManager.removeMessagesListener(it, friend.id)
        }
        messagesListener = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Set layout to match parent to fill screen
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            // Set the background to be transparent to see the content behind the dialog.
            setBackgroundDrawableResource(android.R.color.transparent)

            // Handle fullscreen across different API levels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
            }
        }
    }
}
