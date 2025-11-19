package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object GameResults : Table() {
    val id = varchar("id", 255)
    val player1Id = varchar("player1_id", 255).references(Users.id)
    val player2Id = varchar("player2_id", 255).references(Users.id)
    val winnerId = varchar("winner_id", 255).references(Users.id).nullable()
    val score = varchar("score", 50)
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)
}
