package com.naijaayo.worldwide

import kotlinx.serialization.Serializable

@Serializable
data class Friend(
    val id: String,
    val userId: String, // The current user's ID
    val friendId: String, // The friend's user ID
    val friendUsername: String,
    val friendEmail: String,
    val friendAvatarId: String,
    val status: FriendStatus = FriendStatus.ACCEPTED,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val isOnline: Boolean = false
)

@Serializable
enum class FriendStatus {
    PENDING,
    ACCEPTED,
    BLOCKED
}