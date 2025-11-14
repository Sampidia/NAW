package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.appcompat.widget.TooltipCompat
import com.naijaayo.worldwide.game.GameViewModel
import com.naijaayo.worldwide.game.SinglePlayerGameViewModel
import com.naijaayo.worldwide.game.GameStats
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.ui.VisualSeedManager
import com.naijaayo.worldwide.ui.ImageManager
import com.naijaayo.worldwide.sound.SoundManager
import com.naijaayo.worldwide.sound.SoundEventType
import com.naijaayo.worldwide.sound.SoundPreferencesManager

class MainActivity : AppCompatActivity() {

    // Game mode detection
    private var isSinglePlayer: Boolean = true // Default to single-player for now
    private var gameLevel: com.naijaayo.worldwide.GameLevel = com.naijaayo.worldwide.GameLevel.MEDIUM // Default level

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
    private val pitTextViews = mutableListOf<ImageView>()
    private val pitLabelViews = mutableListOf<TextView>()
    private val pitImageViews = mutableListOf<ImageView>()
    private val pitContainers = mutableListOf<androidx.constraintlayout.widget.ConstraintLayout>()

    // Game board components
    private lateinit var boardBackground: ImageView

    // Game status pop-out components
    private lateinit var gameStatusCard: androidx.cardview.widget.CardView

    // Floating status management
    private var statusCardVisible = false

    // Image management
    private lateinit var imageManager: ImageManager
    private lateinit var visualSeedManager: VisualSeedManager

    // Sound management
    private lateinit var soundManager: SoundManager

    // Avatar management (now dynamic from GameViewModel and Sessions)
    private var currentPlayer1AvatarId: String = com.naijaayo.worldwide.theme.AvatarPreferenceManager.getUserAvatar() // Use user's selected avatar from profile settings
    private var currentPlayer2AvatarId: String = "ai"   // Default AI avatar, or opponent in multiplayer

    // Game state tracking for smart animations
    private var previousGameState: GameState? = null

    // Dynamic status card management
    private lateinit var statusCardContainer: androidx.constraintlayout.widget.ConstraintLayout
    private var statusCardHandler = android.os.Handler()
    private var statusCardRunnable: Runnable? = null
    private val STATUS_DISPLAY_DURATION = 3000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Hide action bar to remove "Naija Ayo Worldwide" title
        supportActionBar?.hide()

        // Enable fullscreen mode for immersive gameplay
        enableFullscreenMode()

        setContentView(R.layout.activity_main)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Initialize avatar preference manager
        com.naijaayo.worldwide.theme.AvatarPreferenceManager.initialize(this)

        // Initialize session manager
        com.naijaayo.worldwide.auth.SessionManager.initialize(this)

        // Initialize board manager
        BoardManager.initialize(this)

        // Initialize UI components FIRST
        initializeViews()

        // THEN initialize image manager and load PNGs
        imageManager = ImageManager(this)
        loadSelectedBoardBackground()

        // Initialize visual seed manager
        visualSeedManager = VisualSeedManager(this)

        // Initialize sound manager
        soundManager = SoundManager(this)
        soundManager.loadSounds()

        // Load and apply sound preferences
        loadSoundPreferences()

        // Initialize and start background music
        android.util.Log.d("MainActivity", "🎵 Initializing BackgroundMusicManager...")
        com.naijaayo.worldwide.sound.BackgroundMusicManager.initialize(this)
        android.util.Log.d("MainActivity", "🎵 Calling startBackgroundMusic()...")
        android.os.Handler().postDelayed({
            com.naijaayo.worldwide.sound.BackgroundMusicManager.startBackgroundMusic()
            android.util.Log.d("MainActivity", "🎵 Background music initialization completed")
        }, 1000) // Delay 1 second to ensure UI is fully loaded

        // Setup menu functionality
        setupMenu()

        // Detect game mode from intent or default to single-player
        isSinglePlayer = intent.getBooleanExtra("isSinglePlayer", true)

        // Get game level from intent (for multiplayer rooms)
        val room = intent.getSerializableExtra("room") as? Room
        if (room != null) {
            gameLevel = room.difficulty
            // Set multiplayer avatars if room has opponent info
            if (room.opponentAvatarId != null) {
                currentPlayer2AvatarId = room.opponentAvatarId!!
            }
            // Apply room settings (creator's preferences) in multiplayer
            room.settings?.let { settings ->
                applyRoomSettings(settings)
            }
        } else {
            val levelName = intent.getStringExtra("level") ?: "MEDIUM"
            gameLevel = try {
                com.naijaayo.worldwide.GameLevel.valueOf(levelName)
            } catch (e: IllegalArgumentException) {
                com.naijaayo.worldwide.GameLevel.MEDIUM
            }
        }

        // Set player 1 avatar from session if multiplayer
        if (isSinglePlayer) {
            val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
            if (currentUser != null) {
                currentPlayer1AvatarId = currentUser.avatarId
            }
        }

