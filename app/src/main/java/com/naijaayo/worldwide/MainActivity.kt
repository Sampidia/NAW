package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import android.widget.Button
import com.bumptech.glide.Glide
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.naijaayo.worldwide.game.SinglePlayerGameViewModel
import com.naijaayo.worldwide.network.FirebaseManager

import com.naijaayo.worldwide.sound.BackgroundMusicManager
import com.naijaayo.worldwide.sound.SoundManager
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.game.CaptureResult
import com.naijaayo.worldwide.game.MoveResult
import com.naijaayo.worldwide.game.SowingStep
import com.naijaayo.worldwide.ui.VisualSeedManager
import android.animation.ValueAnimator
import android.animation.ArgbEvaluator
import android.content.res.ColorStateList
import android.graphics.Color
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Agora Imports
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.Constants
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // --- NetworkGame Mode and State ---
    private var isSinglePlayer: Boolean = true
    private var gameLevel: GameLevel = GameLevel.MEDIUM
    private var gameId: String? = null
    private var selfUid: String? = null

    // Agora Voice Chat
    private var rtcEngine: RtcEngine? = null
    private var isVoiceEnabled = false
    private val AGORA_APP_ID = "4ebc60e6a9a44816b965f4b0253cda7d"
    private val PERMISSION_REQ_ID = 22

    // UI Elements
    private lateinit var player1Avatar: ImageView
    private lateinit var player2Avatar: ImageView
    private lateinit var player1Score: TextView
    private lateinit var player2Score: TextView
    private lateinit var currentPlayer: TextView
    private lateinit var menuButton: ImageButton
    private lateinit var actionButton: ImageButton // Reused for Mic toggle in MP and Save in SP


    // Firebase
    private var gameStateListener: ValueEventListener? = null
    private val localGameEngine = com.naijaayo.worldwide.game.LocalGameEngine() // For calculating MP animations
    private val gameUpdateChannel = kotlinx.coroutines.channels.Channel<PlayNowGame>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    // Single Player ViewModel
    private val singlePlayerViewModel: SinglePlayerGameViewModel by viewModels()

    // --- UI components ---

    // menuButton declaration removed (duplicate)
    private var statusPopup: android.widget.PopupWindow? = null
    private val pitContainers = mutableListOf<androidx.constraintlayout.widget.ConstraintLayout>()
    private lateinit var visualSeedManager: VisualSeedManager
    private lateinit var soundManager: SoundManager

    // --- Previous State for Sound Logic ---
    private var previousGameState: com.naijaayo.worldwide.LocalGameState? = null

    // --- Animation State ---
    private var isAnimating = false
    private var animationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        hideSystemUI() // Enable full screen
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // Prevent screen sleep during gameplay
        setContentView(R.layout.activity_main)

        // --- Initializations ---
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)
        AvatarPreferenceManager.initialize(this)
        BoardManager.initialize(this) // Initialize BoardManager
        BackgroundMusicManager.initialize(this)
        soundManager = SoundManager(this).apply { loadSounds() }
        com.naijaayo.worldwide.ads.AdMobHelper.initialize(this)
        com.naijaayo.worldwide.billing.BillingManager.initialize(this)

        initializeViews()
        setupMenu()
        updateBoardBackground() // Apply saved board background

        selfUid = FirebaseManager.auth.currentUser?.uid

        // --- NetworkGame Mode Detection ---
        gameId = intent.getStringExtra("GAME_ID")
        isSinglePlayer = gameId == null

        if (isSinglePlayer) {
            val levelName = intent.getStringExtra("level") ?: "MEDIUM"
            gameLevel = try { GameLevel.valueOf(levelName) } catch (e: Exception) { GameLevel.MEDIUM }
            setupSinglePlayerMode()
            
            // Single Player: Action Button -> Exit to Home
            actionButton.setImageResource(R.drawable.exit_icon)
            actionButton.setOnClickListener {
                 startActivity(Intent(this, MainMenuActivity::class.java))
                 finish()
            }
        } else {
            setupMultiplayerMode(gameId!!)
            val micEnabled = intent.getBooleanExtra("MIC_ENABLED", false)
            setupVoiceChat(micEnabled)
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private fun updateBoardBackground() {
        val activeBoard = BoardManager.getActiveBoard()
        if (activeBoard != null) {
            val resourceName = activeBoard.backgroundImagePath.replace(".png", "")
            val resourceId = resources.getIdentifier(resourceName, "drawable", packageName)
            if (resourceId != 0) {
                findViewById<ImageView>(R.id.boardBackground).setImageResource(resourceId)
            }
        }
    }

    private fun setupSinglePlayerMode() {
        singlePlayerViewModel.startNewGame(gameLevel)
        singlePlayerViewModel.gameState.observe(this) { gameState ->
            // The single-player VM provides the game state directly
            if (!isAnimating) {
                updateUiForSinglePlayer(gameState)
            }
        }



        // NEW: Move result observer for animations
        singlePlayerViewModel.moveResult.observe(this) { moveResult ->
            moveResult?.let {
                animateMoveSequence(it)
            }
        }

        // Add click listeners for player's pits
        for (i in 0..5) {
            pitContainers[i].setOnClickListener { 
                if (!isAnimating && singlePlayerViewModel.isValidMove(i)) {
                    soundManager.playClickSound()
                    singlePlayerViewModel.makePlayerMove(i) 
                }
            }
        }
    }

    private fun setupMultiplayerMode(gameId: String) {
        // Reset stats flag for new game
        statsRecorded = false

        // Start processing updates
        startUpdateProcessor()
        
        // Start background music for multiplayer
        BackgroundMusicManager.startBackgroundMusic()

        gameStateListener = FirebaseManager.listenForGameStateUpdates(gameId) { game ->
            if (game == null) {
                Toast.makeText(this, "NetworkGame room is no longer available.", Toast.LENGTH_LONG).show()
                finish()
                return@listenForGameStateUpdates
            }
            
            // Send to channel for sequential processing
            gameUpdateChannel.trySend(game)
        }
        
        // Listen for navigation actions from other player
        FirebaseManager.gamesRef.child(gameId).child("navigationAction").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val action = snapshot.getValue(String::class.java)
                when (action) {
                    "playAgain" -> {
                        startActivity(Intent(this@MainActivity, WaitingRoomActivity::class.java))
                        finish()
                    }
                    "mainMenu" -> {
                        startActivity(Intent(this@MainActivity, MainMenuActivity::class.java))
                        finish()
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("MainActivity", "Navigation listener cancelled", error.toException())
            }
        })
    }

    companion object {
        private var processorLaunchCount = 0
    }

    private var updateProcessorJob: Job? = null
    private var lastAnimatedMoveId: String? = null // Track last animated move to prevent duplicates

    private fun startUpdateProcessor() {
        // Cancel any existing processor to prevent duplicate loops
        updateProcessorJob?.cancel()
        
        val processorId = ++processorLaunchCount
        android.util.Log.d("MPAnimation", "Starting update processor #$processorId")
        
        updateProcessorJob = lifecycleScope.launch {
            android.util.Log.d("MPAnimation", "Processor #$processorId running")
            var currentDisplayedGame: PlayNowGame? = null
            
            for (newGame in gameUpdateChannel) {
                if (currentDisplayedGame == null) {
                    // Initial load
                    initializeMultiplayerUI(newGame)
                    currentDisplayedGame = newGame
                } else {
                    // PRIORITY 1: Check for game over FIRST (before animation checks)
                    android.util.Log.d("MPAnimation", "Checking game state: isGameOver=${newGame.gameState.gameOver}")
                    if (newGame.gameState.gameOver) {
                        android.util.Log.d("MPAnimation", "GAME OVER DETECTED! Calling updateUiForMultiplayer")
                        updateUiForMultiplayer(newGame)
                        currentDisplayedGame = newGame
                        continue
                    }
                    
                    // CRITICAL: Skip ALL updates while animation is in progress
                    if (isAnimating) {
                        android.util.Log.d("MPAnimation", "⏭️ Skipping update - animation in progress")
                        continue // Skip this update entirely
                    }
                    
                    val oldBoard = currentDisplayedGame!!.gameState.board
                    val newBoard = newGame.gameState.board
                    val boardChanged = oldBoard != newBoard
                    
                    android.util.Log.d("MPAnimation", "Update received. Board changed: $boardChanged")

                    // Check for changes and animate if needed
                    val animated = detectAndAnimateMultiplayerMove(currentDisplayedGame!!, newGame)
                    
                    if (!animated) {
                        if (boardChanged) {
                            // Board changed but animation failed/skipped -> Full Update (seeds + everything)
                            android.util.Log.d("MPAnimation", "Board changed but no animation -> Force full update")
                            updateUiForMultiplayer(newGame)
                        } else {
                            // Board didn't change -> Update everything EXCEPT seeds (turn, backgrounds, text)
                            // This ensures turn indicators update even if board doesn't
                            updateMultiplayerPitBackgroundsAndTurn(newGame)
                        }
                    }
                    // If animated, the animation will handle ALL visual updates
                    
                    currentDisplayedGame = newGame
                }
            }
        }
    }



    private fun initializeMultiplayerUI(game: PlayNowGame) {
        // Called ONCE on initial load - sets up static elements ONLY
        
        // Set up avatars (once - these don't change during game)
        updateMultiplayerAvatars(game)
        
        // Initial UI update
        updateUiForMultiplayer(game)
    }

    private suspend fun detectAndAnimateMultiplayerMove(oldGame: PlayNowGame, newGame: PlayNowGame): Boolean {
        android.util.Log.d("MPAnimation", "=== detectAndAnimateMultiplayerMove called ===")
        
        // CRITICAL: Prevent overlapping animations
        if (isAnimating) {
            android.util.Log.d("MPAnimation", "⏭️ Skipping - animation already in progress")
            return false
        }
        
        // If game over, just return false to let caller update UI
        if (newGame.gameState.gameOver) {
            android.util.Log.d("MPAnimation", "NetworkGame over detected, skipping animation")
            return false
        }

        // Find which pit changed to 0 (the move source)
        // The player who moved is the one whose turn it WAS
        val previousPlayerUid = oldGame.gameState.nextPlayerUid
        val myPlayerNumber = if (selfUid == oldGame.creatorUid) 1 else 2
        val wasMyTurn = previousPlayerUid == selfUid
        
        // Determine player number (1 or 2) for the move
        val playerNum = if (wasMyTurn) myPlayerNumber else (if (myPlayerNumber == 1) 2 else 1)
        android.util.Log.d("MPAnimation", "Player who moved: $playerNum (wasMyTurn: $wasMyTurn)")
        
        // Find the pit that became empty (or was clicked)
        var movedPitIndex = -1
        
        // PRIORITY 1: Use explicit move index from Firebase (New Robust Method)
        val explicitMoveIndex = newGame.gameState.lastMovePitIndex
        android.util.Log.d("MPAnimation", "Explicit move index from Firebase: $explicitMoveIndex")
        
        if (explicitMoveIndex != null && explicitMoveIndex != -1) {
            // Verify it's a valid move for the player (basic sanity check)
            val isPlayer1Pit = explicitMoveIndex in 0..5
            val isPlayer2Pit = explicitMoveIndex in 6..11
            
            if ((playerNum == 1 && isPlayer1Pit) || (playerNum == 2 && isPlayer2Pit)) {
                movedPitIndex = explicitMoveIndex
                android.util.Log.d("MPAnimation", "Using explicit move index: $movedPitIndex")
            }
        }
        
        // PRIORITY 2: Fallback to heuristic detection (Old Method - for backward compatibility)
        if (movedPitIndex == -1) {
            android.util.Log.d("MPAnimation", "Falling back to heuristic detection")
            android.util.Log.d("MPAnimation", "Old board: ${oldGame.gameState.board}")
            android.util.Log.d("MPAnimation", "New board: ${newGame.gameState.board}")
            
            for (i in 0..11) {
                if (oldGame.gameState.board[i] > 0 && newGame.gameState.board[i] == 0) {
                    val isPlayer1Pit = i in 0..5
                    val isPlayer2Pit = i in 6..11
                    
                    if ((playerNum == 1 && isPlayer1Pit) || (playerNum == 2 && isPlayer2Pit)) {
                        movedPitIndex = i
                        android.util.Log.d("MPAnimation", "Heuristic found move at pit: $movedPitIndex")
                        break
                    }
                }
            }
        }

        if (movedPitIndex != -1) {
            // Create unique move ID based on board state to prevent duplicate animations
            val moveId = "${newGame.gameState.board.hashCode()}_$movedPitIndex"
            
            // Skip if we already animated this exact move
            if (moveId == lastAnimatedMoveId) {
                android.util.Log.d("MPAnimation", "⏭️ Skipping - already animated move $moveId")
                return false
            }
            
            android.util.Log.d("MPAnimation", "✅ Move detected at pit $movedPitIndex, starting animation")
            
            // SET ANIMATION LOCK
            isAnimating = true
            lastAnimatedMoveId = moveId // Remember this move
            
            // Play click sound to indicate a move was made
            soundManager.playClickSound()
            
            // Reconstruct local state to calculate animation steps
            // Convert game level from Firebase to GameLevel enum
            val gameLevel = when (newGame.level) {
                "EASY" -> com.naijaayo.worldwide.GameLevel.EASY
                "HARD" -> com.naijaayo.worldwide.GameLevel.HARD
                else -> com.naijaayo.worldwide.GameLevel.MEDIUM
            }
            
            val localState = com.naijaayo.worldwide.LocalGameState(
                pits = oldGame.gameState.board.toIntArray(),
                player1Score = oldGame.gameState.player1Score,
                player2Score = oldGame.gameState.player2Score,
                currentPlayer = playerNum,
                gameOver = false,
                level = gameLevel
            )

            // Calculate animation steps locally
            val moveResult = localGameEngine.makeAnimatedMove(localState, movedPitIndex, playerNum)
            
            if (moveResult != null) {
                android.util.Log.d("MPAnimation", "MoveResult calculated: ${moveResult.sowingSteps.size} sowing steps, ${moveResult.captureResult.capturedPitIndices.size} captures")
                
                // Animate and WAIT for completion before returning
                animateMultiplayerMoveSequence(moveResult, newGame)
                android.util.Log.d("MPAnimation", "Animation sequence completed")
                
                // RELEASE ANIMATION LOCK
                isAnimating = false
                
                return true
            } else {
                android.util.Log.e("MPAnimation", "❌ makeAnimatedMove returned null!")
                isAnimating = false // Release lock on error
            }
        } else {
            android.util.Log.d("MPAnimation", "❌ No move detected (movedPitIndex = -1)")
        }
        
        return false
    }

    private suspend fun animateMultiplayerMoveSequence(moveResult: MoveResult, finalGame: PlayNowGame) {
        android.util.Log.d("MPAnimation", ">>> animateMultiplayerMoveSequence started")
        try {
            // Initial delay to let the click sound finish before sowing starts
            delay(300)
            android.util.Log.d("MPAnimation", "Initial delay complete")
            
            // Phase 0: Animate picking up seeds (emptying the starting pit)
            if (moveResult.sowingSteps.isNotEmpty()) {
                // The starting pit is the one before the first sowing step, wrapping around if needed
                val firstStepIndex = moveResult.sowingSteps.first().pitIndex
                val startPitIndex = if (firstStepIndex == 0) 11 else firstStepIndex - 1
                
                android.util.Log.d("MPAnimation", "Phase 0: Animating seed pickup from pit $startPitIndex")
                animateSeedPickup(startPitIndex)
                android.util.Log.d("MPAnimation", "Phase 0: Seed pickup complete")
            }

            // Phase 1: Animate sowing steps
            android.util.Log.d("MPAnimation", "Phase 1: Starting sowing animation (${moveResult.sowingSteps.size} steps)")
            animateSowingSteps(moveResult.sowingSteps)
            android.util.Log.d("MPAnimation", "Phase 1: Sowing complete")
            
            // Phase 2: Animate captures
            android.util.Log.d("MPAnimation", "Phase 2: Starting capture animation (${moveResult.captureResult.capturedPitIndices.size} pits)")
            animateCaptureSteps(moveResult.captureResult)
            android.util.Log.d("MPAnimation", "Phase 2: Captures complete")
            
            // Phase 3: Update ONLY non-visual elements (pit backgrounds, turn indicator, click listeners)
            // DO NOT update seed counts - animations already did that!
            android.util.Log.d("MPAnimation", "Phase 3: Updating pit backgrounds and turn state")
            updateMultiplayerPitBackgroundsAndTurn(finalGame)
            android.util.Log.d("MPAnimation", "Phase 3: Update complete")
            
            // Show floating dialog after animation
            if (!finalGame.gameState.gameOver) {
                showGameStatus("", finalGame)
            }
            
            android.util.Log.d("MPAnimation", "<<< animateMultiplayerMoveSequence completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("MPAnimation", "❌ Error during animation: ${e.message}", e)
            // On error, do full update
            updateUiForMultiplayer(finalGame)
            // CRITICAL: Release animation lock on error
            isAnimating = false
        }
    }

    private fun updateMultiplayerPitBackgroundsAndTurn(game: PlayNowGame) {
        // AFTER-ANIMATION UPDATE - Only updates pit backgrounds and turn state
        // Seeds were already updated by animations - DO NOT touch them!
        
        if (game.gameState.gameOver) {
            val myPlayerNumber = if (selfUid == game.creatorUid) 1 else 2
            val winnerNumber = when (game.gameState.winnerUid) {
                selfUid -> 1 // I won
                null -> 0    // Draw
                else -> 2    // Opponent won
            }
            
            // Correctly determine MY score and OPPONENT score
            val myScore = if (myPlayerNumber == 1) game.gameState.player1Score else game.gameState.player2Score
            val opponentScore = if (myPlayerNumber == 1) game.gameState.player2Score else game.gameState.player1Score
            
            showGameOverDialog(winnerNumber, myScore, opponentScore)
            return
        }

        val me = game.players[selfUid]
        val opponent = game.players.values.find { it.uid != selfUid }
        val isMyTurn = game.gameState.nextPlayerUid == selfUid
        val currentPlayerNum = if (isMyTurn) (if (selfUid == game.creatorUid) 1 else 2) else (if (selfUid == game.creatorUid) 2 else 1)

        // Update text labels with scores
        val myPlayerNumber = if (selfUid == game.creatorUid) 1 else 2
        val myScore = if (myPlayerNumber == 1) game.gameState.player1Score else game.gameState.player2Score
        val opponentScore = if (myPlayerNumber == 1) game.gameState.player2Score else game.gameState.player1Score

        player1Score.text = "${me?.displayName ?: "You"}: $myScore"
        player2Score.text = "${opponent?.displayName ?: "Opponent"}: $opponentScore"
        currentPlayer.text = if (isMyTurn) "Your Turn" else "Opponent's Turn"

        // Update ONLY pit backgrounds (not seed counts!)
        game.gameState.board.forEachIndexed { index, _ ->
            updatePitBackground(index, currentPlayerNum)
        }
        
        // Update click listeners
        setupMultiplayerClickListeners(game, isMyTurn)
    }

    private fun performMultiplayerMove(pitIndex: Int) {
        android.util.Log.d("MPAnimation", "User clicked pit $pitIndex")
        lifecycleScope.launch {
            gameId?.let { FirebaseManager.makeMove(it, pitIndex) }
        }
    }

    private fun updatePitBackground(pitIndex: Int, currentPlayer: Int) {
        val pitImageId = resources.getIdentifier("pitImage_$pitIndex", "id", packageName)
        val pitImageView = pitContainers[pitIndex].findViewById<ImageView>(pitImageId)

        // Player 1 owns pits 0-5, Player 2 owns pits 6-11
        val isPlayer1Pit = pitIndex in 0..5
        val isPlayer2Pit = pitIndex in 6..11

        val isActive = (currentPlayer == 1 && isPlayer1Pit) || (currentPlayer == 2 && isPlayer2Pit)

        if (isActive) {
            pitImageView.setImageResource(R.drawable.pit_active)
        } else {
            pitImageView.setImageResource(R.drawable.pit_normal)
        }
    }




    // --- UI Update Logic ---

    private fun updateUiForSinglePlayer(state: com.naijaayo.worldwide.LocalGameState) {
        // playGameSounds(state) - REMOVED, handled by animation system
        previousGameState = state

        if (state.gameOver) {
            showGameOverDialog(state.winner ?: 0, state.player1Score, state.player2Score)
            return
        }

        player1Score.text = "Player 1: ${state.player1Score}"
        player2Score.text = "AI: ${state.player2Score}"
        currentPlayer.text = if (state.currentPlayer == 1) "Your Turn" else "Opponent's Turn"

        state.pits.forEachIndexed { index, count ->
            val pitTextId = resources.getIdentifier("pitText_$index", "id", packageName)
            visualSeedManager.updatePitSeeds(pitContainers[index].findViewById(pitTextId), count, VisualSeedManager.AnimationType.NONE)
            
            // Update pit background
            updatePitBackground(index, state.currentPlayer)
        }
    }

    // Flag to prevent multiple stats updates
    private var statsRecorded = false

    private fun updateUiForMultiplayer(game: PlayNowGame) {
        // FULL UI UPDATE - Called ONLY after animations complete
        // ALL visual elements including seeds and pit backgrounds
        
        // Check for navigation actions first
        if (game.navigationAction != null) {
            when (game.navigationAction) {
                "playAgain" -> {
                    startActivity(Intent(this, WaitingRoomActivity::class.java))
                    finish()
                    return
                }
                "mainMenu" -> {
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                    return
                }
            }
        }


        val me = game.players[selfUid]
        val opponent = game.players.values.find { it.uid != selfUid }
        
        // CRITICAL: Determine player number robustly using creatorUid
        // If I am creator, I am Player 1. Otherwise Player 2.
        val myPlayerNumber = if (selfUid == game.creatorUid) 1 else 2
        
        val myScore = if (myPlayerNumber == 1) game.gameState.player1Score else game.gameState.player2Score
        val opponentScore = if (myPlayerNumber == 1) game.gameState.player2Score else game.gameState.player1Score

        if (game.gameState.gameOver) {
            // Logic for winner display:
            // player1Score TextView shows ME. player2Score TextView shows OPPONENT.
            // So winner=1 means I won. winner=2 means Opponent won.
            val winnerNumber = when (game.gameState.winnerUid) {
                selfUid -> 1 
                null -> 0
                else -> 2 
            }
            // Pass scores in order: MyScore, OpponentScore
            showGameOverDialog(winnerNumber, myScore, opponentScore)

            // RECORD STATS TO DATABASE
            // Each player updates THEIR OWN stats to comply with security rules
            if (!statsRecorded) {
                statsRecorded = true
                android.util.Log.d("Stats", "Recording multiplayer stats for self...")
                
                lifecycleScope.launch {
                    try {
                        FirebaseManager.recordMultiplayerResultForSelf(gameId ?: "", game.gameState.winnerUid)
                        android.util.Log.d("Stats", "Stats recorded successfully.")
                    } catch (e: Exception) {
                        android.util.Log.e("Stats", "Failed to record stats", e)
                    }
                }
            }
            return
        }

        val isMyTurn = game.gameState.nextPlayerUid == selfUid
        // Current Player Number (1 or 2) for pit highlighting
        val currentPlayerNum = if (isMyTurn) myPlayerNumber else (if (myPlayerNumber == 1) 2 else 1)

        player1Score.text = me?.displayName ?: "You"
        player2Score.text = opponent?.displayName ?: "Opponent"
        currentPlayer.text = if (isMyTurn) "Your Turn" else "Opponent's Turn"

        // Update seeds and pit backgrounds
        game.gameState.board.forEachIndexed { index, count ->
            val pitTextId = resources.getIdentifier("pitText_$index", "id", packageName)
            visualSeedManager.updatePitSeeds(pitContainers[index].findViewById(pitTextId), count, VisualSeedManager.AnimationType.NONE)
            
            // Update pit background to show active player
            updatePitBackground(index, currentPlayerNum)
        }
        
        // Update click listeners (needed because turn and pit states change)
        setupMultiplayerClickListeners(game, isMyTurn)
    }
    
    private fun updateMultiplayerNonVisualElements(game: PlayNowGame) {
        // LIGHTWEIGHT UPDATE - Only updates text and click listeners
        // NO visual updates to seeds or pit backgrounds (prevents page refresh)
        // Used when no move was detected (e.g., status changes only)
        
        if (game.gameState.gameOver) {
            val myPlayerNumber = if (selfUid == game.creatorUid) 1 else 2
            val winnerNumber = when (game.gameState.winnerUid) {
                selfUid -> 1 
                null -> 0
                else -> 2 
            }
            // Correctly determine MY score and OPPONENT score
            val myScore = if (myPlayerNumber == 1) game.gameState.player1Score else game.gameState.player2Score
            val opponentScore = if (myPlayerNumber == 1) game.gameState.player2Score else game.gameState.player1Score
            
            showGameOverDialog(winnerNumber, myScore, opponentScore)
            return
        }

        val me = game.players[selfUid]
        val opponent = game.players.values.find { it.uid != selfUid }
        val isMyTurn = game.gameState.nextPlayerUid == selfUid

        // Only update text elements - NO visual board updates
        player1Score.text = me?.displayName ?: "You"
        player2Score.text = opponent?.displayName ?: "Opponent"
        currentPlayer.text = if (isMyTurn) "Your Turn" else "Opponent's Turn"
        
        // Update click listeners for new turn
    }

    private fun animateMoveSequence(moveResult: MoveResult) {
        isAnimating = true
        
        // Cancel any existing animation
        animationJob?.cancel()
        
        animationJob = lifecycleScope.launch {
            try {
                // Initial delay to let the "Click" sound finish before sowing starts
                delay(300)

                // Phase 0: Animate picking up seeds (emptying the starting pit)
                if (moveResult.sowingSteps.isNotEmpty()) {
                    val firstStepIndex = moveResult.sowingSteps.first().pitIndex
                    val startPitIndex = if (firstStepIndex == 0) 11 else firstStepIndex - 1
                    animateSeedPickup(startPitIndex)
                }

                // Phase 1: Animate sowing steps
                animateSowingSteps(moveResult.sowingSteps)
                
                // Phase 2: Animate captures
                animateCaptureSteps(moveResult.captureResult)
                
                // Phase 3: Update final state
                singlePlayerViewModel.onAnimationComplete(moveResult.finalState)
                
            } catch (e: Exception) {
                android.util.Log.e("Animation", "Error during animation: ${e.message}")
                // Fallback: update state immediately
                singlePlayerViewModel.onAnimationComplete(moveResult.finalState)
            } finally {
                isAnimating = false
                // Force UI update after animation to ensure final state is shown
                singlePlayerViewModel.gameState.value?.let { 
                    updateUiForSinglePlayer(it)
                    // Show status popup after animation
                    if (!it.gameOver) {
                        showGameStatus("")
                    }
                }
            }
        }
    }

    private suspend fun animateSowingSteps(sowingSteps: List<SowingStep>) {
        for (step in sowingSteps) {
            // Update pit visual with animation
            val pitTextId = resources.getIdentifier("pitText_${step.pitIndex}", "id", packageName)
            val pitImageView = pitContainers[step.pitIndex].findViewById<ImageView>(pitTextId)
            
            visualSeedManager.updatePitSeeds(
                pitImageView, 
                step.pitValueAfterSowing, 
                VisualSeedManager.AnimationType.SEED_ADDED
            )
            
            // Play wood sound for this seed
            soundManager.playWoodSound(
                volume = 0.7f,
                eventType = com.naijaayo.worldwide.sound.SoundEventType.SEED_ADDED
            )
            
            // Wait for animation to complete - 700ms for better followability
            delay(700)
        }
    }

    private suspend fun animateCaptureSteps(captureResult: CaptureResult) {
        if (captureResult.capturedPitIndices.isEmpty()) return
        
        // Animate each captured pit sequentially
        for (pitIndex in captureResult.capturedPitIndices) {
            // Play capture sound for EACH captured pit
            soundManager.playCaptureSound()

            val pitTextId = resources.getIdentifier("pitText_$pitIndex", "id", packageName)
            val pitImageView = pitContainers[pitIndex].findViewById<ImageView>(pitTextId)
            
            // Show capture animation (golden glow)
            visualSeedManager.updatePitSeeds(
                pitImageView, 
                0,  // Captured pits become empty
                VisualSeedManager.AnimationType.CAPTURE
            )
            
            // Wait for each capture animation to be visible before showing the next
            delay(400)
        }
    }

    private suspend fun animateSeedPickup(pitIndex: Int) {
        val pitTextId = resources.getIdentifier("pitText_$pitIndex", "id", packageName)
        val pitImageView = pitContainers[pitIndex].findViewById<ImageView>(pitTextId)
        
        // Visually remove seeds (red fade) and set count to 0
        visualSeedManager.updatePitSeeds(
            pitImageView, 
            0, 
            VisualSeedManager.AnimationType.SEED_REMOVED
        )
        
        // Small delay to show the "pickup" action
        delay(200)
    }

    private fun setupMultiplayerClickListeners(game: PlayNowGame, isMyTurn: Boolean) {
        val myPlayerNumber = if (selfUid == game.creatorUid) 1 else 2

        for (i in 0..11) {
            val pitContainer = pitContainers[i]
            val isMyPit = (myPlayerNumber == 1 && i < 6) || (myPlayerNumber == 2 && i >= 6)

            if (isMyTurn && isMyPit && game.gameState.board[i] > 0) {
                pitContainer.setOnClickListener { 
                    soundManager.playClickSound()
                    performMultiplayerMove(i) 
                }
            } else {
                pitContainer.setOnClickListener(null)
            }
        }
    }



    private fun showGameStatus(message: String, multiplayerGame: PlayNowGame? = null) {
        // Safety check: ensure activity is in valid state
        if (isFinishing || isDestroyed) return
        
        val titleText: String
        val messageText: String
        val isLocalPlayerTurn: Boolean
        
        if (multiplayerGame != null) {
            // MULTIPLAYER MODE
            val nextPlayerUid = multiplayerGame.gameState.nextPlayerUid
            val playersList = multiplayerGame.players.values.toList()
            val player1 = playersList.getOrNull(0)
            val player2 = playersList.getOrNull(1)
            
            // Determine whose turn it is based on nextPlayerUid
            // Use stable player lookup
            val player1Uid = multiplayerGame.creatorUid
            val player2Uid = multiplayerGame.players.keys.find { it != player1Uid }
            
            val currentPlayerData = if (nextPlayerUid == player1Uid) {
                if (player1Uid != null) multiplayerGame.players[player1Uid] else null
            } else {
                if (player2Uid != null) multiplayerGame.players[player2Uid] else null
            }
            val currentPlayerName = currentPlayerData?.displayName ?: "Opponent"
            
            // Check if it's the local player's turn
            isLocalPlayerTurn = nextPlayerUid == selfUid
            
            if (isLocalPlayerTurn) {
                titleText = currentPlayerName
                messageText = "Make Move"
            } else {
                titleText = currentPlayerName
                messageText = "Thinking..."
            }
        } else {
            // SINGLE PLAYER MODE (existing logic)
            val state = singlePlayerViewModel.gameState.value
            isLocalPlayerTurn = state?.currentPlayer == 1
            
            if (isLocalPlayerTurn) {
                titleText = "Your Turn"
                messageText = "Make Move"
            } else {
                titleText = "Ai Turn"
                messageText = "Ai thinking"
            }
        }

        // Dismiss existing popup
        statusPopup?.dismiss()

        // Post to ensure view is ready
        findViewById<View>(android.R.id.content).post {
            try {
                // Double check activity state
                if (isFinishing || isDestroyed) return@post
                
                // Inflate popup layout
                val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as android.view.LayoutInflater
                val popupView = inflater.inflate(R.layout.dialog_floating_status, null)

                // Set message based on turn
                val titleTextView = popupView.findViewById<TextView>(R.id.dialogCurrentPlayer)
                val messageTextView = popupView.findViewById<TextView>(R.id.dialogGameMessage)
                
                titleTextView.text = titleText
                messageTextView.text = messageText
                
                // Set spacing based on turn
                if (isLocalPlayerTurn) {
                    // Set smaller spacing for player turn (1dp)
                    val params = messageTextView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topMargin = (1 * resources.displayMetrics.density).toInt()
                    messageTextView.layoutParams = params
                } else {
                    // Set spacing for opponent/AI turn (2dp)
                    val params = messageTextView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.topMargin = (2 * resources.displayMetrics.density).toInt()
                    messageTextView.layoutParams = params
                }

                // Create popup window
                statusPopup = android.widget.PopupWindow(
                    popupView,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    false
                ).apply {
                    setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                    elevation = 100f
                }

                // Calculate position
                val rootView = findViewById<View>(android.R.id.content)
                val location = IntArray(2)
                rootView.getLocationOnScreen(location)

                val xOffset: Int
                val yOffset: Int

                if (isLocalPlayerTurn) {
                    // Bottom Left - moved up
                    xOffset = 16
                    yOffset = -280 // From bottom (increased to move up)
                    statusPopup?.showAtLocation(rootView, android.view.Gravity.BOTTOM or android.view.Gravity.START, xOffset, yOffset)
                } else {
                    // Top Right
                    val headerLayout = findViewById<View>(R.id.headerLayout)
                    val headerLocation = IntArray(2)
                    headerLayout.getLocationOnScreen(headerLocation)
                    val headerHeight = headerLayout.height
                    
                    xOffset = -16
                    yOffset = headerHeight + 16
                    statusPopup?.showAtLocation(rootView, android.view.Gravity.TOP or android.view.Gravity.END, xOffset, yOffset)
                }

                // Auto-dismiss after 2 seconds
                lifecycleScope.launch {
                    delay(2000)
                    statusPopup?.dismiss()
                }
            } catch (e: Exception) {
                // Silently catch any window token exceptions
                android.util.Log.e("MainActivity", "Error showing status popup", e)
            }
        }
    }

    private fun showGameOverDialog(winner: Int, p1Score: Int, p2Score: Int) {
        // Play win sound regardless of winner (game over event)
        val mediaPlayer = android.media.MediaPlayer.create(this, R.raw.win_sound)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener { it.release() }

        // Inflate custom layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_over, null)
        val logoImageView = dialogView.findViewById<ImageView>(R.id.gameOverLogo)
        val winnerText = dialogView.findViewById<TextView>(R.id.winnerAnnouncement)
        val scoreText1 = dialogView.findViewById<TextView>(R.id.scoreText1)
        val scoreText2 = dialogView.findViewById<TextView>(R.id.scoreText2)
        val btnPlayAgain = dialogView.findViewById<MaterialButton>(R.id.btnPlayAgain)
        val btnMainMenu = dialogView.findViewById<MaterialButton>(R.id.btnMainMenu)

        // Helper to animate rainbow border
        fun animateRainbowBorder(button: MaterialButton) {
            val rainbowColors = intArrayOf(
                Color.RED, Color.parseColor("#FFA500"), Color.YELLOW, 
                Color.GREEN, Color.BLUE, Color.parseColor("#4B0082"), Color.parseColor("#EE82EE")
            )
            
            val colorAnimation = ValueAnimator.ofInt(*rainbowColors)
            colorAnimation.duration = 2000 // 2 seconds for full cycle
            colorAnimation.repeatCount = ValueAnimator.INFINITE
            colorAnimation.repeatMode = ValueAnimator.REVERSE
            colorAnimation.setEvaluator(ArgbEvaluator())
            
            colorAnimation.addUpdateListener { animator ->
                button.strokeColor = ColorStateList.valueOf(animator.animatedValue as Int)
                button.invalidate() // Force redraw
            }
            
            colorAnimation.start()
        }

        // Start animations
        animateRainbowBorder(btnMainMenu)
        animateRainbowBorder(btnPlayAgain)

        // Load animated logo using Glide
        com.bumptech.glide.Glide.with(this)
            .asGif()
            .load(R.raw.logo_animate)
            .into(logoImageView)

        // Determine Winner Text
        if (isSinglePlayer) {
            winnerText.text = when (winner) {
                1 -> "Player 1 Wins"
                2 -> "AI Wins"
                else -> "It's a Draw"
            }
            scoreText1.text = "Player 1 : $p1Score"
            scoreText2.text = "Ai : $p2Score"
        } else {
            // Multiplayer Logic - Extract names safely
            val p1Raw = player1Score.text.toString()
            val p2Raw = player2Score.text.toString()
            
            // Format is "Name: Score" or just "Name" if score not appended yet
            val p1Name = if (p1Raw.contains(":")) p1Raw.split(":")[0].trim() else p1Raw
            val p2Name = if (p2Raw.contains(":")) p2Raw.split(":")[0].trim() else p2Raw
            
            winnerText.text = when (winner) {
                1 -> "$p1Name Wins"
                2 -> "$p2Name Wins"
                else -> "It's a Draw"
            }
            scoreText1.text = "$p1Name : $p1Score"
            scoreText2.text = "$p2Name : $p2Score"
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Button Listeners
        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            if (isSinglePlayer) {
                startActivity(Intent(this, LevelSelectionActivity::class.java))
                finish()
            } else {
                // Multiplayer: Update game state to trigger navigation for both players
                lifecycleScope.launch {
                    try {
                        gameId?.let { id ->
                            FirebaseManager.gamesRef.child(id).child("navigationAction").setValue("playAgain")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to set navigation action", e)
                        // Fallback: navigate anyway
                        startActivity(Intent(this@MainActivity, WaitingRoomActivity::class.java))
                        finish()
                    }
                }
            }
        }

        btnMainMenu.setOnClickListener {
            dialog.dismiss()
            if (isSinglePlayer) {
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            } else {
                // Multiplayer: Update game state to trigger navigation for both players
                lifecycleScope.launch {
                    try {
                        gameId?.let { id ->
                            FirebaseManager.gamesRef.child(id).child("navigationAction").setValue("mainMenu")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to set navigation action", e)
                        // Fallback: navigate anyway
                        startActivity(Intent(this@MainActivity, MainMenuActivity::class.java))
                        finish()
                    }
                }
            }
        }

        dialog.show()
        
        // Update Firebase scores if needed
        val user = FirebaseManager.auth.currentUser
        // Record game statistics (wins, losses, draws, totalPoints)
        if (user != null) {
            lifecycleScope.launch {
                val displayName = user.displayName ?: "Player"
                val avatarId = AvatarPreferenceManager.getUserAvatar()
                
                if (isSinglePlayer) {
                    // Single-player statistics
                    val currentGameState = singlePlayerViewModel.gameState.value ?: return@launch
                    val result = when {
                        currentGameState.winner == 1 -> FirebaseManager.GameResult.WIN
                        currentGameState.winner == 2 -> FirebaseManager.GameResult.LOSS
                        else -> FirebaseManager.GameResult.DRAW
                    }
                    
                    try {
                        FirebaseManager.recordSinglePlayerResult(user.uid, result, displayName, avatarId)
                        android.util.Log.d("MainActivity", "Single-player statistics recorded: $result")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to record single-player statistics", e)
                    }
                }
            }
        }
    }

    private fun updateMultiplayerAvatars(game: PlayNowGame) {
        val me = game.players[selfUid]
        val opponent = game.players.values.find { it.uid != selfUid }

        me?.avatarId?.let { player1Avatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(it)) }
        opponent?.avatarId?.let { player2Avatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(it)) }
    }

    private fun initializeViews() {
        player1Avatar = findViewById(R.id.player1Avatar)
        player2Avatar = findViewById(R.id.player2Avatar)
        player1Score = findViewById(R.id.player1Score)
        player2Score = findViewById(R.id.player2Score)
        currentPlayer = findViewById(R.id.currentPlayer)
        menuButton = findViewById(R.id.menuButton)
        actionButton = findViewById(R.id.actionButton)

        visualSeedManager = VisualSeedManager(this)
        for (i in 0 until 12) {
            val pitContainerId = resources.getIdentifier("pitContainer_$i", "id", packageName)
            pitContainers.add(findViewById(pitContainerId))
        }
    }

    private fun setupMenu() {
        menuButton.setImageResource(R.drawable.menu_icon)
        menuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            
            if (isSinglePlayer) {
                popupMenu.menuInflater.inflate(R.menu.menu_single_player, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_new_game -> {
                            // Navigate to Level Selection
                            startActivity(Intent(this, LevelSelectionActivity::class.java))
                            finish()
                            true
                        }
                        R.id.menu_multiplayer -> {
                            // Navigate to Create Room (Multiplayer Lobby)
                            startActivity(Intent(this, GameRoomActivity::class.java))
                            finish()
                            true
                        }
                        else -> false
                    }
                }
            } else {
                popupMenu.menuInflater.inflate(R.menu.menu_multiplayer, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_exit_game -> {
                            showExitConfirmationDialog()
                            true
                        }
                        else -> false
                    }
                }
            }
            popupMenu.show()
        }
    }

    private fun saveMultiplayerGame() {
        gameId?.let { id ->
            // Use the current game state from the listener or local variable if available
            // For now, we trigger a save via FirebaseManager
            // We need to pass the current state. Since we don't have a direct reference to the *latest* state object 
            // easily accessible here without potentially being stale, we can fetch it or use the last received update.
            // Better approach: FirebaseManager.saveGame(id) which reads from 'games/{id}' and writes to 'saved_games/{id}'
            FirebaseManager.saveGame(id) { success, message ->
                if (success) {
                    Toast.makeText(this, "Game Saved Successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showExitConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit_game, null)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        val exitButton = dialogView.findViewById<Button>(R.id.exitButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        exitButton.setOnClickListener {
            dialog.dismiss()
            if (isSinglePlayer) {
                // Single player: just exit locally
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            } else {
                // Multiplayer: Update game state to trigger navigation for both players
                lifecycleScope.launch {
                    try {
                        gameId?.let { id ->
                            FirebaseManager.gamesRef.child(id).child("navigationAction").setValue("mainMenu")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to set navigation action", e)
                        // Fallback: navigate anyway
                        startActivity(Intent(this@MainActivity, MainMenuActivity::class.java))
                        finish()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        NigerianThemeManager.applyThemeToActivity(this)
        updateBoardBackground() // Re-apply board background in case settings changed
        BackgroundMusicManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        BackgroundMusicManager.pauseBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        statusPopup?.dismiss()
        
        // Cancel the update processor to stop processing events
        updateProcessorJob?.cancel()
        
        // Close the channel to prevent new events
        gameUpdateChannel.close()
        
        gameStateListener?.let { listener ->
            gameId?.let { id ->
                FirebaseManager.removeListener(listener, FirebaseManager.gamesRef.child(id))
            }
        }
        soundManager.release()
        if (!isSinglePlayer) {

            // Destroy Agora Engine
            RtcEngine.destroy()
            rtcEngine = null
        }
    }

    // --- Agora Voice Chat Methods ---

    private fun setupVoiceChat(autoEnable: Boolean = false) {
        // Change Save Button to Mic Button
        actionButton.setImageResource(if (autoEnable) R.drawable.microphone_on else R.drawable.microphone_off)
        actionButton.setOnClickListener {
            onMicrophoneClicked()
        }

        initializeAgora()
        
        // Join channel automatically if permission is granted, otherwise wait for user to click mic
        if (checkPermissions()) {
             joinChannel()
             if (autoEnable) {
                 isVoiceEnabled = true
                 rtcEngine?.muteLocalAudioStream(false)
                 Toast.makeText(this, "Voice Chat On", Toast.LENGTH_SHORT).show()
             }
        } else if (autoEnable) {
            // Request permissions if auto-enable is requested
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS), PERMISSION_REQ_ID)
        }
    }

    private fun initializeAgora() {
        try {
            val config = io.agora.rtc2.RtcEngineConfig()
            config.mContext = baseContext
            config.mAppId = AGORA_APP_ID
            config.mEventHandler = mRtcEventHandler
            rtcEngine = RtcEngine.create(config)
            
            // Default to profile for gaming
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            
            // Default to muted
            rtcEngine?.muteLocalAudioStream(true)
            isVoiceEnabled = false
        } catch (e: Exception) {
            android.util.Log.e("Agora", "Error initializing Agora: ${e.message}")
        }
    }

    private fun joinChannel() {
        val channelName = gameId ?: "demo_channel"
        // Join with 0 to let Agora assign uid, or use a specific one if needed. 
        // For simplicity, we use 0 and let Agora handle it.
        // Token is null because App Certificate is disabled (App ID Only mode)
        rtcEngine?.joinChannel(null, channelName, "Extra Optional Data", 0)
        android.util.Log.d("Agora", "Joining channel: $channelName (No Token)")
    }

    private fun onMicrophoneClicked() {
        if (checkPermissions()) {
            isVoiceEnabled = !isVoiceEnabled
            rtcEngine?.muteLocalAudioStream(!isVoiceEnabled)
            
            if (isVoiceEnabled) {
                actionButton.setImageResource(R.drawable.microphone_on)
                Toast.makeText(this, "Voice Chat On", Toast.LENGTH_SHORT).show()
                // Ensure we are in the channel
                if (rtcEngine?.connectionState == Constants.CONNECTION_STATE_DISCONNECTED) {
                    joinChannel()
                }
            } else {
                actionButton.setImageResource(R.drawable.microphone_off)
                Toast.makeText(this, "Voice Chat Off", Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS), PERMISSION_REQ_ID)
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, turn on mic
                onMicrophoneClicked()
            } else {
                Toast.makeText(this, "Microphone permission needed for voice chat", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                android.util.Log.d("Agora", "Join channel success: $channel, uid: $uid")
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                android.util.Log.d("Agora", "User joined: $uid")
                Toast.makeText(applicationContext, "Opponent joined voice chat", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                android.util.Log.d("Agora", "User offline: $uid")
                 Toast.makeText(applicationContext, "Opponent left voice chat", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                android.util.Log.e("Agora", "Agora Error code: $err")
                // Error 110 = Invalid Token (ERR_INVALID_TOKEN)
                // Error 101 = Invalid App ID (ERR_INVALID_APP_ID)
                if (err == 110) {
                    Toast.makeText(applicationContext, "Voice Chat Error: Invalid Token. Check Agora Console.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
