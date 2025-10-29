package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.naijaayo.worldwide.game.GameViewModel
import com.naijaayo.worldwide.theme.NigerianThemeManager

class WaitingRoomActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    private lateinit var player1Avatar: ImageView
    private lateinit var player2Avatar: ImageView
    private lateinit var player1Name: TextView
    private lateinit var player2Name: TextView
    private lateinit var roomTitleTextView: TextView
    private lateinit var difficultyTextView: TextView
    private lateinit var startGameButton: Button
    private lateinit var backToLobbyButton: Button

    private var currentRoom: Room? = null
    private var currentUserId = "user1" // TODO: Get from user session
    private var currentUsername = "Player 1" // TODO: Get from user session
    private var currentAvatarId = com.naijaayo.worldwide.theme.AvatarPreferenceManager.getUserAvatar() // Get from user preferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Initialize avatar preference manager
        com.naijaayo.worldwide.theme.AvatarPreferenceManager.initialize(this)

        // Hide action bar and status bar for immersive experience
        supportActionBar?.hide()
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN, android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_waiting_room)

        // Get room data from intent
        val roomId = intent.getStringExtra("roomId")
        val hostUsername = intent.getStringExtra("hostUsername")
        val difficultyString = intent.getStringExtra("difficulty")
        val isHost = intent.getBooleanExtra("isHost", false)

        if (roomId != null && hostUsername != null && difficultyString != null) {
            // Create room object from intent data
            val difficulty = when (difficultyString) {
                "EASY" -> GameLevel.EASY
                "MEDIUM" -> GameLevel.MEDIUM
                "HARD" -> GameLevel.HARD
                else -> GameLevel.MEDIUM
            }

            currentRoom = Room(
                roomId = roomId,
                hostUid = currentUserId,
                hostUsername = hostUsername,
                hostAvatarId = currentAvatarId,
                difficulty = difficulty,
                type = "public"
            )
        }

        initializeViews()
        setupRoomInfo()
        setupNavigation()
        observeRoomState()
    }

    private fun initializeViews() {
        player1Avatar = findViewById(R.id.player1Avatar)
        player2Avatar = findViewById(R.id.player2Avatar)
        player1Name = findViewById(R.id.player1Name)
        player2Name = findViewById(R.id.player2Name)
        roomTitleTextView = findViewById(R.id.roomTitleTextView)
        difficultyTextView = findViewById(R.id.difficultyTextView)
        startGameButton = findViewById(R.id.startGameButton)
        backToLobbyButton = findViewById(R.id.backToLobbyButton)
    }

    private fun setupRoomInfo() {
        currentRoom?.let { room ->
            roomTitleTextView.text = "Room: ${room.roomId}"

            val difficultyText = when (room.difficulty) {
                GameLevel.EASY -> "Easy"
                GameLevel.MEDIUM -> "Medium"
                GameLevel.HARD -> "Hard"
            }
            difficultyTextView.text = "Difficulty: $difficultyText"

            // Set player 1 (host/current user) info
            player1Name.text = room.hostUsername
            val player1AvatarRes = getAvatarResource(room.hostAvatarId)
            player1Avatar.setImageResource(player1AvatarRes)

            // Update player count and enable/disable start button
            updatePlayerStatus(room)
        }
    }

    private fun updatePlayerStatus(room: Room) {
        when {
            room.players.size >= 2 -> {
                // Room is full, show both players
                player2Name.text = "Opponent" // TODO: Get actual opponent name
                startGameButton.isEnabled = room.players.contains(currentUserId)
                startGameButton.alpha = if (startGameButton.isEnabled) 1.0f else 0.5f
            }
            room.players.size == 1 -> {
                // Waiting for opponent
                player2Name.text = "Waiting for opponent..."
                startGameButton.isEnabled = false
                startGameButton.alpha = 0.5f
            }
            else -> {
                // Empty room (shouldn't happen)
                player2Name.text = "Waiting for players..."
                startGameButton.isEnabled = false
                startGameButton.alpha = 0.5f
            }
        }
    }

    private fun setupNavigation() {
        startGameButton.setOnClickListener {
            currentRoom?.let { room ->
                // Navigate to game with room data (pass minimal data to avoid serialization issues)
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("isSinglePlayer", false)
                    putExtra("roomId", room.roomId)
                    putExtra("difficulty", room.difficulty.name)
                }
                startActivity(intent)
                finish()
            }
        }

        backToLobbyButton.setOnClickListener {
            // Navigate back to lobby
            val intent = Intent(this, MultiplayerLobbyActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun observeRoomState() {
        // TODO: Observe room updates from server via ViewModel
        // For now, we'll rely on the initial room data
    }

    private fun getAvatarResource(avatarId: String): Int {
        return when (avatarId) {
            "ayo" -> R.drawable.char_ayo_portrait
            "ada" -> R.drawable.char_ada_portrait
            "fatima" -> R.drawable.char_fatima_portrait
            "ai" -> R.drawable.char_ai_portrait
            else -> R.drawable.char_ayo_portrait // Default fallback
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme when activity becomes visible
        NigerianThemeManager.applyThemeToActivity(this)
        // Resume background music
        com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
    }
}