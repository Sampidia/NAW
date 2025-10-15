package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.naijaayo.worldwide.game.GameViewModel
import com.naijaayo.worldwide.game.SinglePlayerGameViewModel
import com.naijaayo.worldwide.game.GameStats

class MainActivity : AppCompatActivity() {

    // Game mode detection
    private var isSinglePlayer: Boolean = true // Default to single-player for now

    // ViewModels for different game modes
    private val multiplayerViewModel: GameViewModel by viewModels()
    private val singlePlayerViewModel: SinglePlayerGameViewModel by viewModels()

    // UI components
    private lateinit var player1Avatar: ImageView
    private lateinit var player2Avatar: ImageView
    private lateinit var player1Score: TextView
    private lateinit var player2Score: TextView
    private lateinit var currentPlayer: TextView
    private lateinit var gameMessage: TextView
    private lateinit var saveButton: ImageButton
    private lateinit var menuButton: ImageButton
    private val pitTextViews = mutableListOf<TextView>()

    // Avatar management (now dynamic from GameViewModel)
    private var currentPlayer1AvatarId: String = "ayo" // Will be updated from GameViewModel
    private var currentPlayer2AvatarId: String = "ai"   // Default AI avatar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Hide action bar to remove "Naija Ayo Worldwide" title
        supportActionBar?.hide()

        setContentView(R.layout.activity_main)

        // Initialize UI components
        initializeViews()

        // Setup menu functionality
        setupMenu()

        // Detect game mode from intent or default to single-player
        isSinglePlayer = intent.getBooleanExtra("isSinglePlayer", true)

        if (isSinglePlayer) {
            setupSinglePlayerMode()
        } else {
            setupMultiplayerMode()
        }

