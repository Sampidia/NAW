package com.naijaayo.worldwide.game

import com.naijaayo.worldwide.GameState
import kotlin.random.Random

/**
 * Local game engine for single-player Ayo game
 * Implements complete Ayo game logic without network dependencies
 */
class LocalGameEngine {

    /**
     * Makes a move for the specified player
     * @param gameState Current game state
     * @param pitIndex Index of the pit to play from (0-11)
     * @param player Player making the move (1 or 2)
     * @return New game state after the move, or null if move is invalid
     */
    fun makeMove(gameState: GameState, pitIndex: Int, player: Int): GameState? {
        // Validate move
        if (!isValidMove(gameState, pitIndex, player)) {
            return null
        }

        // Create a copy of the game state to modify
        val newState = gameState.copy(
            pits = gameState.pits.copyOf()
        )

        // Execute the move
        return executeMove(newState, pitIndex, player)
    }

    /**
     * Validates if a move is legal
     */
    private fun isValidMove(gameState: GameState, pitIndex: Int, player: Int): Boolean {
        // Game must not be over
        if (gameState.gameOver) return false

        // Must be correct player's turn
        if (gameState.currentPlayer != player) return false

        // Pit must belong to current player
        val player1Pits = 0..5
        val player2Pits = 6..11
        val isPlayer1Pit = pitIndex in player1Pits
        val isPlayer2Pit = pitIndex in player2Pits

        if ((player == 1 && !isPlayer1Pit) || (player == 2 && !isPlayer2Pit)) {
            return false
        }

        // Pit must not be empty
        if (gameState.pits[pitIndex] == 0) return false

        // ✅ REMOVED: Must feed opponent requirement - players can now end on their own side

        return true
    }



    /**
     * Checks if a pit belongs to the opponent
     */
    private fun isOpponentPit(pitIndex: Int, player: Int): Boolean {
        return if (player == 1) {
            pitIndex in 6..11 // Player 2's pits
        } else {
            pitIndex in 0..5 // Player 1's pits
        }
    }

    /**
     * Executes the complete move logic
     */
    private fun executeMove(gameState: GameState, pitIndex: Int, player: Int): GameState {
        val pits = gameState.pits
        val seeds = pits[pitIndex]

        // Clear the starting pit
        pits[pitIndex] = 0

        // Sow seeds counterclockwise
        var currentPit = pitIndex
        var seedsLeft = seeds

        while (seedsLeft > 0) {
            currentPit = (currentPit + 1) % 12

            // Skip the original pit
            if (currentPit != pitIndex) {
                pits[currentPit]++
                seedsLeft--
            }
        }

        // Handle captures with UPDATED pits array
        val captureResult = handleCaptures(pits, currentPit, player)
        val newPits = captureResult.first
        val capturedSeeds = captureResult.second

        // Update scores
        val player1Score = gameState.player1Score + (if (player == 1) capturedSeeds else 0)
        val player2Score = gameState.player2Score + (if (player == 2) capturedSeeds else 0)

        // Check for game end
        val gameEndResult = checkGameEnd(GameState(
            pits = newPits,
            player1Score = player1Score,
            player2Score = player2Score,
            currentPlayer = if (player == 1) 2 else 1,
            gameOver = false
        ))

        return gameEndResult
    }

    /**
     * Handles seed capturing logic - captures ANY 3-seed pits in opponent's side
     */
    private fun handleCaptures(pits: IntArray, lastPitIndex: Int, player: Int): Pair<IntArray, Int> {
        val pitsCopy = pits.copyOf()
        var capturedSeeds = 0

        // Check ALL opponent's pits for 3-seed pits (regardless of last seed position)
        for (i in 0 until 12) {
            if (isOpponentPit(i, player) && pitsCopy[i] == 3) {
                // Start chain capture backwards from this 3-seed pit
                var currentIndex = i
                while (currentIndex >= 0 && currentIndex < 12) {
                    if (isOpponentPit(currentIndex, player) && pitsCopy[currentIndex] == 3) {
                        capturedSeeds += pitsCopy[currentIndex]
                        pitsCopy[currentIndex] = 0
                        currentIndex--
                    } else {
                        break
                    }
                }
            }
        }

        return Pair(pitsCopy, capturedSeeds)
    }

    /**
     * Checks if the game should end and determines winner
     */
    private fun checkGameEnd(gameState: GameState): GameState {
        val pits = gameState.pits

        // Count seeds on each side
        val player1Seeds = (0..5).sumOf { pits[it] }
        val player2Seeds = (6..11).sumOf { pits[it] }
        val totalSeeds = player1Seeds + player2Seeds

        // Game ends if one side is empty OR total seeds ≤ 3
        val gameOver = player1Seeds == 0 || player2Seeds == 0 || totalSeeds <= 3

        if (gameOver) {
            // ✅ CORRECT: Remaining seeds are discarded - only captured seeds count
            val finalPlayer1Score = gameState.player1Score  // Only captured seeds
            val finalPlayer2Score = gameState.player2Score  // Only captured seeds

            val winner = when {
                finalPlayer1Score > finalPlayer2Score -> 1
                finalPlayer2Score > finalPlayer1Score -> 2
                else -> null
            }

            return gameState.copy(
                pits = pits,
                player1Score = finalPlayer1Score,
                player2Score = finalPlayer2Score,
                currentPlayer = 0, // No current player
                gameOver = true,
                winner = winner
            )
        }

        return gameState
    }

    /**
     * Gets all valid moves for the current player
     */
    fun getValidMoves(gameState: GameState): List<Int> {
        val validMoves = mutableListOf<Int>()
        val player = gameState.currentPlayer

        val pitsRange = if (player == 1) 0..5 else 6..11

        for (pitIndex in pitsRange) {
            if (isValidMove(gameState, pitIndex, player)) {
                validMoves.add(pitIndex)
            }
        }

        return validMoves
    }

    /**
     * Makes a move for AI player
     */
    fun makeAIMove(gameState: GameState): GameState? {
        val validMoves = getValidMoves(gameState)
        if (validMoves.isEmpty()) return null

        // Simple AI: choose a random valid move
        // TODO: Implement more sophisticated AI strategy
        val randomMove = validMoves[Random.nextInt(validMoves.size)]
        return makeMove(gameState, randomMove, gameState.currentPlayer)
    }

    /**
     * Creates a new game state
     */
    fun createNewGame(): GameState {
        return GameState(
            pits = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
            player1Score = 0,
            player2Score = 0,
            currentPlayer = 1,
            gameOver = false,
            winner = null
        )
    }
}
