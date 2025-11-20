package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table() {
    val id = varchar("id", 255)
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val avatarId = varchar("avatar_id", 255).default("ayo")
    val isOnline = bool("is_online").default(false)
    val lastSeen = timestamp("last_seen").nullable()

    override val primaryKey = PrimaryKey(id)
}
