package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.timestamp
import com.naijaayo.worldwide.FriendStatus

object Friends : Table() {
    val id = varchar("id", 255)
    val userId = varchar("user_id", 255).references(Users.id)
    val friendId = varchar("friend_id", 255).references(Users.id)
    val friendUsername = varchar("friend_username", 255)
    val friendEmail = varchar("friend_email", 255)
    val friendAvatarId = varchar("friend_avatar_id", 255)
    val status = enumerationByName("status", 20, FriendStatus::class).default(FriendStatus.ACCEPTED)
    val createdAt = timestamp("created_at")
    val lastSeen = timestamp("last_seen")
    val isOnline = bool("is_online").default(false)

    override val primaryKey = PrimaryKey(id)
}
