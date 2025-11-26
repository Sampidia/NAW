package com.naijaayo.worldwide

/**
 * Local game state for single-player and multiplayer game logic
 * Renamed from GameState to LocalGameState to avoid conflict with Firebase GameState
 */
data class LocalGameState(
    val pits: IntArray,
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val currentPlayer: Int = 1,
    val gameOver: Boolean = false,
    val winner: Int? = null,
    val level: GameLevel = GameLevel.MEDIUM,
    val lastCapturedPitIndices: List<Int> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalGameState

        if (!pits.contentEquals(other.pits)) return false
        if (player1Score != other.player1Score) return false
        if (player2Score != other.player2Score) return false
        if (currentPlayer != other.currentPlayer) return false
        if (gameOver != other.gameOver) return false
        if (winner != other.winner) return false
        if (level != other.level) return false
        if (lastCapturedPitIndices != other.lastCapturedPitIndices) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pits.contentHashCode()
        result = 31 * result + player1Score
        result = 31 * result + player2Score
        result = 31 * result + currentPlayer
        result = 31 * result + gameOver.hashCode()
        result = 31 * result + (winner ?: 0)
        result = 31 * result + level.hashCode()
        result = 31 * result + lastCapturedPitIndices.hashCode()
        return result
    }
}

/**
 * Game difficulty levels
 */
enum class GameLevel {
    EASY,    // Capture 2 or 3 seeds
    MEDIUM,  // Capture 3 seeds
    HARD     // Capture 4 seeds
}
