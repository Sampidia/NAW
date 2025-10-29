package com.naijaayo.worldwide.game

import com.naijaayo.worldwide.GameState
import kotlin.random.Random

/**
 * Result of a capture operation containing pits array, captured seeds count, and specific pit indices
 */
data class CaptureResult(
    val pits: IntArray,
    val capturedSeeds: Int,
    val capturedPitIndices: List<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CaptureResult

        if (!pits.contentEquals(other.pits)) return false
        if (capturedSeeds != other.capturedSeeds) return false
        if (capturedPitIndices != other.capturedPitIndices) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pits.contentHashCode()
        result = 31 * result + capturedSeeds
        result = 31 * result + capturedPitIndices.hashCode()
        return result
    }
}

/**
 * Represents a single sowing step with the pit that received a seed
 */
data class SowingStep(
    val pitIndex: Int,
    val pitValueAfterSowing: Int,
    val isFinalStep: Boolean = false
)

/**
 * Complete move result containing intermediate sowing steps and final game state
 */
data class MoveResult(
    val sowingSteps: List<SowingStep>,
    val finalState: GameState,
    val captureResult: CaptureResult
)

/**
 * Local game engine for single-player Ayo game
 * Implements complete Ayo game logic without network dependencies
 */
class LocalGameEngine {

    private val easyEngine = EasyGameEngine()
    private val mediumEngine = MediumGameEngine()
    private val hardEngine = HardGameEngine()

    private fun getEngineForLevel(level: com.naijaayo.worldwide.GameLevel): Any {
        return when (level) {
            com.naijaayo.worldwide.GameLevel.EASY -> easyEngine
            com.naijaayo.worldwide.GameLevel.MEDIUM -> mediumEngine
            com.naijaayo.worldwide.GameLevel.HARD -> hardEngine
        }
    }

    /**
     * Makes a move for the specified player
     * @param gameState Current game state
     * @param pitIndex Index of the pit to play from (0-11)
     * @param player Player making the move (1 or 2)
     * @return New game state after the move, or null if move is invalid
     */
    fun makeMove(gameState: GameState, pitIndex: Int, player: Int): GameState? {
        return when (gameState.level) {
            com.naijaayo.worldwide.GameLevel.EASY -> easyEngine.makeMove(gameState, pitIndex, player)
            com.naijaayo.worldwide.GameLevel.MEDIUM -> mediumEngine.makeMove(gameState, pitIndex, player)
            com.naijaayo.worldwide.GameLevel.HARD -> hardEngine.makeMove(gameState, pitIndex, player)
        }
    }

    /**
     * Makes a move for the specified player with level-specific logic
     * @param gameState Current game state
     * @param pitIndex Index of the pit to play from (0-11)
     * @param player Player making the move (1 or 2)
     * @param level Game level for capture logic
     * @return New game state after the move, or null if move is invalid
     */
    fun makeMove(gameState: GameState, pitIndex: Int, player: Int, level: com.naijaayo.worldwide.GameLevel): GameState? {
        // Validate move
        if (!isValidMove(gameState, pitIndex, player)) {
            return null
        }

        // Create a copy of the game state to modify
        val newState = gameState.copy(
            pits = gameState.pits.copyOf(),
            level = level
        )

        // Execute the move
        return executeMove(newState, pitIndex, player)
    }

    /**
     * Makes a move with detailed intermediate steps for animation
     * @param gameState Current game state
     * @param pitIndex Index of the pit to play from (0-11)
     * @param player Player making the move (1 or 2)
     * @return MoveResult containing sowing steps and final state, or null if invalid
     */
    fun makeAnimatedMove(gameState: GameState, pitIndex: Int, player: Int): MoveResult? {
        return when (gameState.level) {
            com.naijaayo.worldwide.GameLevel.EASY -> easyEngine.makeAnimatedMove(gameState, pitIndex, player)
            com.naijaayo.worldwide.GameLevel.MEDIUM -> mediumEngine.makeAnimatedMove(gameState, pitIndex, player)
            com.naijaayo.worldwide.GameLevel.HARD -> hardEngine.makeAnimatedMove(gameState, pitIndex, player)
        }
    }

