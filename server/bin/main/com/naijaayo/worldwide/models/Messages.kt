package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import com.naijaayo.worldwide.MessageType

object Messages : Table() {
    val id = varchar("id", 255)
    val fromUserId = varchar("from_user_id", 255).references(Users.id)
    val toUserId = varchar("to_user_id", 255).references(Users.id)
    val fromUsername = varchar("from_username", 255)
    val content = text("content")
    val timestamp = timestamp("timestamp")
    val type = enumerationByName("type", 20, MessageType::class).default(MessageType.TEXT)
    val gameInvitationJson = text("game_invitation_json").nullable() // JSON string for GameInvitation

    override val primaryKey = PrimaryKey(id)
}
