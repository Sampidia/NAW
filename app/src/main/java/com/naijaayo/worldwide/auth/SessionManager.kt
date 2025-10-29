package com.naijaayo.worldwide.auth

import android.content.Context
import android.content.SharedPreferences
import com.naijaayo.worldwide.User

object SessionManager {

    private const val PREF_NAME = "user_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_AVATAR_ID = "avatar_id"
    private const val KEY_TOKEN = "auth_token"

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserSession(user: User, token: String) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_AVATAR_ID, user.avatarId)
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    fun getCurrentUser(): User? {
        if (!isLoggedIn()) return null

        val id = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        val username = sharedPreferences.getString(KEY_USERNAME, null) ?: return null
        val email = sharedPreferences.getString(KEY_EMAIL, null) ?: return null
        val avatarId = sharedPreferences.getString(KEY_AVATAR_ID, "ayo") ?: "ayo"

        return User(
            id = id,
            username = username,
            email = email,
            avatarId = avatarId,
            createdAt = "", // Not stored locally
            isOnline = true
        )
    }

    fun getAuthToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        sharedPreferences.edit().apply {
            clear()
            apply()
        }
    }

    fun updateAvatar(avatarId: String) {
        if (isLoggedIn()) {
            sharedPreferences.edit().putString(KEY_AVATAR_ID, avatarId).apply()
        }
    }
}