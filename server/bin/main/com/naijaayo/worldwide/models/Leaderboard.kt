package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Leaderboard : Table() {
    val id = varchar("id", 255)
    val userId = varchar("user_id", 255).references(Users.id)
    val username = varchar("username", 255)
    val avatarId = varchar("avatar_id", 255)
    val gameMode = varchar("game_mode", 50) // "single_player" or "multiplayer"
    val eloRating = integer("elo_rating").default(1200)
    val gamesPlayed = integer("games_played").default(0)
    val gamesWon = integer("games_won").default(0)
    val gamesLost = integer("games_lost").default(0)
    val gamesDrawn = integer("games_drawn").default(0)
    val winStreak = integer("win_streak").default(0)
    val bestWinStreak = integer("best_win_streak").default(0)
    val lastPlayed = timestamp("last_played")

    override val primaryKey = PrimaryKey(id)
}

// Data class for server-side leaderboard operations (matches table structure)
data class LeaderboardRecord(
    val id: String,
    val userId: String,
    val username: String,
    val avatarId: String,
    val gameMode: String,
    val eloRating: Int = 1200,
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val gamesLost: Int = 0,
    val gamesDrawn: Int = 0,
    val winStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val lastPlayed: java.time.LocalDateTime
)
