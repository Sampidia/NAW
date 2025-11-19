package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import com.naijaayo.worldwide.GameLevel

object Rooms : Table() {
    val id = varchar("id", 255)
    val roomId = varchar("room_id", 255).uniqueIndex()
    val hostUid = varchar("host_uid", 255).references(Users.id)
    val hostUsername = varchar("host_username", 255)
    val hostAvatarId = varchar("host_avatar_id", 255)
    val difficulty = enumerationByName("difficulty", 10, GameLevel::class).default(GameLevel.MEDIUM)
    val type = varchar("type", 50).default("public")
    val status = varchar("status", 50).default("waiting") // waiting, playing, finished
    val players = text("players") // JSON array of player IDs
    val maxPlayers = integer("max_players").default(2)
    val createdAt = timestamp("created_at")
    val settingsJson = text("settings_json").nullable() // RoomSettings JSON

    override val primaryKey = PrimaryKey(id)
}
