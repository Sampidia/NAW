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
import kotlinx.coroutines.tasks.await

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
        // Show level selection dialog
        val dialogView = layoutInflater.inflate(R.layout.dialog_level_selection, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Game Level")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<Button>(R.id.easyButton).setOnClickListener {
            dialog.dismiss()
            createRoomWithLevel("EASY")
        }

        dialogView.findViewById<Button>(R.id.mediumButton).setOnClickListener {
            dialog.dismiss()
            createRoomWithLevel("MEDIUM")
        }

        dialogView.findViewById<Button>(R.id.hardButton).setOnClickListener {
            dialog.dismiss()
            createRoomWithLevel("HARD")
        }

        dialogView.findViewById<Button>(R.id.rulesButton).setOnClickListener {
            showRules()
        }

        dialog.show()
    }

    private fun showRules() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Game Rules")
            .setMessage("Naija Ayo is a traditional African board game.\n\n" +
                "Objective: Capture more seeds than your opponent.\n\n" +
                "Levels:\n" +
                "- Easy: Capture 2 or 3 seeds\n" +
                "- Medium: Capture 3 seeds (standard)\n" +
                "- Hard: Capture 4 seeds\n\n" +
                "Sow seeds counterclockwise. Capture opponent pits that match the level's seed count after sowing if a transition occurs.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun createRoomWithLevel(level: String) {
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
                val roomId = FirebaseManager.createPrivateRoom(player, roomCode, level)
                
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

    private fun joinRoom(roomCode: String) {
        loadingProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val user = FirebaseManager.auth.currentUser
            if (user != null) {
                // First, find the room by code
                val roomId = FirebaseManager.findRoomByCode(roomCode.uppercase())
                
                if (roomId == null) {
                    Toast.makeText(this@GameRoomActivity, "Game Room Code Invalid", Toast.LENGTH_SHORT).show()
                    loadingProgressBar.visibility = View.GONE
                    return@launch
                }
                
                // Fetch latest profile data
                val profile = FirebaseManager.getUserProfile(user.uid)
                val avatarId = profile?.get("avatarId") as? String ?: AvatarPreferenceManager.getUserAvatar()
                val displayName = profile?.get("displayName") as? String ?: "Player"

                val player = Player(user.uid, displayName, avatarId)
                val success = FirebaseManager.joinPrivateRoom(roomId, player)
                
                if (success) {
                    navigateToWaitingRoom(roomId, roomCode.uppercase())
                } else {
                    // Check if room is full or already started
                    val roomSnapshot = FirebaseManager.gamesRef.child(roomId).get().await()
                    val game = roomSnapshot.getValue(Game::class.java)
                    
                    if (game != null && game.players.size >= 2) {
                        Toast.makeText(this@GameRoomActivity, "Game Room Full", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@GameRoomActivity, "Cannot join room. Game may have already started.", Toast.LENGTH_SHORT).show()
                    }
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
