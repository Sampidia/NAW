package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table

object Leaderboard : Table() {
    val id = varchar("id", 255)
    val userId = varchar("user_id", 255).references(Users.id)
    val gameMode = varchar("game_mode", 50)
    val wins = integer("wins").default(0)
    val losses = integer("losses").default(0)
    val draws = integer("draws").default(0)

    override val primaryKey = PrimaryKey(id)
}
