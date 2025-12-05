package com.naijaayo.worldwide

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.model.Friend
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Light_NoTitleBar_Fullscreen)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        return dialog
    }

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

        // Setup RecyclerView
        messageAdapter = MessageAdapter()
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
        // Make dialog full screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
