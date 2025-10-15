package com.naijaayo.worldwide

import java.util.Date

data class Message(
    val messageId: String,
    val channelId: String,
    val senderUid: String,
    val text: String,
    val type: String, // e.g., "text", "image", "invite"
    val createdAt: Date = Date()
)