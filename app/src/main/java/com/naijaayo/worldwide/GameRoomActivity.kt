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
import com.naijaayo.worldwide.network.NetworkGame
import com.naijaayo.worldwide.network.Player
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.bumptech.glide.Glide

class GameRoomActivity : AppCompatActivity() {

    private lateinit var searchInput: EditText
    private lateinit var searchJoinButton: Button
    private lateinit var roomsRecyclerView: RecyclerView
    private lateinit var createRoomButton: Button
    private lateinit var resumeGameButton: Button
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
        resumeGameButton = findViewById(R.id.resumeGameButton)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
    }

    private fun setupRecyclerView() {
        roomAdapter = RoomAdapter(emptyList()) { game ->
            // Show dialog instead of joining directly
            showJoinRoomDialog()
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

        resumeGameButton.setOnClickListener {
            startActivity(Intent(this, ResumeGameActivity::class.java))
        }
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
        
        val appLogo = dialogView.findViewById<ImageView>(R.id.appLogo)
        Glide.with(this).load(R.raw.logo_animate).into(appLogo)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
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
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_rules, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val rulesContent = dialogView.findViewById<android.widget.TextView>(R.id.rulesContent)
        val rulesText = "Naija Ayo is a traditional African board game.<br><br>" +
                "<b>Objective:</b> Capture more seeds than your opponent.<br><br>" +
                "<b>Levels:</b><br>" +
                "- Easy: Capture 2 or 3 seeds<br>" +
                "- Medium: Capture 3 seeds (standard)<br>" +
                "- Hard: Capture 4 seeds<br><br>" +
                "Sow seeds counterclockwise. Capture opponent pits that match the level's seed count after sowing if a transition occurs."
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            rulesContent.text = android.text.Html.fromHtml(rulesText, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            rulesContent.text = android.text.Html.fromHtml(rulesText)
        }

        dialogView.findViewById<android.widget.Button>(R.id.okButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
                val roomId = FirebaseManager.createRoom(player, roomCode, level)
                
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

    private fun showJoinRoomDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_room, null)
        val roomIdInput = dialogView.findViewById<EditText>(R.id.roomIdInput)
        val joinButton = dialogView.findViewById<Button>(R.id.joinRoomButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        joinButton.setOnClickListener {
            val roomCode = roomIdInput.text.toString().trim().uppercase()
            if (roomCode.isNotEmpty()) {
                dialog.dismiss()
                joinRoom(roomCode)
            } else {
                Toast.makeText(this, "Please enter a Room ID", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun joinRoom(roomCode: String) {
        loadingProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val user = FirebaseManager.auth.currentUser
            if (user != null) {
                // First, find the room by code
                val roomId = FirebaseManager.findRoomByCode(roomCode.uppercase())
                
                if (roomId == null) {
                    Toast.makeText(this@GameRoomActivity, "NetworkGame Room Code Invalid", Toast.LENGTH_SHORT).show()
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
                    val game = roomSnapshot.getValue(NetworkGame::class.java)
                    
                    if (game != null && game.players.size >= 2) {
                        Toast.makeText(this@GameRoomActivity, "NetworkGame Room Full", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@GameRoomActivity, "Cannot join room. NetworkGame may have already started.", Toast.LENGTH_SHORT).show()
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
