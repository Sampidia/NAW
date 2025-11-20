package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import com.naijaayo.worldwide.FriendRequestStatus

object FriendRequests : Table() {
    val id = varchar("id", 255)
    val fromUserId = varchar("from_user_id", 255).references(Users.id)
    val toUserId = varchar("to_user_id", 255).references(Users.id)
    val fromUsername = varchar("from_username", 255)
    val fromEmail = varchar("from_email", 255)
    val fromAvatarId = varchar("from_avatar_id", 255)
    val status = enumerationByName("status", 20, FriendRequestStatus::class).default(FriendRequestStatus.PENDING)
    val createdAt = timestamp("created_at")
    val message = varchar("message", 500).nullable()

    override val primaryKey = PrimaryKey(id)
}
