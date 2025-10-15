package com.naijaayo.worldwide

import java.util.Date

data class Friend(
    val id: String,
    val requesterUid: String,
    val receiverUid: String,
    val status: String, // e.g., "pending", "accepted", "declined"
    val createdAt: Date = Date()
)