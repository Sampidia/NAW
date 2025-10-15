package com.naijaayo.worldwide

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class User(
    val uid: String,
    val username: String,
    val email: String,
    val avatarId: String? = null,
    val rating: Int = 1000,
    val wins: Int = 0,
    val losses: Int = 0,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date()),
    val fcmToken: String? = null
)
