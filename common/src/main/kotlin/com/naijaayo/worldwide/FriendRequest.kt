package com.naijaayo.worldwide

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val toUserId: String,
    val fromUsername: String,
    val fromEmail: String,
    val fromAvatarId: String,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val message: String? = null
)

@Serializable
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
