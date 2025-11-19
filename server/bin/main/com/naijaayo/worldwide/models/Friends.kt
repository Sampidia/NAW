package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table

object Friends : Table() {
    val id = varchar("id", 255)
    val userId = varchar("user_id", 255).references(Users.id)
    val friendId = varchar("friend_id", 255).references(Users.id)

    override val primaryKey = PrimaryKey(id)
}