    /**
     * Calculates all intermediate sowing steps for animation purposes
     * @param originalPits Starting pit configuration
     * @param startingPitIndex Pit to sow from
     * @return List of sowing steps showing where each seed lands
     */
    private fun calculateSowingSteps(originalPits: IntArray, startingPitIndex: Int): List<SowingStep> {
        val steps = mutableListOf<SowingStep>()
        val simulationPits = originalPits.copyOf()
        val seeds = simulationPits[startingPitIndex]

        // Clear the starting pit
        simulationPits[startingPitIndex] = 0

        // Correct Ayo sowing: start from NEXT pit (no eternal skipping)
        var currentPit = startingPitIndex
        var seedsLeft = seeds

        while (seedsLeft > 0) {
            currentPit = (currentPit + 1) % 12

            // Ayo sowing logic: ALWAYS drop seed, even back into starting pit on subsequent laps
            simulationPits[currentPit]++

            // Record this sowing step
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
        val originalPits = pits.copyOf() // Store original state for capture detection
        val seeds = pits[pitIndex]

        // Clear the starting pit
        pits[pitIndex] = 0

        // Sow seeds counterclockwise - correct Ayo logic: drop into EVERY pit including wraps
        var currentPit = pitIndex
        var seedsLeft = seeds

        while (seedsLeft > 0) {
            currentPit = (currentPit + 1) % 12

            // Always drop seed, even back into starting pit on subsequent laps
            pits[currentPit]++
            seedsLeft--
        }

        // Handle captures - pass both original and updated pits for accurate detection
        val captureResult = handleCaptures(originalPits, pits, currentPit, player, gameState.level)
        val newPits = captureResult.pits
        val capturedSeeds = captureResult.capturedSeeds
        val capturedPitIndices = captureResult.capturedPitIndices

        // Update scores
        val player1Score = gameState.player1Score + (if (player == 1) capturedSeeds else 0)
        val player2Score = gameState.player2Score + (if (player == 2) capturedSeeds else 0)

        // Check for game end - create new GameState with capture data
        val gameEndResult = checkGameEnd(GameState(
            pits = newPits,
            player1Score = player1Score,
            player2Score = player2Score,
            currentPlayer = if (player == 1) 2 else 1,
            gameOver = false,
            lastCapturedPitIndices = capturedPitIndices
        ))

        return gameEndResult
    }

    /**
     * Handles Ayo capture logic based on level:
     * - Easy: capture 2 and 3 seeds
     * - Medium: capture 3 seeds (current logic)
     * - Hard: capture 4 seeds
     * If sowing creates any 2→3 transition (for medium), capture opponent pits based on level
     * @param originalPits Pits before sowing
     * @param currentPits Pits after sowing
     * @param lastPitIndex Where the last seed was sown (unused)
     * @param player Current player
     * @param level Game level
     * @return CaptureResult with updated pits, captured seeds, and captured pit indices
     */
    private fun handleCaptures(originalPits: IntArray, currentPits: IntArray, lastPitIndex: Int, player: Int, level: com.naijaayo.worldwide.GameLevel): CaptureResult {
        val pitsCopy = currentPits.copyOf()
        var capturedSeeds = 0
        val capturedPitIndices = mutableListOf<Int>()

        // Determine capture count based on level
        val captureCount = when (level) {
            com.naijaayo.worldwide.GameLevel.EASY -> 2..3
            com.naijaayo.worldwide.GameLevel.MEDIUM -> 3..3
            com.naijaayo.worldwide.GameLevel.HARD -> 4..4
        }

        // Check if sowing created any transition that enables chain capture
        // For medium, it's 2→3; generalize to any transition to capture count
        var chainCaptureEnabled = false
        for (i in 0 until 12) {
            if (isOpponentPit(i, player) &&
                originalPits[i] + 1 in captureCount &&  // original +1 is in capture range
                currentPits[i] in captureCount) {
                chainCaptureEnabled = true
                break
            }
        }

        if (chainCaptureEnabled) {
            // Capture opponent pits that are in the capture range and were below it
            for (i in 0 until 12) {
                if (isOpponentPit(i, player) &&
                    currentPits[i] in captureCount &&
                    originalPits[i] < currentPits[i]) {  // Sowing added seeds

                    capturedSeeds += currentPits[i]
                    capturedPitIndices.add(i)
                    pitsCopy[i] = 0
                }
            }
        }

        return CaptureResult(pitsCopy, capturedSeeds, capturedPitIndices)
    }

    /**
     * Captures a chain of adjacent 3-seed pits moving in specified direction
     * @param pits Working copy of pits array to modify
     * @param capturedPits Set of already captured pit indices
     * @param startPit Index of pit to start chain capture from
     * @param player Current player (used to determine opponent)
     * @param chainDirection Direction to move for chain (-1 for backward counterclockwise)
     */
    private fun captureChain(pits: IntArray, capturedPits: MutableSet<Int>, startPit: Int, player: Int, chainDirection: Int) {
        var currentPit = startPit
        val opponentSide = if (player == 1) 6..11 else 0..5

        // Capture starting pit
        if (pits[currentPit] == 3 && !capturedPits.contains(currentPit) && currentPit in opponentSide) {
            capturedPits.add(currentPit)
        }

        // Chain capture backward (to previous pit on opponent's side)
        // Move counterclockwise positionally (-1 index) within opponent pits
        currentPit = (currentPit + chainDirection + 12) % 12

        // Continue chain as long as there are adjacent 3-seed pits (backward direction)
        while (currentPit in opponentSide && pits[currentPit] == 3 && !capturedPits.contains(currentPit)) {
            capturedPits.add(currentPit)

            // Continue chain in same direction
            currentPit = (currentPit + chainDirection + 12) % 12

            // Stop if we've exited opponent pits (avoid wrapping around board)
            if (currentPit !in opponentSide) break
        }
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
        return when (gameState.level) {
            com.naijaayo.worldwide.GameLevel.EASY -> easyEngine.getValidMoves(gameState)
            com.naijaayo.worldwide.GameLevel.MEDIUM -> mediumEngine.getValidMoves(gameState)
            com.naijaayo.worldwide.GameLevel.HARD -> hardEngine.getValidMoves(gameState)
        }
    }

    /**
     * Makes a move for AI player using the level from gameState
     */
    fun makeAIMove(gameState: GameState): GameState? {
        return when (gameState.level) {
            com.naijaayo.worldwide.GameLevel.EASY -> easyEngine.makeAIMove(gameState)
            com.naijaayo.worldwide.GameLevel.MEDIUM -> mediumEngine.makeAIMove(gameState)
            com.naijaayo.worldwide.GameLevel.HARD -> hardEngine.makeAIMove(gameState)
        }
    }

    /**
     * Creates a new game state with specified level
     */
    fun createNewGame(level: com.naijaayo.worldwide.GameLevel = com.naijaayo.worldwide.GameLevel.MEDIUM): GameState {
        return GameState(
            pits = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
            player1Score = 0,
            player2Score = 0,
            currentPlayer = 1,
            gameOver = false,
            winner = null,
            level = level
        )
    }
}
