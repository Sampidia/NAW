package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object SavedGames : Table() {
    val id = varchar("id", 255)
    val userId = varchar("user_id", 255).references(Users.id)
    val gameMode = varchar("game_mode", 50) // "single_player" or "multiplayer"
    val opponentId = varchar("opponent_id", 255).nullable()
    val opponentUsername = varchar("opponent_username", 255).nullable()
    val opponentAvatarId = varchar("opponent_avatar_id", 255).nullable()
    val gameStateJson = text("game_state_json")
    val createdAt = timestamp("created_at")
    val lastPlayed = timestamp("last_played")

    override val primaryKey = PrimaryKey(id)
}
