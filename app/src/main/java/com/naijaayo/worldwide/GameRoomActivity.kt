package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.network.Game
import com.naijaayo.worldwide.network.Player
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import kotlinx.coroutines.launch

class GameRoomActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchJoinButton: Button
    private lateinit var roomsRecyclerView: RecyclerView
    private lateinit var createRoomButton: Button
    private lateinit var currentUserAvatar: ImageView
    private lateinit var loadingProgressBar: ProgressBar

    private lateinit var roomAdapter: RoomAdapter
    private var roomsListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_game_room)

        // Initialize AvatarPreferenceManager before using it
        AvatarPreferenceManager.initialize(this)

        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadCurrentUserAvatar()
    }

    override fun onResume() {
        super.onResume()
        listenForRooms()
    }

    override fun onPause() {
        super.onPause()
        roomsListener?.let { FirebaseManager.removeListener(it) }
    }

    private fun initializeViews() {
        searchInput = findViewById(R.id.searchRoomInput)
        searchJoinButton = findViewById(R.id.searchJoinButton)
        roomsRecyclerView = findViewById(R.id.roomsRecyclerView)
        createRoomButton = findViewById(R.id.createRoomButton)
        currentUserAvatar = findViewById(R.id.currentUserAvatar)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
    }

    private fun setupRecyclerView() {
        roomAdapter = RoomAdapter(emptyList()) { game ->
            joinRoom(game.roomId ?: "")
        }
        roomsRecyclerView.layoutManager = LinearLayoutManager(this)
        roomsRecyclerView.adapter = roomAdapter
    }

    private fun setupListeners() {
        searchJoinButton.setOnClickListener {
            val roomId = searchInput.text.toString().trim().uppercase()
            if (roomId.isNotEmpty()) {
                joinRoom(roomId)
            } else {
                Toast.makeText(this, "Please enter a Room ID", Toast.LENGTH_SHORT).show()
            }
        }

        createRoomButton.setOnClickListener {
            createRoom()
        }
    }

    private fun loadCurrentUserAvatar() {
        val avatarId = AvatarPreferenceManager.getUserAvatar()
        currentUserAvatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(avatarId))
    }

    private fun listenForRooms() {
        roomsListener = FirebaseManager.listenForPublicRooms { rooms ->
            // Show all rooms as requested by user
            roomAdapter.updateRooms(rooms)
        }
    }

    private fun createRoom() {
        loadingProgressBar.visibility = View.VISIBLE
        createRoomButton.isEnabled = false

        lifecycleScope.launch {
            val user = FirebaseManager.auth.currentUser
            if (user != null) {
                // Fetch latest profile data to ensure name/avatar are correct
                val profile = FirebaseManager.getUserProfile(user.uid)
                val avatarId = profile?.get("avatarId") as? String ?: AvatarPreferenceManager.getUserAvatar()
                val displayName = profile?.get("displayName") as? String ?: "Player" // Fallback

                val player = Player(user.uid, displayName, avatarId)
                
                // Generate 6-7 digit room code
                val roomCode = generateRoomCode()
                val roomId = FirebaseManager.createPrivateRoom(player, roomCode)
                
                navigateToWaitingRoom(roomId, roomCode)
            } else {
                Toast.makeText(this@GameRoomActivity, "You must be logged in.", Toast.LENGTH_SHORT).show()
            }
            loadingProgressBar.visibility = View.GONE
            createRoomButton.isEnabled = true
        }
    }

    private fun generateRoomCode(): String {
        // Generate random 6-7 digit code
        val codeLength = (6..7).random()
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..codeLength).map { chars.random() }.joinToString("")
    }

    private fun joinRoom(roomId: String) {
        loadingProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val user = FirebaseManager.auth.currentUser
            if (user != null) {
                 // Fetch latest profile data
                val profile = FirebaseManager.getUserProfile(user.uid)
                val avatarId = profile?.get("avatarId") as? String ?: AvatarPreferenceManager.getUserAvatar()
                val displayName = profile?.get("displayName") as? String ?: "Player"

                val player = Player(user.uid, displayName, avatarId)
                val success = FirebaseManager.joinPrivateRoom(roomId, player)
                
                if (success) {
                    // When joining, use roomId as the code (they should be the same for private rooms)
                    navigateToWaitingRoom(roomId, roomId)
                } else {
                    Toast.makeText(this@GameRoomActivity, "Cannot join room. It may be full or started.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@GameRoomActivity, "You must be logged in.", Toast.LENGTH_SHORT).show()
            }
            loadingProgressBar.visibility = View.GONE
        }
    }

    private fun navigateToWaitingRoom(roomId: String, roomCode: String) {
        val intent = Intent(this, WaitingRoomActivity::class.java)
        intent.putExtra("ROOM_ID", roomId)
        intent.putExtra("ROOM_CODE", roomCode)
        startActivity(intent)
    }
}
