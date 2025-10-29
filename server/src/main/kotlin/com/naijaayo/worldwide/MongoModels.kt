package com.naijaayo.worldwide

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import java.time.LocalDateTime

// User Model
@Serializable
data class MongoUser(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val username: String,
    val email: String,
    val passwordHash: String,
    val avatarId: String = "ayo",
    val createdAt: String = LocalDateTime.now().toString(),
    val lastSeen: String = LocalDateTime.now().toString(),
    val isOnline: Boolean = false
)

// Friend Model
@Serializable
data class MongoFriend(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val userId: String,
    val friendId: String,
    val friendUsername: String,
    val friendEmail: String,
    val friendAvatarId: String,
    val status: FriendStatus = FriendStatus.ACCEPTED,
    val createdAt: String = LocalDateTime.now().toString(),
    val lastSeen: String = LocalDateTime.now().toString(),
    val isOnline: Boolean = false
)

// Friend Request Model
@Serializable
data class MongoFriendRequest(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val fromUserId: String,
    val toUserId: String,
    val fromUsername: String,
    val fromEmail: String,
    val fromAvatarId: String,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: String = LocalDateTime.now().toString(),
    val message: String? = null
)

// Message Model
@Serializable
data class MongoMessage(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val fromUserId: String,
    val toUserId: String,
    val fromUsername: String,
    val content: String,
    val timestamp: String = LocalDateTime.now().toString(),
    val type: MessageType = MessageType.TEXT,
    val gameInvitationRoomId: String? = null,
    val gameInvitationHostUsername: String? = null,
    val gameInvitationGameType: String? = null,
    val gameInvitationDifficulty: String? = null
)

// Saved Game Model
@Serializable
data class MongoSavedGame(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val userId: String,
    val gameMode: String, // "single_player" or "multiplayer"
    val opponentId: String? = null,
    val opponentUsername: String? = null,
    val opponentAvatarId: String? = null,
    val gameStateJson: String,
    val createdAt: String = LocalDateTime.now().toString(),
    val lastPlayed: String = LocalDateTime.now().toString()
)

// Leaderboard Model
@Serializable
data class MongoLeaderboardEntry(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val userId: String,
    val username: String,
    val avatarId: String,
    val gameMode: String, // "single_player" or "multiplayer"
    val eloRating: Int = 1200,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val gamesDrawn: Int = 0,
    val winStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val lastPlayed: String = LocalDateTime.now().toString()
)

// Room Model
@Serializable
data class MongoRoom(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val roomId: String,
    val hostUid: String,
    val hostUsername: String,
    val hostAvatarId: String,
    val difficulty: GameLevel,
    val type: String = "public",
    val status: String = "waiting", // waiting, playing, finished
    val players: String, // JSON array of player IDs
    val maxPlayers: Int = 2,
    val createdAt: String = LocalDateTime.now().toString(),
    val settingsJson: String? = null // RoomSettings JSON
)

// Game Result Model
@Serializable
data class MongoGameResult(
    @SerialName("_id")
    val id: String = ObjectId().toString(),
    val gameId: String,
    val player1Id: String,
    val player1Username: String,
    val player1Score: Int,
    val player2Id: String? = null,
    val player2Username: String? = null,
    val player2Score: Int,
    val winnerId: String? = null,
    val gameMode: String, // "single_player" or "multiplayer"
    val difficulty: GameLevel,
    val duration: Int? = null, // in seconds
    val completedAt: String = LocalDateTime.now().toString()
)