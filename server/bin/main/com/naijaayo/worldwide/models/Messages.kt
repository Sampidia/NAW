package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Messages : Table() {
    val id = varchar("id", 255)
    val fromUserId = varchar("from_user_id", 255).references(Users.id)
    val toUserId = varchar("to_user_id", 255).references(Users.id)
    val content = text("content")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)
}