        // Setup avatar observation and initial update
        setupAvatarObservation()
    }

    private fun initializeViews() {
        // Header components
        player1Avatar = findViewById(R.id.player1Avatar)
        player2Avatar = findViewById(R.id.player2Avatar)
        player1Score = findViewById(R.id.player1Score)
        player2Score = findViewById(R.id.player2Score)
        saveButton = findViewById(R.id.saveButton)
        menuButton = findViewById(R.id.menuButton)

        // Game components
        currentPlayer = findViewById(R.id.currentPlayer)
        gameMessage = findViewById(R.id.gameMessage)

        // Initialize pits
        initializePits()
    }

    private fun setupMenu() {
        // Save button (placeholder for future feature)
        saveButton.setOnClickListener {
            Toast.makeText(this, "Save feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Hamburger menu button
        menuButton.setOnClickListener { view ->
            showPopupMenu(view)
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.game_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_new_game -> {
                    if (isSinglePlayer) {
                        singlePlayerViewModel.startNewGame()
                    } else {
                        finish() // Return to multiplayer lobby
                    }
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.menu_multiplayer -> {
                    startActivity(Intent(this, MultiplayerLobbyActivity::class.java))
                    true
                }
                R.id.menu_friends -> {
                    Toast.makeText(this, "Friends feature coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    private fun updateAvatars() {
        // Set player 1 avatar (from profile)
        val player1AvatarRes = getAvatarResource(currentPlayer1AvatarId)
        player1Avatar.setImageResource(player1AvatarRes)

        // Set player 2 avatar (AI in single-player, opponent in multiplayer)
        val player2AvatarRes = getAvatarResource(currentPlayer2AvatarId)
        player2Avatar.setImageResource(player2AvatarRes)
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

    private fun setupAvatarObservation() {
        // Observe current user avatar changes from GameViewModel
        multiplayerViewModel.currentUserAvatar.observe(this) { avatarId ->
            currentPlayer1AvatarId = avatarId ?: "ayo"
            updateAvatars()
        }

        // Fetch initial avatar
        multiplayerViewModel.getCurrentUserAvatar()
    }

    /**
     * Refresh user avatar when returning from profile screen
     */
    fun refreshUserAvatar() {
        multiplayerViewModel.refreshCurrentUserAvatar()
    }

    private fun initializePits() {
        for (i in 0 until 12) {
            val pitId = resources.getIdentifier("pit_$i", "id", packageName)
            val pitTextView = findViewById<TextView>(pitId)
            pitTextViews.add(pitTextView)
        }
    }

    /**
     * Sets up single-player mode with local game engine
     */
    private fun setupSinglePlayerMode() {
        // Observe single-player game state
        singlePlayerViewModel.gameState.observe(this) { gameState ->
            updateUI(gameState)
        }

        // Observe game messages
        singlePlayerViewModel.gameMessage.observe(this) { message ->
            gameMessage.text = message
            gameMessage.visibility = View.VISIBLE
        }

        // Observe processing state (disable menu during AI moves)
        singlePlayerViewModel.isProcessingMove.observe(this) { isProcessing ->
            menuButton.isEnabled = !isProcessing
            saveButton.isEnabled = !isProcessing
        }

        // Update game mode indicator
        gameMessage.text = "Single-player mode! You're Player 1 vs AI."
    }

    /**
     * Sets up multiplayer mode with network game engine
     */
    private fun setupMultiplayerMode() {
        // Observe multiplayer game state
        multiplayerViewModel.gameState.observe(this) { gameState ->
            updateUI(gameState)
        }

        // Update game mode indicator for multiplayer
        gameMessage.text = "Multiplayer mode! Waiting for opponent..."
        gameMessage.visibility = View.VISIBLE
    }

    /**
     * Sets up click listeners for pits based on game mode
     */
    private fun setupClickListeners(gameState: GameState) {
        if (isSinglePlayer) {
            setupSinglePlayerClickListeners(gameState)
        } else {
            setupMultiplayerClickListeners(gameState)
        }
    }

    /**
     * Sets up click listeners for single-player mode
     */
    private fun setupSinglePlayerClickListeners(gameState: GameState) {
        for (i in 0..11) {
            if (i < 6) {
                // Player 1's pits (bottom row) - always clickable when it's player's turn
                if (gameState.currentPlayer == 1 && gameState.pits[i] > 0) {
                    pitTextViews[i].setOnClickListener {
                        singlePlayerViewModel.makePlayerMove(i)
                    }
                } else {
                    pitTextViews[i].setOnClickListener(null)
                }
            } else {
                // Player 2's pits (top row) - never clickable in single-player
                pitTextViews[i].setOnClickListener(null)
            }
        }
    }

    /**
     * Sets up click listeners for multiplayer mode
     */
    private fun setupMultiplayerClickListeners(gameState: GameState) {
        val isPlayer1Turn = gameState.currentPlayer == 1

        for (i in 0..5) { // Player 1's pits
            if (isPlayer1Turn && gameState.pits[i] > 0) {
                pitTextViews[i].setOnClickListener {
                    multiplayerViewModel.playMove(i)
                }
            } else {
                pitTextViews[i].setOnClickListener(null)
            }
        }

        // Player 2's pits are controlled by remote player
        for (i in 6..11) {
            pitTextViews[i].setOnClickListener(null)
        }
    }

    /**
     * Updates the UI based on current game state
     */
    private fun updateUI(gameState: GameState) {
        if (gameState.gameOver) {
            showGameOverDialog(gameState)
            return
        }

        // Update scores with correct format
        player1Score.text = "Player 1: ${gameState.player1Score}"
        player2Score.text = if (isSinglePlayer) "${gameState.player2Score} :AI" else "Opponent: ${gameState.player2Score}"

        // Update current player indicator
        val currentPlayerText = if (isSinglePlayer) {
            if (gameState.currentPlayer == 1) "Your Turn" else "AI Thinking..."
        } else {
            "Current Player: ${gameState.currentPlayer}"
        }
        currentPlayer.text = currentPlayerText

        // Update pit displays
        val activePitDrawable = ContextCompat.getDrawable(this, R.drawable.pit_background_active)
        val inactivePitDrawable = ContextCompat.getDrawable(this, R.drawable.pit_background)

        for ((index, pit) in pitTextViews.withIndex()) {
            pit.text = gameState.pits[index].toString()

            // Highlight valid moves
            val isPlayer1Pit = index < 6
            val shouldHighlight = if (isSinglePlayer) {
                (gameState.currentPlayer == 1 && isPlayer1Pit) ||
                (gameState.currentPlayer == 2 && !isPlayer1Pit)
            } else {
                (gameState.currentPlayer == 1 && isPlayer1Pit) ||
                (gameState.currentPlayer == 2 && !isPlayer1Pit)
            }

            if (shouldHighlight && gameState.pits[index] > 0) {
                pit.background = activePitDrawable
            } else {
                pit.background = inactivePitDrawable
            }
        }

        setupClickListeners(gameState)
    }

    /**
     * Shows game over dialog
     */
    private fun showGameOverDialog(gameState: GameState) {
        val message = if (isSinglePlayer) {
            when (gameState.winner) {
                1 -> "🎉 You win!"
                2 -> "🤖 AI wins!"
                else -> "🤝 It's a draw!"
            }
        } else {
            when (gameState.winner) {
                1 -> "You win!"
                2 -> "Opponent wins!"
                else -> "It's a draw!"
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage("$message\n\nFinal Score:\nYou: ${gameState.player1Score}\nOpponent/AI: ${gameState.player2Score}")
            .setPositiveButton("Play Again") { _, _ ->
                if (isSinglePlayer) {
                    singlePlayerViewModel.startNewGame()
                } else {
                    finish() // Return to multiplayer lobby
                }
            }
            .setCancelable(false)
            .show()
    }
}
