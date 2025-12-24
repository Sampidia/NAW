package com.naijaayo.worldwide.model

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val avatarId: String = "ayo",
    val score: Int = 0,
    val coinBalance: Int = 0,
    val stats: Map<String, Any> = emptyMap(),
    val displayName: String = ""
)