        if (isSinglePlayer) {
            setupSinglePlayerMode()
        } else {
            setupMultiplayerMode()
        }

        // Setup avatar observation and initial update
        setupAvatarObservation()

        // Removed problematic test code that triggers animations at startup
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

        // Board background
        boardBackground = findViewById(R.id.boardBackground)

        // Game status pop-out
        gameStatusCard = findViewById(R.id.gameStatusCard)

        // Set background drawable programmatically to avoid CardView conflicts
        gameStatusCard.background = ContextCompat.getDrawable(this, R.drawable.game_status_background)

        // Initialize pits and labels
        initializePits()
    }

    private fun loadSelectedBoardBackground() {
        val activeBoard = BoardManager.getActiveBoard()
        if (activeBoard != null && activeBoard.isAvailable) {
            val imagePath = activeBoard.backgroundImagePath
            imageManager.loadBoardBackground(boardBackground, imagePath)
        } else {
            // Fallback to default
            imageManager.loadBoardBackground(boardBackground)
        }
    }

    private fun setupMenu() {
        // Save button - requires authentication
        saveButton.setOnClickListener {
            saveCurrentGame()
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
                        singlePlayerViewModel.startNewGame(gameLevel)
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
            currentPlayer1AvatarId = avatarId ?: com.naijaayo.worldwide.theme.AvatarPreferenceManager.getUserAvatar()
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

    /**
     * Enables fullscreen mode for immersive gameplay experience
     */
    private fun enableFullscreenMode() {
        // For API 30+ (Android 11+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        // Also hide the action bar
        supportActionBar?.hide()

        // Set the app to stay awake during gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initializePits() {
        // Initialize pit image views for seed display
        for (i in 0 until 12) {
            val pitId = resources.getIdentifier("pitText_$i", "id", packageName)
            try {
                val pitImageView = findViewById<ImageView>(pitId)
                pitTextViews.add(pitImageView)
                // Ensure the view is clickable for TooltipCompat
                pitImageView.isClickable = true
                // Set TooltipCompat for long-press to show seed count
                TooltipCompat.setTooltipText(pitImageView, "Seeds: 0")
            } catch (e: Exception) {
                // Create a placeholder ImageView if cast fails
                val placeholderImageView = ImageView(this)
                placeholderImageView.setImageResource(R.drawable.one_seed) // Default image
                pitTextViews.add(placeholderImageView)
            }
        }

        // Initialize pit label views (below pits)
        for (i in 0 until 12) {
            val pitLabelId = resources.getIdentifier("pitLabel_$i", "id", packageName)
            val pitLabelTextView = findViewById<TextView>(pitLabelId)
            pitLabelViews.add(pitLabelTextView)
        }

        // Initialize pit image views (background images)
        for (i in 0 until 12) {
            val pitImageId = resources.getIdentifier("pitImage_$i", "id", packageName)
            try {
                val pitImageView = findViewById<ImageView>(pitImageId)
                pitImageViews.add(pitImageView)
            } catch (e: Exception) {
                // Some pit images might not exist yet, add null placeholder
                pitImageViews.add(ImageView(this))
            }
        }

        // Initialize pit containers for VisualSeedManager and TooltipCompat
        for (i in 0 until 12) {
            val pitContainerId = resources.getIdentifier("pitContainer_$i", "id", packageName)
            try {
                val pitContainer = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(pitContainerId)
                pitContainers.add(pitContainer)
            } catch (e: Exception) {
                // Some pit containers might not exist yet, add null placeholder
                pitContainers.add(findViewById(R.id.pitContainer_0)) // fallback to first container
            }
        }
    }

    /**
     * Gets the pit container view for the given index
     */
    private fun getPitContainer(index: Int): androidx.constraintlayout.widget.ConstraintLayout? {
        return if (index < pitContainers.size) pitContainers[index] else null
    }

    /**
     * Detects what type of animation should be played for a specific pit
     * Uses game engine capture data for accurate animation and sound effects
     */
    private fun detectAnimationType(pitIndex: Int, currentGameState: GameState): VisualSeedManager.AnimationType {
        android.util.Log.d("AnimationDebug", "🎯 detectAnimationType CALLED for pit $pitIndex")

        // Check if this pit was captured in the last move
        if (currentGameState.lastCapturedPitIndices.contains(pitIndex)) {
            android.util.Log.d("CaptureDebug", "🎯 Pit $pitIndex was CAPTURED! Animation only (sound handled by sequential animation)")
            return VisualSeedManager.AnimationType.CAPTURE
        }

        // Sequential animation now handles ALL sounds, so detectAnimationType only does visual animations
        android.util.Log.d("AnimationDebug", "🎭 Sequential animation handles sounds - detectAnimationType for visuals only")
        return VisualSeedManager.AnimationType.NONE

        return VisualSeedManager.AnimationType.NONE
    }



    /**
     * Manual test method to trigger capture sound and animation
     * This can be called directly for testing purposes
     */
    fun testCaptureSoundAndAnimation() {
        android.util.Log.d("TestCapture", "🧪 Manual capture test triggered!")

        // Create a simple capture scenario
        val testPreviousState = GameState(
            pits = intArrayOf(4, 4, 4, 4, 4, 4, 3, 4, 4, 4, 4, 4), // Pit 6 has 3 seeds
            currentPlayer = 1,
            player1Score = 0,
            player2Score = 0,
            gameOver = false,
            winner = 0
        )

        val testCurrentState = GameState(
            pits = intArrayOf(4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4), // Pit 6 captured (3→0)
            currentPlayer = 1,
            player1Score = 3, // Seeds captured
            player2Score = 0,
            gameOver = false,
            winner = 0
        )

        // Set previous state and trigger UI update
        previousGameState = testPreviousState
        android.util.Log.d("TestCapture", "🔄 Triggering manual UI update for capture test...")
        updateUI(testCurrentState)
    }



    /**
      * Sets up single-player mode with local game engine
      */
    private fun setupSinglePlayerMode() {
        // Start new game with the selected level
        singlePlayerViewModel.startNewGame(gameLevel)

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
                // Player 1's pits (bottom row) - clickable when it's player's turn
                if (gameState.currentPlayer == 1 && gameState.pits[i] > 0) {
                    // Set click listener on both text and image views for better UX
                    val clickListener = View.OnClickListener {
                        executeAnimatedMove(i)
                    }

                    // Set click listener on both the text view and image view
                    if (i < pitTextViews.size) {
                        pitTextViews[i].setOnClickListener(clickListener)
                    }
                    if (i < pitImageViews.size && pitImageViews[i] != null) {
                        pitImageViews[i].setOnClickListener(clickListener)
                    }
                } else {
                    // Remove click listeners when not player's turn
                    if (i < pitTextViews.size) {
                        pitTextViews[i].setOnClickListener(null)
                    }
                    if (i < pitImageViews.size && pitImageViews[i] != null) {
                        pitImageViews[i].setOnClickListener(null)
                    }
                }
            } else {
                // Player 2's pits (top row) - never clickable in single-player
                if (i < pitTextViews.size) {
                    pitTextViews[i].setOnClickListener(null)
                }
                if (i < pitImageViews.size && pitImageViews[i] != null) {
                    pitImageViews[i].setOnClickListener(null)
                }
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
                // Set click listener on both text and image views
                val clickListener = View.OnClickListener {
                    multiplayerViewModel.playMove(i)
                }

                if (i < pitTextViews.size) {
                    pitTextViews[i].setOnClickListener(clickListener)
                }
                if (i < pitImageViews.size && pitImageViews[i] != null) {
                    pitImageViews[i].setOnClickListener(clickListener)
                }
            } else {
                // Remove click listeners when not player's turn or no seeds
                if (i < pitTextViews.size) {
                    pitTextViews[i].setOnClickListener(null)
                }
                if (i < pitImageViews.size && pitImageViews[i] != null) {
                    pitImageViews[i].setOnClickListener(null)
                }
            }
        }

        // Player 2's pits are controlled by remote player
        for (i in 6..11) {
            if (i < pitTextViews.size) {
                pitTextViews[i].setOnClickListener(null)
            }
            if (i < pitImageViews.size && pitImageViews[i] != null) {
                pitImageViews[i].setOnClickListener(null)
            }
        }
    }



    /**
     * Shows game over dialog with custom background and two-button layout
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

        // Create a custom layout for the dialog with background
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over, null)
        val dialogBackground = dialogView.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.dialogBackground)
        dialogBackground.background = ContextCompat.getDrawable(this, R.drawable.game_over_background)

        // Update dialog text
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val messageText = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val playAgainButton = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.playAgainButton)
        val exitButton = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.exitButton)

        titleText.text = "Game Over"
        messageText.text = "$message\n\nFinal Score:\nYou: ${gameState.player1Score}\nOpponent/AI: ${gameState.player2Score}"

        // Fix: Store dialog reference for proper dismissal
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .show()

        // Fix: Ensure dialog closes before starting new game
        playAgainButton.setOnClickListener {
            dialog.dismiss() // Close dialog first
            if (isSinglePlayer) {
                singlePlayerViewModel.startNewGame()
            } else {
                finish() // Return to multiplayer lobby
            }
        }

        // Add exit button functionality
        exitButton.setOnClickListener {
            dialog.dismiss() // Close dialog first
            navigateToHomePage()
        }
    }

    /**
     * Navigate to home page with game mode selection
     */
    private fun navigateToHomePage() {
        // Navigate to MainMenuActivity (the actual home page with game mode buttons)
        val intent = Intent(this, MainMenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish() // Close current game activity to show home page
    }

    /**
     * Shows floating status card with maximum elevation
     */
    private fun showFloatingStatusCard(player: Int) {
        // Cancel any existing timer
        statusCardRunnable?.let { statusCardHandler.removeCallbacks(it) }

        // Update status card text based on player
        val statusText = if (isSinglePlayer) {
            if (player == 1) "Your Turn" else "AI Turn"
        } else {
            "Player $player Turn"
        }

        // Update the CardView text directly
        currentPlayer.text = statusText

        // Position CardView with maximum elevation to ensure it appears above everything
        val layoutParams = gameStatusCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

        if (player == 1) {
            // Bottom-left for Player 1
            layoutParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.leftMargin = 64 // 32dp margin
            layoutParams.bottomMargin = 200 // Position well above game board
        } else {
            // Top-right for Player 2/AI
            layoutParams.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            layoutParams.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.rightMargin = 64 // 32dp margin
            layoutParams.topMargin = 200 // Position well below header
        }

        gameStatusCard.layoutParams = layoutParams

        // Set maximum elevation to ensure it appears above all game elements
        gameStatusCard.elevation = 50f // Very high elevation

        // Show CardView with fade-in animation
        gameStatusCard.visibility = View.VISIBLE
        gameStatusCard.alpha = 0f
        gameStatusCard.animate()
            .alpha(1f)
            .setDuration(300)
            .setListener(null)

        statusCardVisible = true

        // Set auto-hide timer
        statusCardRunnable = Runnable {
            hideFloatingStatusCard()
        }
        statusCardHandler.postDelayed(statusCardRunnable!!, STATUS_DISPLAY_DURATION)
    }

    /**
     * Hides floating status card with fade-out animation
     */
    private fun hideFloatingStatusCard() {
        if (!statusCardVisible) return

        gameStatusCard.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    gameStatusCard.visibility = View.GONE
                    gameStatusCard.alpha = 1f // Reset for next show
                    statusCardVisible = false
                }
            })
    }

    /**
     * Updates the UI based on current game state with floating status
     */
    private fun updateUI(gameState: GameState) {
        android.util.Log.d("GameState", "🔄 UI Update - Player: ${gameState.currentPlayer}, GameOver: ${gameState.gameOver}")
        android.util.Log.d("GameState", "📊 Scores - P1: ${gameState.player1Score}, P2: ${gameState.player2Score}")
        android.util.Log.d("GameState", "🎲 Pits: ${gameState.pits.joinToString()}")

        // BASIC DEBUG: Check if we have previous state for comparison
        val currentPreviousState = previousGameState
        if (currentPreviousState == null) {
            android.util.Log.w("GameState", "⚠️ No previous state available for animation detection!")
        } else {
            android.util.Log.d("GameState", "✅ Previous state available: ${currentPreviousState.pits.joinToString()}")
        }

        if (gameState.gameOver) {
            android.util.Log.d("GameState", "🏁 Game Over! Winner: ${gameState.winner}")
            // Hide status card when game is over
            hideFloatingStatusCard()

            // Play win sound effect for game completion
            soundManager.playWinSound()

            // Submit game result to server for leaderboard tracking
            submitGameResult(gameState, isSinglePlayer)

            // Update leaderboard data when game completes
            if (isSinglePlayer) {
                // For single-player, we need to update leaderboard through GameViewModel
                // since SinglePlayerGameViewModel doesn't handle server communication
                multiplayerViewModel.onGameCompleted(gameState, true) // true = singlePlayer flag
                singlePlayerViewModel.onGameCompleted(gameState)
            } else {
                multiplayerViewModel.onGameCompleted(gameState, false)
            }

            showGameOverDialog(gameState)
            return
        }

        // Update scores with correct format
        player1Score.text = "Player 1: ${gameState.player1Score}"
        player2Score.text = if (isSinglePlayer) "${gameState.player2Score} :AI" else "Opponent: ${gameState.player2Score}"

        // Show floating status card for current player
        showFloatingStatusCard(gameState.currentPlayer)

        // Update pit displays with smart animation detection
        for (index in 0 until 12) {
            if (index < pitTextViews.size) {
                val animationType = detectAnimationType(index, gameState)
                visualSeedManager.updatePitSeeds(pitTextViews[index], gameState.pits[index], animationType)
            }

            // Update pit image background using ImageManager (with null safety)
            if (index < pitImageViews.size && pitImageViews[index] != null && ::imageManager.isInitialized) {
                val isPlayer1Pit = index < 6
                val shouldHighlight = if (isSinglePlayer) {
                    (gameState.currentPlayer == 1 && isPlayer1Pit && gameState.pits[index] > 0) ||
                    (gameState.currentPlayer == 2 && !isPlayer1Pit && gameState.pits[index] > 0)
                } else {
                    (gameState.currentPlayer == 1 && isPlayer1Pit && gameState.pits[index] > 0) ||
                    (gameState.currentPlayer == 2 && !isPlayer1Pit && gameState.pits[index] > 0)
                }

                if (shouldHighlight) {
                    imageManager.loadActivePitImage(pitImageViews[index])
                } else {
                    imageManager.loadPitImage(pitImageViews[index])
                }
            }
        }

        // Update TooltipCompat text for each pit
        for (index in 0 until 12) {
            if (index < pitTextViews.size && pitTextViews[index] != null) {
                val seedCount = gameState.pits[index]
                TooltipCompat.setTooltipText(pitTextViews[index], "Seeds: $seedCount")
            }
        }

        // Store current state for next comparison (only if not null)
        if (gameState != null) {
            previousGameState = gameState.copy()
            android.util.Log.d("GameState", "💾 Previous state stored for next comparison")
        }

        setupClickListeners(gameState)
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible (e.g., after returning from profile/settings)
         NigerianThemeManager.applyThemeToActivity(this)
         // Reload selected board background
         loadSelectedBoardBackground()
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
     }

     override fun onPause() {
         super.onPause()
         // Pause background music when activity is not visible
         com.naijaayo.worldwide.sound.BackgroundMusicManager.pauseBackgroundMusic()
     }

     override fun onDestroy() {
         super.onDestroy()
         // Clean up sound resources
         if (::soundManager.isInitialized) {
             soundManager.release()
         }
     }

    /**
     * Load sound preferences and apply them to the SoundManager
     */
    private fun loadSoundPreferences() {
        try {
            // Initialize the preferences manager
            SoundPreferencesManager.initialize(this)

            val soundEnabled = SoundPreferencesManager.isSoundEnabled()
            val masterVolume = SoundPreferencesManager.getMasterVolumeFloat()

            android.util.Log.d("SoundDebug", "🎵 Sound preferences loaded - Enabled: $soundEnabled, Volume: ${masterVolume * 100}% (raw: $masterVolume)")

            // Apply preferences to sound manager
            soundManager.setEnabled(soundEnabled)
            soundManager.setMasterVolume(masterVolume)

            android.util.Log.d("SoundDebug", "✅ Sound preferences applied successfully")
        } catch (e: Exception) {
            android.util.Log.e("SoundDebug", "❌ Error loading sound preferences", e)
            // Fallback to defaults
            soundManager.setEnabled(true)
            soundManager.setMasterVolume(0.7f)
        }
    }

    /**
     * TEST METHOD: Force a capture scenario to test capture detection
     * This creates a simulated game state that should trigger capture detection
     */
    private fun testCaptureScenario() {
        android.util.Log.d("TestCapture", "🧪 Starting capture test scenario...")

        // Create a test game state that simulates a capture
        val testPreviousState = GameState(
            pits = intArrayOf(4, 4, 4, 4, 4, 4, 3, 4, 4, 4, 4, 4), // Pit 6 has 3 seeds
            currentPlayer = 1,
            player1Score = 0,
            player2Score = 0,
            gameOver = false,
            winner = 0
        )

        val testCurrentState = GameState(
            pits = intArrayOf(4, 4, 4, 4, 4, 4, 0, 4, 4, 4, 4, 4), // Pit 6 captured (3→0)
            currentPlayer = 1,
            player1Score = 3, // Seeds captured
            player2Score = 0,
            gameOver = false,
            winner = 0
        )

        // Manually set previous state and test capture detection
        previousGameState = testPreviousState

        android.util.Log.d("TestCapture", "🔬 Testing capture detection...")
        android.util.Log.d("TestCapture", "Previous: ${testPreviousState.pits.joinToString()}")
        android.util.Log.d("TestCapture", "Current:  ${testCurrentState.pits.joinToString()}")

        // Force UI update with test state to trigger capture detection
        android.os.Handler().postDelayed({
            android.util.Log.d("TestCapture", "🚀 Triggering test UI update...")
            updateUI(testCurrentState)
        }, 5000) // Delay 5 seconds to ensure sound manager is fully initialized

        // Also add a manual test method for debugging
        android.os.Handler().postDelayed({
            android.util.Log.d("TestCapture", "🔧 Running manual capture sound test...")
            testCaptureSound()
        }, 7000) // Additional test after 7 seconds
    }

    /**
     * Manual test method to directly test capture sound playback
     */
    private fun testCaptureSound() {
        android.util.Log.d("TestCapture", "🎵 Testing capture sound directly...")

        try {
            // Test if sound manager is initialized
            if (::soundManager.isInitialized) {
                android.util.Log.d("TestCapture", "✅ SoundManager is initialized")

                // Test if capture sound is loaded
                val soundId = soundManager.javaClass.getDeclaredField("soundMap").let { field ->
                    field.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    val soundMap = field.get(soundManager) as MutableMap<String, Int>
                    soundMap["capture"]
                }

                if (soundId != null && soundId != 0) {
                    android.util.Log.d("TestCapture", "✅ Capture sound is loaded (ID: $soundId)")
                    android.util.Log.d("TestCapture", "🔊 Playing capture sound directly...")
                    soundManager.playCaptureSound()
                } else {
                    android.util.Log.e("TestCapture", "❌ Capture sound not loaded properly (ID: $soundId)")
                }
            } else {
                android.util.Log.e("TestCapture", "❌ SoundManager not initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("TestCapture", "❌ Error testing capture sound", e)
        }
    }

    /**
     * Add a manual test button to the UI for testing capture sound and animation
     */
    private fun addManualTestButton() {
        try {
            android.util.Log.d("TestCapture", "🔘 Adding manual test button to UI...")

            // Create a test button
            val testButton = androidx.appcompat.widget.AppCompatButton(this)
            testButton.text = "Test Capture"
            testButton.textSize = 12f
            testButton.setBackgroundColor(0xFF2196F3.toInt()) // Blue background
            testButton.setTextColor(0xFFFFFFFF.toInt()) // White text

            // Position the button in top-right corner using FrameLayout approach
            val layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = android.view.Gravity.TOP or android.view.Gravity.END
            layoutParams.rightMargin = 16
            layoutParams.topMargin = 16

            testButton.layoutParams = layoutParams

            // Add click listener
            testButton.setOnClickListener {
                android.util.Log.d("TestCapture", "🎯 Manual test button clicked!")
                testCaptureSoundAndAnimation()
                Toast.makeText(this, "Testing capture sound and animation...", Toast.LENGTH_SHORT).show()
            }

            // Add to root layout (which is a ConstraintLayout, but we'll treat it as a ViewGroup)
            val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(android.R.id.content)
                ?.getChildAt(0) as? androidx.constraintlayout.widget.ConstraintLayout
                ?: return

            rootLayout.addView(testButton)

            android.util.Log.d("TestCapture", "✅ Manual test button added successfully")

        } catch (e: Exception) {
            android.util.Log.e("TestCapture", "❌ Error adding manual test button", e)
        }
    }

    /**
     * Executes a move with sequential animations - pit clearing, sowing, then captures
     */
    private fun executeAnimatedMove(pitIndex: Int, isPlayerMove: Boolean = true, playerNumber: Int = 1) {
        val currentState = singlePlayerViewModel.gameState.value ?: return
        val gameEngine = com.naijaayo.worldwide.game.LocalGameEngine()

        android.util.Log.d("SequentialAnim", "🎬 Starting sequential animation for ${if (isPlayerMove) "player" else "AI"} pit $pitIndex")

        // Execute the full move immediately for correct game state
        val finalState = gameEngine.makeMove(currentState, pitIndex, playerNumber)
        if (finalState == null) {
            android.util.Log.w("SequentialAnim", "❌ Invalid move!")
            return
        }

        // Get capture information for animation
        val capturedPitIndices = finalState.lastCapturedPitIndices
        android.util.Log.d("SequentialAnim", "🎯 Captures detected: ${capturedPitIndices.size}")

        // Calculate the sowing steps for animation
        val sowingSteps = calculateSowingSteps(currentState.pits, pitIndex)
        android.util.Log.d("SequentialAnim", "📋 Calculated ${sowingSteps.size} sowing steps")

        // Store initial pit counts before animation
        val initialPitCounts = currentState.pits.copyOf()

        // Start the complete animation sequence: clear pit → sow → capture
        startCompleteAnimationSequence(pitIndex, sowingSteps, initialPitCounts, finalState, capturedPitIndices)
    }

    /**
     * Animates sowing sequence with proper timing
     */
    private fun animateSowingSequence(sowingSteps: List<SowingStep>,
                                     initialPitCounts: IntArray,
                                     finalState: GameState) {
        var stepIndex = 0
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                if (stepIndex < sowingSteps.size) {
                    val step = sowingSteps[stepIndex]
                    android.util.Log.d("SequentialAnim", "🌱 Animating sowing step ${stepIndex + 1}/${sowingSteps.size}: pit ${step.pitIndex} → ${step.pitValueAfterSowing}")

                    // Update pit display with sowing animation
                    if (step.pitIndex < pitTextViews.size) {
                        visualSeedManager.updatePitSeeds(
                            pitTextViews[step.pitIndex],
                            step.pitValueAfterSowing,
                            VisualSeedManager.AnimationType.SEED_ADDED
                        )
                    }

                    // Play wood sound for each seed landing
                    soundManager.playWoodSound(0.7f, SoundEventType.SEED_ADDED)

                    stepIndex++

                    // Schedule next sowing step with 500ms delay
                    handler.postDelayed(this, 500)
                } else {
                    android.util.Log.d("SequentialAnim", "✅ Sowing animation complete, updating to final game state")

                    // Sowing animation complete - now update to final state (triggers capture animations)
                    singlePlayerViewModel.updateGameState(finalState)

                    // Continue with game logic (AI move if needed)
                    if (isSinglePlayer && finalState.currentPlayer == 2 && !finalState.gameOver) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            android.util.Log.d("SequentialAnim", "🤖 Starting AI move after sowing animation...")

                            executeAIMove(finalState)
                        }, 1000) // Allow time for capture animations to complete
                    }
                }
            }
        }

        // Start the sowing animation sequence
        runnable.run()
    }

    /**
     * Calculates sowing steps for animation (simulates where each seed lands)
     */
    private fun calculateSowingSteps(originalPits: IntArray, startingPitIndex: Int): List<SowingStep> {
        val steps = mutableListOf<SowingStep>()
        val simulationPits = originalPits.copyOf()
        val seeds = simulationPits[startingPitIndex]

        // Clear the starting pit
        simulationPits[startingPitIndex] = 0

        // Simulate sowing seeds counterclockwise with proper Ayo logic
        var currentPit = startingPitIndex
        var seedsLeft = seeds

        while (seedsLeft > 0) {
            currentPit = (currentPit + 1) % 12

            // Ayo logic: ALWAYS sow into every pit, including wraparound into starting pit
            simulationPits[currentPit]++

            // Record this sowing step for ALL pits (including start pit on wraparound)
            steps.add(SowingStep(
                pitIndex = currentPit,
                pitValueAfterSowing = simulationPits[currentPit],
                isFinalStep = (seedsLeft == 1) // Mark the last seed
            ))

            seedsLeft--
        }

        return steps
    }

    /**
     * Executes AI move automatically with animations
     */
    private fun executeAIMove(gameState: GameState) {
        val gameEngine = com.naijaayo.worldwide.game.LocalGameEngine()
        val aiPitChoice = gameEngine.getValidMoves(gameState).randomOrNull()

        if (aiPitChoice != null) {
            android.util.Log.d("SequentialAnim", "🤖 AI choosing pit $aiPitChoice")

            // Execute AI move with the same animation logic as player moves
            executeAnimatedMove(aiPitChoice, false, 2)
        } else {
            android.util.Log.w("SequentialAnim", "🤖 AI has no valid moves!")
        }
    }

    /**
     * Starts the complete animation sequence: pit clearing → sowing → captures
     */
    private fun startCompleteAnimationSequence(pitIndex: Int,
                                              sowingSteps: List<SowingStep>,
                                              initialPitCounts: IntArray,
                                              finalState: GameState,
                                              capturedPitIndices: List<Int>) {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var currentSequence = 0 // 0=clearing pit, 1=sowing, 2=captures
        var sowingIndex = 0
        var captureIndex = 0

        val animationRunnable = object : Runnable {
            override fun run() {
                when (currentSequence) {
                    0 -> {
                        // Step 1: Clear the starting pit with animation and sound
                        android.util.Log.d("SequentialAnim", "🗑️ Clearing starting pit $pitIndex with animation/sound")
                        if (pitIndex < pitTextViews.size) {
                            visualSeedManager.updatePitSeeds(
                                pitTextViews[pitIndex],
                                0,
                                VisualSeedManager.AnimationType.SEED_REMOVED
                            )
                            // Play a distinct sound for picking up seeds from starting pit
                            soundManager.playWoodSound(0.8f, SoundEventType.SEED_REMOVED)
                        }
                        currentSequence = 1
                        // Continue immediately to sowing
                        this.run()
                    }

                    1 -> {
                        // Step 2: Animate sowing one seed at a time
                        if (sowingIndex < sowingSteps.size) {
                            val step = sowingSteps[sowingIndex]
                            android.util.Log.d("SequentialAnim", "🌱 Sowing animation step ${sowingIndex + 1}/${sowingSteps.size}: pit ${step.pitIndex} → ${step.pitValueAfterSowing}")

                            if (step.pitIndex < pitTextViews.size) {
                                visualSeedManager.updatePitSeeds(
                                    pitTextViews[step.pitIndex],
                                    step.pitValueAfterSowing,
                                    VisualSeedManager.AnimationType.SEED_ADDED
                                )
                            }

                            // Play wood sound for sowing
                            soundManager.playWoodSound(0.7f, SoundEventType.SEED_ADDED)

                            sowingIndex++
                            handler.postDelayed(this, 500) // 500ms delay between sowing steps
                        } else {
                            currentSequence = 2
                            // Continue immediately to captures
                            this.run()
                        }
                    }

                    2 -> {
                        // Step 3: Animate captures one pit at a time
                        if (captureIndex < capturedPitIndices.size) {
                            val capturedPitIndex = capturedPitIndices[captureIndex]
                            android.util.Log.d("SequentialAnim", "🎯 Capture animation step ${captureIndex + 1}/${capturedPitIndices.size}: pit $capturedPitIndex")

                            if (capturedPitIndex < pitTextViews.size) {
                                visualSeedManager.updatePitSeeds(
                                    pitTextViews[capturedPitIndex],
                                    0, // Captured pits become 0
                                    VisualSeedManager.AnimationType.CAPTURE
                                )
                            }

                            // Play capture sound for each capture
                            soundManager.playCaptureSound()

                            captureIndex++
                            handler.postDelayed(this, 1000) // 1s delay between captures
                        } else {
                            android.util.Log.d("SequentialAnim", "✅ Complete animation sequence finished")

                            // All animations complete - finally update game state
                            singlePlayerViewModel.updateGameState(finalState)

                            // Continue with game logic (AI move if turn has switched)
                            if (isSinglePlayer && finalState.currentPlayer == 2 && !finalState.gameOver) {
                                handler.postDelayed({
                                    android.util.Log.d("SequentialAnim", "🤖 Starting AI move after complete animation...")
                                    executeAIMove(finalState)
                                }, 1000)
                            }
                        }
                    }
                }
            }
        }

        // Start the complete animation sequence
        animationRunnable.run()
    }

    private fun applyRoomSettings(settings: RoomSettings) {
        // Get valid themes from NigerianThemeManager
        val validThemes = NigerianThemeManager.getAvailableThemes().map { it.id }

        // Apply theme using existing NigerianThemeManager
        val themeId = if (settings.themeId in validThemes) settings.themeId else "lagos"
        NigerianThemeManager.setActiveTheme(themeId)
        NigerianThemeManager.applyThemeToActivity(this)

        // Apply board background using existing BoardManager system
        BoardManager.setActiveBoard(settings.boardId)

        // Apply background music using existing SoundPreferencesManager system
        // Note: Theme and Music are separate settings. Music is applied directly via musicId
        if (settings.musicId.isNotEmpty()) {
            com.naijaayo.worldwide.sound.SoundPreferencesManager.setSelectedMusicTrack(settings.musicId)
            // Find the MusicTrack by ID before switching
            val musicTracks = com.naijaayo.worldwide.sound.BackgroundMusicManager.getAllMusicTracks()
            val selectedTrack = musicTracks.find { it.id == settings.musicId }
            if (selectedTrack != null) {
                com.naijaayo.worldwide.sound.BackgroundMusicManager.switchTrack(selectedTrack)
            }
        }

        // Apply sound settings using existing SoundPreferencesManager system
        com.naijaayo.worldwide.sound.SoundPreferencesManager.setSoundEnabled(settings.soundEnabled)
        com.naijaayo.worldwide.sound.SoundPreferencesManager.setMasterVolumeFloat(settings.volume)

        // Reload board background to apply new settings
        loadSelectedBoardBackground()
    }

    private fun submitGameResult(gameState: GameState, isSinglePlayer: Boolean) {
        // Only submit if user is authenticated
        if (!com.naijaayo.worldwide.auth.SessionManager.isLoggedIn()) {
            return // Skip submission for unauthenticated users
        }

        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        if (currentUser == null) return

        // TODO: Implement API call to submit game result
        // This will be implemented when the network layer is set up
        println("Game result ready for submission: Player ${currentUser.username} - Winner: ${gameState.winner}")
    }

    private fun saveCurrentGame() {
        // Check if user is authenticated
        if (!com.naijaayo.worldwide.auth.SessionManager.isLoggedIn()) {
            // Show authentication dialog
            val authDialog = com.naijaayo.worldwide.auth.AuthDialog(this) { userId, username, avatarId ->
                // After successful authentication, retry saving
                saveCurrentGame()
            }
            authDialog.show()
            return
        }

        // Get current game state
        val currentGameState = if (isSinglePlayer) {
            singlePlayerViewModel.gameState.value
        } else {
            multiplayerViewModel.gameState.value
        }

        if (currentGameState == null) {
            Toast.makeText(this, "No active game to save", Toast.LENGTH_SHORT).show()
            return
        }

        // Get current user
        val currentUser = com.naijaayo.worldwide.auth.SessionManager.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement actual save API call
        // For now, just show a placeholder message
        Toast.makeText(this, "Game saved successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun showResumeGameDialog(gameMode: String) {
        // Check if user is authenticated
        if (!com.naijaayo.worldwide.auth.SessionManager.isLoggedIn()) {
            // Show authentication dialog
            val authDialog = com.naijaayo.worldwide.auth.AuthDialog(this) { userId, username, avatarId ->
                // After successful authentication, retry showing resume dialog
                showResumeGameDialog(gameMode)
            }
            authDialog.show()
            return
        }

        // TODO: Fetch saved games from server
        // For now, show empty dialog or "no saved games" message
        val savedGames = emptyList<SavedGame>() // TODO: Replace with actual API call

        val resumeDialog = ResumeGameDialog(
            context = this,
            savedGames = savedGames,
            onGameSelected = { savedGame ->
                // TODO: Load selected game
                Toast.makeText(this, "Loading saved game...", Toast.LENGTH_SHORT).show()
            },
            onStartNewGame = {
                // Start new game based on mode
                if (gameMode == "single_player") {
                    singlePlayerViewModel.startNewGame(gameLevel)
                } else {
                    // For multiplayer, go back to lobby
                    finish()
                }
            }
        )
        resumeDialog.show()
    }
}

/**
 * Data class representing a sowing step for animation
 */
data class SowingStep(
    val pitIndex: Int,
    val pitValueAfterSowing: Int,
    val isFinalStep: Boolean = false
)
