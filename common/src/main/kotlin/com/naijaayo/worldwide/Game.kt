package com.naijaayo.worldwide

import java.util.Date

data class Game(
    val gameId: String,
    val roomId: String,
    val moves: List<String> = emptyList(),
    val finalScore: String,
    val winnerUid: String? = null,
    val startedAt: Date = Date(),
    val endedAt: Date? = null
)

data class GameResult(
    val gameId: String? = null,
    val player1Id: String,
    val player2Id: String? = null, // null for AI games
    val player1Score: Int,
    val player2Score: Int,
    val winner: Int, // 1 = player1, 2 = player2, 0 = draw
    val isSinglePlayer: Boolean,
    val gameMode: String, // "single" or "multiplayer"
    val completedAt: String
)