package com.naijaayo.worldwide

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val fromUsername: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: MessageType = MessageType.TEXT,
    val gameInvitation: GameInvitation? = null
)

@Serializable
enum class MessageType {
    TEXT,
    GAME_INVITATION
}

@Serializable
data class GameInvitation(
    val roomId: String,
    val hostUsername: String,
    val gameType: String, // "single_player" or "multiplayer"
    val difficulty: String
)