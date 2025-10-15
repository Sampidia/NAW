package com.naijaayo.worldwide

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Room(
    val roomId: String,
    val hostUid: String,
    val type: String, // e.g., "public", "private", "qualifier"
    val players: List<String> = emptyList(),
    val state: GameState? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val gameConfig: GameConfig = GameConfig()
)
