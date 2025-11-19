package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import com.naijaayo.worldwide.GameLevel

object GameResults : Table() {
    val id = varchar("id", 255)
    val gameId = varchar("game_id", 255)
    val player1Id = varchar("player1_id", 255).references(Users.id)
    val player1Username = varchar("player1_username", 255)
    val player1Score = integer("player1_score")
    val player2Id = varchar("player2_id", 255).references(Users.id).nullable()
    val player2Username = varchar("player2_username", 255).nullable()
    val player2Score = integer("player2_score")
    val winnerId = varchar("winner_id", 255).references(Users.id).nullable()
    val gameMode = varchar("game_mode", 50) // "single_player" or "multiplayer"
    val difficulty = enumerationByName("difficulty", 10, GameLevel::class)
    val duration = integer("duration").nullable() // in seconds
    val completedAt = timestamp("completed_at")

    override val primaryKey = PrimaryKey(id)
}
