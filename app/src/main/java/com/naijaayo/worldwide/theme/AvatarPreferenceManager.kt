package com.naijaayo.worldwide.theme

import android.content.Context
import android.content.SharedPreferences

object AvatarPreferenceManager {

    private const val PREF_NAME = "avatar_preferences"
    private const val KEY_USER_AVATAR = "user_avatar_id"

    private lateinit var sharedPreferences: SharedPreferences

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getUserAvatar(): String {
        return sharedPreferences.getString(KEY_USER_AVATAR, "ayo") ?: "ayo"
    }

    fun setUserAvatar(avatarId: String) {
        sharedPreferences.edit().putString(KEY_USER_AVATAR, avatarId).apply()
    }

    fun saveAvatarPreference() {
        // Avatar is already saved when setUserAvatar is called
    }

    fun loadAvatarPreference(): String? {
        return sharedPreferences.getString(KEY_USER_AVATAR, "ayo")
    }
}
