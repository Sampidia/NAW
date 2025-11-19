package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table

object FriendRequests : Table() {
    val id = varchar("id", 255)
    val fromUserId = varchar("from_user_id", 255).references(Users.id)
    val toUserId = varchar("to_user_id", 255).references(Users.id)
    val status = varchar("status", 50)

    override val primaryKey = PrimaryKey(id)
}
