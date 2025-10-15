package com.naijaayo.worldwide

/**
 * This file contains the core game logic for the Ayo game.
 */

fun playMove(currentState: GameState, pitIndex: Int): GameState {
    // 1. Validate the move
    val isPlayer1 = currentState.currentPlayer == 1
    val playerPitsRange = if (isPlayer1) 0..5 else 6..11
    if (pitIndex !in playerPitsRange || currentState.pits[pitIndex] == 0) {
        return currentState // Invalid move: Not player's pit or it's empty
    }

    val newPits = currentState.pits.clone()
    var seedsToSow = newPits[pitIndex]
    newPits[pitIndex] = 0

    // 2. Sow the seeds
    var lastSownPit = -1
    var currentPit = pitIndex
    while (seedsToSow > 0) {
        currentPit = (currentPit + 1) % 12
        newPits[currentPit]++
        seedsToSow--
    }
    lastSownPit = currentPit

    // 3. Handle capturing
    var capturedSeeds = 0
    val opponentPitsRange = if (isPlayer1) 6..11 else 0..5
    var capturePitIndex = lastSownPit

    // Check if the last sown pit is on the opponent's side and triggers a capture
    if (lastSownPit in opponentPitsRange) {
        // Start a chain capture backwards
        while (capturePitIndex in opponentPitsRange && newPits[capturePitIndex] == 3) {
            capturedSeeds += newPits[capturePitIndex]
            newPits[capturePitIndex] = 0
            capturePitIndex--
        }
    }

    var newPlayer1Score = currentState.player1Score
    var newPlayer2Score = currentState.player2Score
    if (isPlayer1) {
        newPlayer1Score += capturedSeeds
    } else {
        newPlayer2Score += capturedSeeds
    }

    // 4. Check for game over (starvation)
    val player1PitsSum = newPits.slice(0..5).sum()
    val player2PitsSum = newPits.slice(6..11).sum()
    var isGameOver = false
    var winner: Int? = null

    if (player1PitsSum == 0 || player2PitsSum == 0) {
        isGameOver = true
        // The player who cleared their side captures the remaining seeds
        newPlayer1Score += player1PitsSum
        newPlayer2Score += player2PitsSum
        // Clear the board
        for (i in newPits.indices) {
            newPits[i] = 0
        }

        winner = when {
            newPlayer1Score > newPlayer2Score -> 1
            newPlayer2Score > newPlayer1Score -> 2
            else -> 0 // Draw
        }
    }

    // 5. Determine the next player
    val nextPlayer = if (currentState.currentPlayer == 1) 2 else 1

    return currentState.copy(
        pits = newPits,
        player1Score = newPlayer1Score,
        player2Score = newPlayer2Score,
        currentPlayer = nextPlayer,
        gameOver = isGameOver,
        winner = winner
    )
}
