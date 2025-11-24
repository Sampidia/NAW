package com.naijaayo.worldwide.theme

import android.content.Context
import android.content.SharedPreferences
import com.naijaayo.worldwide.R

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

    fun getAvatarPortrait(avatarId: String): Int {
        return when (avatarId) {
            "ayo" -> R.drawable.char_ayo_portrait
            "ada" -> R.drawable.char_ada_portrait
            "fatima" -> R.drawable.char_fatima_portrait
            else -> R.mipmap.ic_launcher_foreground
        }
    }

    fun getAvatarFullBody(avatarId: String): Int {
        return when (avatarId) {
            "ayo" -> R.drawable.char_ayo_full
            "ada" -> R.drawable.char_ada_full
            "fatima" -> R.drawable.char_fatima_full
            else -> R.mipmap.ic_launcher_foreground
        }
    }
}
