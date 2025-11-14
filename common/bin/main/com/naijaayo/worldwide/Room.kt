package com.naijaayo.worldwide

import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Room(
    val roomId: String,
    val hostUid: String,
    val hostUsername: String,
    val hostAvatarId: String,
    val opponentUid: String? = null,
    val opponentUsername: String? = null,
    val opponentAvatarId: String? = null,
    val settings: RoomSettings? = null, // Creator's preferences applied to all players
    val difficulty: GameLevel = GameLevel.MEDIUM,
    val type: String = "public", // e.g., "public", "private", "qualifier"
    val players: List<String> = emptyList(),
    val state: GameState? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val gameConfig: GameConfig = GameConfig()
) : Serializable
