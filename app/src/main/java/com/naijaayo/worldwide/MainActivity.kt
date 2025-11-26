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
import com.naijaayo.worldwide.ui.VisualSeedManager
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

    // --- UI components ---
    private lateinit var player1Avatar: ImageView
    private lateinit var player2Avatar: ImageView
    private lateinit var player1Score: TextView
    private lateinit var player2Score: TextView
    private lateinit var currentPlayer: TextView
    private lateinit var menuButton: ImageButton
    private val pitContainers = mutableListOf<androidx.constraintlayout.widget.ConstraintLayout>()
    private lateinit var visualSeedManager: VisualSeedManager
    private lateinit var soundManager: SoundManager

    // --- Previous State for Sound Logic ---
    private var previousGameState: com.naijaayo.worldwide.LocalGameState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
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
            updateUiForSinglePlayer(gameState)
        }
        // Add click listeners for player's pits
        for (i in 0..5) {
            pitContainers[i].setOnClickListener { 
                if (singlePlayerViewModel.isValidMove(i)) {
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
            currentFirebaseGame = game
            updateUiForMultiplayer(game)
        }
    }

    // --- UI Update Logic ---

    private fun updateUiForSinglePlayer(state: com.naijaayo.worldwide.LocalGameState) {
        playGameSounds(state)
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

    private fun playGameSounds(newState: com.naijaayo.worldwide.LocalGameState) {
        val oldState = previousGameState ?: return

        // Check for game over
        if (newState.gameOver && !oldState.gameOver) {
            soundManager.playWinSound()
            return
        }

        // Check for score increase (Capture)
        if (newState.player1Score > oldState.player1Score || newState.player2Score > oldState.player2Score) {
            soundManager.playCaptureSound()
        } 
        // Check for seed movement (any pit count change that isn't just a capture)
        else if (newState.pits != oldState.pits) {
            soundManager.playWoodSound()
        }
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

    private fun showGameOverDialog(winner: Int, p1Score: Int, p2Score: Int) {
        val message = when (winner) {
            0 -> "It's a draw!"
            1 -> "🎉 You Win!"
            else -> "You Lose!"
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
