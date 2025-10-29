package com.naijaayo.worldwide

import java.io.Serializable

data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarId: String = "ayo", // Default avatar
    val createdAt: String,
    val lastLoginAt: String? = null,
    val isOnline: Boolean = false
) : Serializable

data class AuthRequest(
    val username: String,
    val email: String,
    val password: String
) : Serializable

data class AuthResponse(
    val success: Boolean,
    val user: User? = null,
    val token: String? = null,
    val message: String? = null
) : Serializable

data class LoginRequest(
    val email: String,
    val password: String
) : Serializable
