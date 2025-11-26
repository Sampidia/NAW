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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.game.SinglePlayerGameViewModel
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.network.Game
import com.naijaayo.worldwide.sound.BackgroundMusicManager
import com.naijaayo.worldwide.sound.SoundManager
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.game.CaptureResult
import com.naijaayo.worldwide.game.MoveResult
import com.naijaayo.worldwide.game.SowingStep
import com.naijaayo.worldwide.ui.VisualSeedManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // --- Game Mode and State ---
    private var isSinglePlayer: Boolean = true
    private var gameLevel: GameLevel = GameLevel.MEDIUM
    private var gameId: String? = null
    private var selfUid: String? = null

    // Single Player ViewModel
    private val singlePlayerViewModel: SinglePlayerGameViewModel by viewModels()

    // Firebase
    private var gameStateListener: ValueEventListener? = null
    private var currentFirebaseGame: Game? = null
    private var previousFirebaseGame: Game? = null // To detect changes
    private val localGameEngine = com.naijaayo.worldwide.game.LocalGameEngine() // For calculating MP animations

    // --- UI components ---

    private lateinit var player1Avatar: ImageView
    private lateinit var player2Avatar: ImageView
    private lateinit var player1Score: TextView
    private lateinit var player2Score: TextView
    private lateinit var currentPlayer: TextView
    private lateinit var menuButton: ImageButton
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
        setContentView(R.layout.activity_main)

        // --- Initializations ---
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)
        AvatarPreferenceManager.initialize(this)
        BoardManager.initialize(this) // Initialize BoardManager
        BackgroundMusicManager.initialize(this)
        soundManager = SoundManager(this).apply { loadSounds() }

        initializeViews()
        setupMenu()
        updateBoardBackground() // Apply saved board background

        selfUid = FirebaseManager.auth.currentUser?.uid

        // --- Game Mode Detection ---
        gameId = intent.getStringExtra("GAME_ID")
        isSinglePlayer = gameId == null

        if (isSinglePlayer) {
            val levelName = intent.getStringExtra("level") ?: "MEDIUM"
            gameLevel = try { GameLevel.valueOf(levelName) } catch (e: Exception) { GameLevel.MEDIUM }
            setupSinglePlayerMode()
        } else {
            setupMultiplayerMode(gameId!!)
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
        gameStateListener = FirebaseManager.listenForGameStateUpdates(gameId) { game ->
            if (game == null) {
                Toast.makeText(this, "Game room is no longer available.", Toast.LENGTH_LONG).show()
                finish()
                return@listenForGameStateUpdates
            }
            
            // Store previous game state to detect moves
            val oldGame = currentFirebaseGame
            currentFirebaseGame = game
            
            if (oldGame != null && !isAnimating) {
                // Detect if a move happened
                detectAndAnimateMultiplayerMove(oldGame, game)
            } else {
                // Initial load or no animation needed
                updateUiForMultiplayer(game)
            }
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

    private fun updateUiForMultiplayer(game: Game) {
        // Map network GameState to local GameState for sound logic reuse if possible, 
        // or implement separate sound logic. For now, simple sound logic for MP.
        // (Skipping complex MP sound logic for brevity, focusing on Single Player as requested/tested)

        if (game.gameState.isGameOver) {
            val myPlayerNumber = if (game.players.values.first().uid == selfUid) 1 else 2
            val winnerNumber = when (game.gameState.winnerUid) {
                selfUid -> myPlayerNumber
                null -> 0
                else -> 3 - myPlayerNumber // The other player
            }
            showGameOverDialog(winnerNumber, -1, -1) // Scores are not tracked in multiplayer yet
            return
        }

        val me = game.players[selfUid]
        val opponent = game.players.values.find { it.uid != selfUid }
        val isMyTurn = game.gameState.nextPlayerUid == selfUid
        val currentPlayerNum = if (isMyTurn) (if (game.players.values.first().uid == selfUid) 1 else 2) else (if (game.players.values.first().uid == selfUid) 2 else 1)

        player1Score.text = me?.displayName ?: "You"
        player2Score.text = opponent?.displayName ?: "Opponent"
        currentPlayer.text = if (isMyTurn) "Your Turn" else "Opponent's Turn"

        game.gameState.board.forEachIndexed { index, count ->
            val pitTextId = resources.getIdentifier("pitText_$index", "id", packageName)
            visualSeedManager.updatePitSeeds(pitContainers[index].findViewById(pitTextId), count, VisualSeedManager.AnimationType.NONE)
            
            // Update pit background
            updatePitBackground(index, currentPlayerNum)
        }

        setupMultiplayerClickListeners(game, isMyTurn)
        updateMultiplayerAvatars(game)
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

    private fun detectAndAnimateMultiplayerMove(oldGame: Game, newGame: Game) {
        // If game over, just update UI
        if (newGame.gameState.isGameOver) {
            updateUiForMultiplayer(newGame)
            return
        }

        // Find which pit changed to 0 (the move source)
        // The player who moved is the one whose turn it WAS
        val previousPlayerUid = oldGame.gameState.nextPlayerUid
        val myPlayerNumber = if (oldGame.players.values.first().uid == selfUid) 1 else 2
        val wasMyTurn = previousPlayerUid == selfUid
        
        // Determine player number (1 or 2) for the move
        val playerNum = if (wasMyTurn) myPlayerNumber else (if (myPlayerNumber == 1) 2 else 1)
        
        // Find the pit that became empty (or was clicked)
        // In Ayo, the starting pit becomes empty (0)
        var movedPitIndex = -1
        
        // Simple detection: find pit that went to 0 and had seeds before
        // Note: This is a heuristic. For robust MP, we should send the move index in Firebase
        for (i in 0..11) {
            if (oldGame.gameState.board[i] > 0 && newGame.gameState.board[i] == 0) {
                // Also check if it belongs to the player who moved
                val isPlayer1Pit = i in 0..5
                val isPlayer2Pit = i in 6..11
                
                if ((playerNum == 1 && isPlayer1Pit) || (playerNum == 2 && isPlayer2Pit)) {
                    movedPitIndex = i
                    break
                }
            }
        }

        if (movedPitIndex != -1) {
            // Reconstruct local state to calculate animation steps
            val localState = com.naijaayo.worldwide.LocalGameState(
                pits = oldGame.gameState.board.toIntArray(),
                player1Score = 0, // Score not tracked in MP yet
                player2Score = 0,
                currentPlayer = playerNum,
                gameOver = false
            )

            // Calculate animation steps locally
            val moveResult = localGameEngine.makeAnimatedMove(localState, movedPitIndex, playerNum)
            
            if (moveResult != null) {
                // Animate!
                animateMultiplayerMoveSequence(moveResult, newGame)
            } else {
                updateUiForMultiplayer(newGame)
            }
        } else {
            updateUiForMultiplayer(newGame)
        }
    }

    private fun animateMultiplayerMoveSequence(moveResult: MoveResult, finalGame: Game) {
        isAnimating = true
        animationJob?.cancel()
        
        animationJob = lifecycleScope.launch {
            try {
                // Phase 0: Animate picking up seeds (emptying the starting pit)
                if (moveResult.sowingSteps.isNotEmpty()) {
                    // The starting pit is the one before the first sowing step, wrapping around if needed
                    // But simpler: we know the move started from a pit that is now empty or different.
                    // Actually, we can infer the starting pit from the sowing steps logic or pass it.
                    // Since we don't have the start index explicitly in MoveResult, we can infer it:
                    // In Ayo, you pick from a pit and sow into the NEXT pits.
                    // So the pit BEFORE the first sowing step is likely the start pit.
                    val firstStepIndex = moveResult.sowingSteps.first().pitIndex
                    val startPitIndex = if (firstStepIndex == 0) 11 else firstStepIndex - 1
                    
                    animateSeedPickup(startPitIndex)
                }

                animateSowingSteps(moveResult.sowingSteps)
                animateCaptureSteps(moveResult.captureResult)
                updateUiForMultiplayer(finalGame)
            } catch (e: Exception) {
                updateUiForMultiplayer(finalGame)
            } finally {
                isAnimating = false
            }
        }
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
            
            // Wait for animation to complete - Increased to 300ms for better followability
            delay(300)
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

    private fun setupMultiplayerClickListeners(game: Game, isMyTurn: Boolean) {
        val myPlayerNumber = if (game.players.values.first().uid == selfUid) 1 else 2

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

    private fun performMultiplayerMove(pitIndex: Int) {
        lifecycleScope.launch {
            gameId?.let { FirebaseManager.makeMove(it, pitIndex) }
        }
    }

    private fun showGameStatus(message: String) {
        // Safety check: ensure activity is in valid state
        if (isFinishing || isDestroyed) return
        
        val state = singlePlayerViewModel.gameState.value
        val isPlayer1Turn = state?.currentPlayer == 1

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
                val titleText = popupView.findViewById<TextView>(R.id.dialogCurrentPlayer)
                val messageText = popupView.findViewById<TextView>(R.id.dialogGameMessage)
                
                if (isPlayer1Turn) {
                    titleText.text = "Your Turn"
                    messageText.text = "Make Move"
                } else {
                    titleText.text = "Ai Turn"
                    messageText.text = "Ai thinking"
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

                if (isPlayer1Turn) {
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
        val message = when (winner) {
            0 -> "It's a draw!"
            1 -> "🎉 You Win!"
            else -> "You Lose!"
        }

        // Play win sound if player won
        if (winner == 1) {
            soundManager.playWinSound()
        }

        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("Play Again") { _, _ ->
                if (isSinglePlayer) {
                    previousGameState = null // Reset state tracking
                    singlePlayerViewModel.startNewGame(gameLevel) 
                } else finish()
            }
            .setNegativeButton("Main Menu") { _, _ -> finish() }
            .setCancelable(false)
            .show()

        // --- UPDATE LEADERBOARD LOGIC ---
        val user = FirebaseManager.auth.currentUser
        if (user != null && winner == 1) { // If human player won
            lifecycleScope.launch {
                val scoreUpdate = 10L // Award 10 points for a win
                val displayName = user.displayName ?: "Player"
                val avatarId = AvatarPreferenceManager.getUserAvatar()

                if (isSinglePlayer) {
                    FirebaseManager.updateSinglePlayerScore(scoreUpdate, displayName, avatarId)
                } else {
                    FirebaseManager.updateMultiplayerScore(scoreUpdate, displayName, avatarId)
                }
            }
        }
    }

    private fun updateMultiplayerAvatars(game: Game) {
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

        visualSeedManager = VisualSeedManager(this)
        for (i in 0 until 12) {
            val pitContainerId = resources.getIdentifier("pitContainer_$i", "id", packageName)
            pitContainers.add(findViewById(pitContainerId))
        }
    }

    private fun setupMenu() {
        menuButton.setOnClickListener { view ->
            val popupMenu = PopupMenu(this, view)
            popupMenu.menuInflater.inflate(R.menu.game_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_new_game -> {
                        if (isSinglePlayer) {
                            previousGameState = null
                            singlePlayerViewModel.startNewGame(gameLevel) 
                        } else finish()
                        true
                    }
                    R.id.menu_profile -> {
                        startActivity(Intent(this, ProfileActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
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
        gameStateListener?.let { listener ->
            gameId?.let { id ->
                FirebaseManager.removeListener(listener, FirebaseManager.gamesRef.child(id))
            }
        }
        soundManager.release()
        if (!isSinglePlayer) {
            FirebaseManager.leaveGameSession()
        }
    }
}
