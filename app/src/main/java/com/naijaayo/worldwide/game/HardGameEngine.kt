package com.naijaayo.worldwide.game

import com.naijaayo.worldwide.GameState
import kotlin.random.Random

/**
 * Game engine for HARD level - captures pits with exactly 4 seeds
 */
class HardGameEngine {

    /**
     * Makes a move for the specified player
     */
    fun makeMove(gameState: GameState, pitIndex: Int, player: Int): GameState? {
        if (!isValidMove(gameState, pitIndex, player)) return null

        val newState = gameState.copy(pits = gameState.pits.copyOf())
        return executeMove(newState, pitIndex, player)
    }

    /**
     * Makes a move with detailed intermediate steps for animation
     */
    fun makeAnimatedMove(gameState: GameState, pitIndex: Int, player: Int): MoveResult? {
        if (!isValidMove(gameState, pitIndex, player)) return null

        val sowingSteps = calculateSowingSteps(gameState.pits, pitIndex)
        val finalState = makeMove(gameState, pitIndex, player)

        if (finalState == null) return null

        val captureResult = CaptureResult(
            pits = finalState.pits,
            capturedSeeds = (finalState.player1Score - gameState.player1Score) +
                           (finalState.player2Score - gameState.player2Score),
            capturedPitIndices = finalState.lastCapturedPitIndices
        )

        return MoveResult(sowingSteps, finalState, captureResult)
    }

    private fun executeMove(gameState: GameState, pitIndex: Int, player: Int): GameState {
        val pits = gameState.pits
        val originalPits = pits.copyOf()
        val seeds = pits[pitIndex]

        pits[pitIndex] = 0

        var currentPit = pitIndex
        var seedsLeft = seeds

        while (seedsLeft > 0) {
            currentPit = (currentPit + 1) % 12
            pits[currentPit]++
            seedsLeft--
        }

        val captureResult = handleCaptures(originalPits, pits, currentPit, player)
        val capturedSeeds = captureResult.capturedSeeds
        val capturedPitIndices = captureResult.capturedPitIndices

        val player1Score = gameState.player1Score + if (player == 1) capturedSeeds else 0
        val player2Score = gameState.player2Score + if (player == 2) capturedSeeds else 0

        return checkGameEnd(GameState(
            pits = captureResult.pits,
            player1Score = player1Score,
            player2Score = player2Score,
            currentPlayer = if (player == 1) 2 else 1,
            gameOver = false,
            lastCapturedPitIndices = capturedPitIndices,
            level = gameState.level
        ))
    }

    /**
     * HARD LEVEL: Captures pits with exactly 4 seeds if sowing created 3→4 transition
     */
    private fun handleCaptures(originalPits: IntArray, currentPits: IntArray, lastPitIndex: Int, player: Int): CaptureResult {
        val pitsCopy = currentPits.copyOf()
        var capturedSeeds = 0
        val capturedPitIndices = mutableListOf<Int>()

        // Check if sowing created any 3→4 transition
        var captureEnabled = false
        for (i in 0 until 12) {
            if (isOpponentPit(i, player) &&
                originalPits[i] + 1 == 4 && currentPits[i] == 4) {
                captureEnabled = true
                break
            }
        }

        if (captureEnabled) {
            // Capture opponent pits that are now exactly 4 seeds and were increased by sowing
            for (i in 0 until 12) {
                if (isOpponentPit(i, player) &&
                    currentPits[i] == 4 &&
                    originalPits[i] < currentPits[i]) {
                    capturedSeeds += currentPits[i]
                    capturedPitIndices.add(i)
                    pitsCopy[i] = 0
                }
            }
        }

        return CaptureResult(pitsCopy, capturedSeeds, capturedPitIndices)
    }

    private fun calculateSowingSteps(originalPits: IntArray, startingPitIndex: Int): List<SowingStep> {
        val steps = mutableListOf<SowingStep>()
        val simulationPits = originalPits.copyOf()
        val seeds = simulationPits[startingPitIndex]

        simulationPits[startingPitIndex] = 0

        var currentPit = startingPitIndex
        var seedsLeft = seeds

        while (seedsLeft > 0) {
            currentPit = (currentPit + 1) % 12
            simulationPits[currentPit]++

            steps.add(SowingStep(
                pitIndex = currentPit,
                pitValueAfterSowing = simulationPits[currentPit],
                isFinalStep = (seedsLeft == 1)
            ))

            seedsLeft--
        }

        return steps
    }

    private fun isValidMove(gameState: GameState, pitIndex: Int, player: Int): Boolean {
        if (gameState.gameOver) return false
        if (gameState.currentPlayer != player) return false

        val isPlayer1Pit = pitIndex in 0..5
        val isPlayer2Pit = pitIndex in 6..11

        if ((player == 1 && !isPlayer1Pit) || (player == 2 && !isPlayer2Pit)) return false
        if (gameState.pits[pitIndex] == 0) return false

        return true
    }

    private fun isOpponentPit(pitIndex: Int, player: Int): Boolean {
        return if (player == 1) pitIndex in 6..11 else pitIndex in 0..5
    }

    private fun checkGameEnd(gameState: GameState): GameState {
        val pits = gameState.pits
        val player1Seeds = (0..5).sumOf { pits[it] }
        val player2Seeds = (6..11).sumOf { pits[it] }
        val totalSeeds = player1Seeds + player2Seeds

        val gameOver = player1Seeds == 0 || player2Seeds == 0 || totalSeeds <= 3

        if (gameOver) {
            val winner = when {
                gameState.player1Score > gameState.player2Score -> 1
                gameState.player2Score > gameState.player1Score -> 2
                else -> null
            }

            return gameState.copy(
                currentPlayer = 0,
                gameOver = true,
                winner = winner
            )
        }

        return gameState
    }

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

    fun makeAIMove(gameState: GameState): GameState? {
        val validMoves = getValidMoves(gameState)
        if (validMoves.isEmpty()) return null

        val randomMove = validMoves[Random.nextInt(validMoves.size)]
        return makeMove(gameState, randomMove, gameState.currentPlayer)
    }
}