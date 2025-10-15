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