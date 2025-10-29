package com.naijaayo.worldwide

import java.io.Serializable

// User preferences that can be customized
data class UserPreferences(
    val themeId: String = "default",
    val boardId: String = "wood",
    val musicId: String = "afro_beat",
    val soundEnabled: Boolean = true,
    val volume: Float = 0.7f
) : Serializable

// Room settings that are synchronized across all players
data class RoomSettings(
    val themeId: String,
    val boardId: String,
    val musicId: String,
    val soundEnabled: Boolean,
    val volume: Float
) : Serializable

// Settings update request
data class SettingsUpdateRequest(
    val preferences: UserPreferences
) : Serializable

// Room settings response
data class RoomSettingsResponse(
    val success: Boolean,
    val settings: RoomSettings? = null,
    val message: String? = null
) : Serializable