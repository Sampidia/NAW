package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table

object Rooms : Table() {
    val id = varchar("id", 255)
    val roomId = varchar("room_id", 255).uniqueIndex()
    val player1Id = varchar("player1_id", 255).references(Users.id)
    val player2Id = varchar("player2_id", 255).references(Users.id).nullable()
    val isPrivate = bool("is_private").default(false)

    override val primaryKey = PrimaryKey(id)
}
