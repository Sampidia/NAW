package com.naijaayo.worldwide

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.LocalDateTime

object Users : Table() {
    val id = varchar("id", 50).primaryKey()
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val avatarId = varchar("avatar_id", 50).default("ayo")
    val createdAt = timestamp("created_at").default(LocalDateTime.now())
    val lastSeen = timestamp("last_seen").default(LocalDateTime.now())
    val isOnline = bool("is_online").default(false)
}

object Friends : Table() {
    val id = varchar("id", 50).primaryKey()
    val userId = varchar("user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val friendId = varchar("friend_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val friendUsername = varchar("friend_username", 50)
    val friendEmail = varchar("friend_email", 255)
    val friendAvatarId = varchar("friend_avatar_id", 50)
    val status = enumeration<FriendStatus>("status").default(FriendStatus.ACCEPTED)
    val createdAt = timestamp("created_at").default(LocalDateTime.now())
    val lastSeen = timestamp("last_seen").default(LocalDateTime.now())
    val isOnline = bool("is_online").default(false)

    init {
        uniqueIndex(userId, friendId)
    }
}

object FriendRequests : Table() {
    val id = varchar("id", 50).primaryKey()
    val fromUserId = varchar("from_user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val toUserId = varchar("to_user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val fromUsername = varchar("from_username", 50)
    val fromEmail = varchar("from_email", 255)
    val fromAvatarId = varchar("from_avatar_id", 50)
    val status = enumeration<FriendRequestStatus>("status").default(FriendRequestStatus.PENDING)
    val createdAt = timestamp("created_at").default(LocalDateTime.now())
    val message = text("message").nullable()
}

object Messages : Table() {
    val id = varchar("id", 50).primaryKey()
    val fromUserId = varchar("from_user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val toUserId = varchar("to_user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val fromUsername = varchar("from_username", 50)
    val content = text("content")
    val timestamp = timestamp("timestamp").default(LocalDateTime.now())
    val type = enumeration<MessageType>("type").default(MessageType.TEXT)
    val gameInvitationRoomId = varchar("game_invitation_room_id", 50).nullable()
    val gameInvitationHostUsername = varchar("game_invitation_host_username", 50).nullable()
    val gameInvitationGameType = varchar("game_invitation_game_type", 50).nullable()
    val gameInvitationDifficulty = varchar("game_invitation_difficulty", 50).nullable()
}

object SavedGames : Table() {
    val id = varchar("id", 50).primaryKey()
    val userId = varchar("user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val gameMode = varchar("game_mode", 20) // "single_player" or "multiplayer"
    val opponentId = varchar("opponent_id", 50).nullable()
    val opponentUsername = varchar("opponent_username", 50).nullable()
    val opponentAvatarId = varchar("opponent_avatar_id", 50).nullable()
    val gameStateJson = text("game_state_json")
    val createdAt = timestamp("created_at").default(LocalDateTime.now())
    val lastPlayed = timestamp("last_played").default(LocalDateTime.now())
}

object Leaderboard : Table() {
    val id = varchar("id", 50).primaryKey()
    val userId = varchar("user_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val username = varchar("username", 50)
    val avatarId = varchar("avatar_id", 50)
    val gameMode = varchar("game_mode", 20) // "single_player" or "multiplayer"
    val eloRating = integer("elo_rating").default(1200)
    val gamesPlayed = integer("games_played").default(0)
    val gamesWon = integer("games_won").default(0)
    val gamesLost = integer("games_lost").default(0)
    val gamesDrawn = integer("games_drawn").default(0)
    val winStreak = integer("win_streak").default(0)
    val bestWinStreak = integer("best_win_streak").default(0)
    val lastPlayed = timestamp("last_played").default(LocalDateTime.now())
}

object Rooms : Table() {
    val id = varchar("id", 50).primaryKey()
    val roomId = varchar("room_id", 50).uniqueIndex()
    val hostUid = varchar("host_uid", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val hostUsername = varchar("host_username", 50)
    val hostAvatarId = varchar("host_avatar_id", 50)
    val difficulty = enumeration<GameLevel>("difficulty")
    val type = varchar("type", 20).default("public")
    val status = varchar("status", 20).default("waiting") // waiting, playing, finished
    val players = text("players") // JSON array of player IDs
    val maxPlayers = integer("max_players").default(2)
    val createdAt = timestamp("created_at").default(LocalDateTime.now())
    val settingsJson = text("settings_json").nullable() // RoomSettings JSON
}

object GameResults : Table() {
    val id = varchar("id", 50).primaryKey()
    val gameId = varchar("game_id", 50)
    val player1Id = varchar("player1_id", 50).references(Users.id, onDelete = ReferenceOption.CASCADE)
    val player1Username = varchar("player1_username", 50)
    val player1Score = integer("player1_score")
    val player2Id = varchar("player2_id", 50).nullable().references(Users.id, onDelete = ReferenceOption.CASCADE)
    val player2Username = varchar("player2_username", 50).nullable()
    val player2Score = integer("player2_score")
    val winnerId = varchar("winner_id", 50).nullable().references(Users.id, onDelete = ReferenceOption.CASCADE)
    val gameMode = varchar("game_mode", 20) // "single_player" or "multiplayer"
    val difficulty = enumeration<GameLevel>("difficulty")
    val duration = integer("duration").nullable() // in seconds
    val completedAt = timestamp("completed_at").default(LocalDateTime.now())
}

fun initDatabase() {
    Database.connect(System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/naija_ayo", driver = "org.postgresql.Driver")

    transaction {
        SchemaUtils.create(
            Users,
            Friends,
            FriendRequests,
            Messages,
            SavedGames,
            Leaderboard,
            Rooms,
            GameResults
        )
    }
}