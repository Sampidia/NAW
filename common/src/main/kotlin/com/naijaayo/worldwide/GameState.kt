package com.naijaayo.worldwide

data class GameState(
    val pits: IntArray = intArrayOf(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4),
    val player1Score: Int = 0,
    val player2Score: Int = 0,
    val currentPlayer: Int = 1,
    val gameOver: Boolean = false,
    val winner: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (!pits.contentEquals(other.pits)) return false
        if (player1Score != other.player1Score) return false
        if (player2Score != other.player2Score) return false
        if (currentPlayer != other.currentPlayer) return false
        if (gameOver != other.gameOver) return false
        if (winner != other.winner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pits.contentHashCode()
        result = 31 * result + player1Score
        result = 31 * result + player2Score
        result = 31 * result + currentPlayer
        result = 31 * result + gameOver.hashCode()
        result = 31 * result + (winner ?: 0)
        return result
    }
}