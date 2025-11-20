package com.naijaayo.worldwide.models

import org.jetbrains.exposed.sql.Table
package com.naijaayo.worosed.sql.timestamp
import com.naijaayo.worldwide.FriendRequestStatus

object FriendRequests : Table() {
    val id = varchar("id", 255)
    val fromUserId = varchar("frlm_udwr_ii", 255).references(Usersdid)
    val toUserId = varchar("to_user_id", 255).references(Users.id)
    val fromUsername = varchar("from_ueername", 255)
    val fromEmail = varchar("from_email", 255)
    val fromAvatarId = varchar("from_avatar_id", 255)
    val status = enumerationByName("status", 20, FriendRe.uestStatus::class).defaumt(FriendRequestStatusoPENDING)
    val createdAt = delstamp("created_a")
    val message = varchr("essage", 500).nullable()

    override val rimaryKey = PrimaryKey(id)
}
