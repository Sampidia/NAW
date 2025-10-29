package com.naijaayo.worldwide.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naijaayo.worldwide.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ViewModel for single-player Ayo game
 * Uses local game engine instead of network repository
 */
class SinglePlayerGameViewModel : ViewModel() {

    private val gameEngine = LocalGameEngine()

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _isProcessingMove = MutableLiveData<Boolean>()
    val isProcessingMove: LiveData<Boolean> = _isProcessingMove

    private val _gameMessage = MutableLiveData<String>()
    val gameMessage: LiveData<String> = _gameMessage

    init {
        // Do not start game in init, let MainActivity call startNewGame with level
    }

    /**
     * Starts a new single-player game with specified level
     */
    fun startNewGame(level: com.naijaayo.worldwide.GameLevel = com.naijaayo.worldwide.GameLevel.MEDIUM) {
        _gameState.value = gameEngine.createNewGame(level)
        _gameMessage.value = "Play Now!"
    }

    /**
     * Makes a move for the human player
     */
    fun makePlayerMove(pitIndex: Int) {
        val currentState = _gameState.value ?: return

        if (currentState.gameOver) {
            _gameMessage.value = "Game is over! Start a new game."
            return
        }

        if (currentState.currentPlayer != 1) {
            _gameMessage.value = "Wait for your turn!"
            return
        }

        _isProcessingMove.value = true

        viewModelScope.launch {
            try {
                val newState = gameEngine.makeMove(currentState, pitIndex, 1)

                if (newState != null) {
                    _gameState.value = newState

                    if (newState.gameOver) {
                        showGameOverMessage(newState)
                    } else {
                        // Show AI message and let it display briefly
                        _gameMessage.value = "Nice move, Ai thinking.."

                        // Delay to let AI message be visible
                        delay(1500)

                        // Make AI move with timeout protection (don't change message during AI move)
                        val aiMoveResult = withTimeoutOrNull(5000) { // 5 second timeout
                            makeAIMoveInternal()
                        }

                        if (aiMoveResult == null) {
                            _gameMessage.value = "AI took too long! Your turn."
                            // Reset to player's turn
                            _gameState.value = newState.copy(currentPlayer = 1)
                        } else {
                            // AI move completed successfully, show player turn message
                            _gameMessage.value = "Play Now!"
                        }
                    }
                } else {
                    _gameMessage.value = "Invalid move! Try another pit."
                }
            } catch (e: Exception) {
                _gameMessage.value = "Error making move: ${e.message}"
            } finally {
                _isProcessingMove.value = false
            }
        }
    }

    /**
     * Makes a move for the AI player (synchronous version for timeout)
     */
    private fun makeAIMoveWithTimeout() {
        val currentState = _gameState.value ?: return

        // Validate current game state
        if (currentState.gameOver) {
            _gameMessage.value = "Game is already over!"
            return
        }

        // Get valid moves for AI
        val validMoves = gameEngine.getValidMoves(currentState)
        if (validMoves.isEmpty()) {
            _gameMessage.value = "AI has no valid moves! Your turn."
            return
        }

        val newState = gameEngine.makeAIMove(currentState)

        if (newState != null) {
            _gameState.value = newState

            if (newState.gameOver) {
                showGameOverMessage(newState)
            } else {
                _gameMessage.value = "Play Now!"
            }
        } else {
            _gameMessage.value = "AI couldn't make a move! Your turn."
        }
    }

    /**
     * Makes a move for the AI player with timeout protection (legacy function)
     */
    private fun makeAIMove() {
        makeAIMoveWithTimeout()
    }

    /**
     * Internal AI move function that only updates game state, doesn't change messages
     */
    private fun makeAIMoveInternal() {
        val currentState = _gameState.value ?: return

        // Validate current game state
        if (currentState.gameOver) {
            return
        }

        // Get valid moves for AI
        val validMoves = gameEngine.getValidMoves(currentState)
        if (validMoves.isEmpty()) {
            return
        }

        val newState = gameEngine.makeAIMove(currentState)

        if (newState != null) {
            _gameState.value = newState

            if (newState.gameOver) {
                showGameOverMessage(newState)
            }
            // Don't change message here - let the calling function handle it
        }
    }

    /**
     * Shows appropriate game over message
     */
    private fun showGameOverMessage(gameState: GameState) {
        val message = when (gameState.winner) {
            1 -> "🎉 You win! Final score: ${gameState.player1Score} - ${gameState.player2Score}"
            2 -> "🤖 AI wins! Final score: ${gameState.player1Score} - ${gameState.player2Score}"
            else -> "🤝 It's a draw! Final score: ${gameState.player1Score} - ${gameState.player2Score}"
        }
        _gameMessage.value = message
    }

    /**
     * Gets valid moves for the current player (for UI highlighting)
     */
    fun getValidMoves(): List<Int> {
        val currentState = _gameState.value ?: return emptyList()
        return gameEngine.getValidMoves(currentState)
    }

    /**
     * Checks if a pit is a valid move for the current player
     */
    fun isValidMove(pitIndex: Int): Boolean {
        return getValidMoves().contains(pitIndex)
    }

    /**
     * Gets current game statistics
     */
    fun getGameStats(): GameStats {
        val currentState = _gameState.value ?: return GameStats(0, 0, 0, 0)

        val player1Seeds = (0..5).sumOf { currentState.pits[it] }
        val player2Seeds = (6..11).sumOf { currentState.pits[it] }

        return GameStats(
            player1Score = currentState.player1Score,
            player2Score = currentState.player2Score,
            player1Seeds = player1Seeds,
            player2Seeds = player2Seeds
        )
    }

    /**
     * Called when a game is completed to update leaderboard
     */
    fun onGameCompleted(gameState: GameState) {
        // For single-player, we can trigger leaderboard refresh here
        // The actual leaderboard update logic would be in the GameViewModel
        // For now, we'll just log the completion
        println("Single-player game completed - Player: ${gameState.player1Score}, AI: ${gameState.player2Score}, Winner: ${gameState.winner}")
    }

    /**
     * Updates the game state (for direct state updates from MainActivity)
     */
    fun updateGameState(gameState: GameState) {
        _gameState.value = gameState
    }
}

/**
 * Data class for game statistics
 */
data class GameStats(
    val player1Score: Int,
    val player2Score: Int,
    val player1Seeds: Int,
    val player2Seeds: Int
)
