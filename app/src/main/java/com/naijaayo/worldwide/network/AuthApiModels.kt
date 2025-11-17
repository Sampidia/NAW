package com.naijaayo.worldwide.network

// Request models for auth endpoints
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)

// Response models
data class AuthResponse(
    val token: String,
    val user: AuthUser
)

data class AuthUser(
    val id: String,
    val username: String,
    val email: String,
    val avatarId: String
)

fun AuthUser.toUser(): com.naijaayo.worldwide.User {
    return com.naijaayo.worldwide.User(
        id = id,
        username = username,
        email = email,
        avatarId = avatarId,
        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()),
        isOnline = true
    )
}
