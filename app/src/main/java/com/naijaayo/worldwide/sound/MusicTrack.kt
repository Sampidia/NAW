package com.naijaayo.worldwide.sound

/**
 * Data class representing a background music track
 */
data class MusicTrack(
    val id: String,
    val displayName: String,
    val drawableResName: String,
    val audioResName: String?,
    val isAvailable: Boolean = true,
    val isActive: Boolean = false
) {
    val hasAudio: Boolean
        get() = !audioResName.isNullOrEmpty()
}
