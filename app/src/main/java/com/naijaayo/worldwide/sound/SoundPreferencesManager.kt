package com.naijaayo.worldwide.sound

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages sound preferences using SharedPreferences
 * Provides a clean interface for sound settings without activity lifecycle dependencies
 */
object SoundPreferencesManager {

    private const val PREFS_NAME = "sound_settings"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_MASTER_VOLUME = "master_volume"
    private const val KEY_BACKGROUND_MUSIC_ENABLED = "background_music_enabled"
    private const val KEY_BACKGROUND_MUSIC_VOLUME = "background_music_volume"
    private const val KEY_SELECTED_MUSIC_TRACK = "selected_music_track"

    // Default values
    const val DEFAULT_SOUND_ENABLED = true
    const val DEFAULT_MASTER_VOLUME = 70 // 70%
    const val DEFAULT_BACKGROUND_MUSIC_ENABLED = true
    const val DEFAULT_BACKGROUND_MUSIC_VOLUME = 70 // 70%
    const val DEFAULT_MUSIC_TRACK = "afro_beat"

    private lateinit var sharedPreferences: SharedPreferences

    /**
     * Initialize the preferences manager with context
     * Must be called before using any other methods
     */
    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if sound is enabled
     */
    fun isSoundEnabled(): Boolean {
        ensureInitialized()
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
    }

    /**
     * Set sound enabled state
     */
    fun setSoundEnabled(enabled: Boolean) {
        ensureInitialized()
        sharedPreferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, enabled)
            .apply()
    }

    /**
     * Get master volume as percentage (0-100)
     */
    fun getMasterVolumePercent(): Int {
        ensureInitialized()
        val volume = sharedPreferences.getInt(KEY_MASTER_VOLUME, DEFAULT_MASTER_VOLUME)

        android.util.Log.d("SoundPrefs", "🔊 Loaded volume: $volume% (default: $DEFAULT_MASTER_VOLUME%)")
        return volume
    }

    /**
     * Set master volume as percentage (0-100)
     */
    fun setMasterVolumePercent(volumePercent: Int) {
        ensureInitialized()
        val clampedVolume = volumePercent.coerceIn(0, 100)
        sharedPreferences.edit()
            .putInt(KEY_MASTER_VOLUME, clampedVolume)
            .apply()
    }

    /**
     * Get master volume as float (0.0f to 1.0f)
     */
    fun getMasterVolumeFloat(): Float {
        val volumeFloat = getMasterVolumePercent() / 100.0f
        android.util.Log.d("SoundPrefs", "🎵 Volume float: $volumeFloat")
        return volumeFloat
    }

    /**
     * Set master volume as float (0.0f to 1.0f)
     */
    fun setMasterVolumeFloat(volume: Float) {
        val volumePercent = (volume.coerceIn(0f, 1f) * 100).toInt()
        setMasterVolumePercent(volumePercent)
    }

    /**
     * Check if background music is enabled
     */
    fun isBackgroundMusicEnabled(): Boolean {
        ensureInitialized()
        return sharedPreferences.getBoolean(KEY_BACKGROUND_MUSIC_ENABLED, DEFAULT_BACKGROUND_MUSIC_ENABLED)
    }

    /**
     * Set background music enabled state
     */
    fun setBackgroundMusicEnabled(enabled: Boolean) {
        ensureInitialized()
        sharedPreferences.edit()
            .putBoolean(KEY_BACKGROUND_MUSIC_ENABLED, enabled)
            .apply()
    }

    /**
     * Get background music volume as percentage (0-100)
     */
    fun getBackgroundMusicVolumePercent(): Int {
        ensureInitialized()
        val volume = sharedPreferences.getInt(KEY_BACKGROUND_MUSIC_VOLUME, DEFAULT_BACKGROUND_MUSIC_VOLUME)

        android.util.Log.d("SoundPrefs", "🔊 Loaded background volume: $volume%")
        return volume
    }

    /**
     * Set background music volume as percentage (0-100)
     */
    fun setBackgroundMusicVolumePercent(volumePercent: Int) {
        ensureInitialized()
        val clampedVolume = volumePercent.coerceIn(0, 100)
        sharedPreferences.edit()
            .putInt(KEY_BACKGROUND_MUSIC_VOLUME, clampedVolume)
            .apply()
    }

    /**
     * Get background music volume as float (0.0f to 1.0f)
     */
    fun getBackgroundMusicVolumeFloat(): Float {
        val volumeFloat = getBackgroundMusicVolumePercent() / 100.0f
        android.util.Log.d("SoundPrefs", "🎵 Background volume float: $volumeFloat")
        return volumeFloat
    }

    /**
     * Set background music volume as float (0.0f to 1.0f)
     */
    fun setBackgroundMusicVolumeFloat(volume: Float) {
        val volumePercent = (volume.coerceIn(0f, 1f) * 100).toInt()
        setBackgroundMusicVolumePercent(volumePercent)
    }

    /**
     * Get selected music track ID
     */
    fun getSelectedMusicTrack(): String {
        ensureInitialized()
        val track = sharedPreferences.getString(KEY_SELECTED_MUSIC_TRACK, DEFAULT_MUSIC_TRACK) ?: DEFAULT_MUSIC_TRACK
        android.util.Log.d("SoundPrefs", "🎵 getSelectedMusicTrack() returning: '$track'")
        return track
    }

    /**
     * Set selected music track ID
     */
    fun setSelectedMusicTrack(trackId: String) {
        ensureInitialized()
        sharedPreferences.edit()
            .putString(KEY_SELECTED_MUSIC_TRACK, trackId)
            .apply()
    }

    /**
     * Reset all sound preferences to defaults
     */
    fun resetToDefaults() {
        ensureInitialized()
        sharedPreferences.edit()
            .putBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
            .putInt(KEY_MASTER_VOLUME, DEFAULT_MASTER_VOLUME)
            .putBoolean(KEY_BACKGROUND_MUSIC_ENABLED, DEFAULT_BACKGROUND_MUSIC_ENABLED)
            .putInt(KEY_BACKGROUND_MUSIC_VOLUME, DEFAULT_BACKGROUND_MUSIC_VOLUME)
            .putString(KEY_SELECTED_MUSIC_TRACK, DEFAULT_MUSIC_TRACK)
            .apply()
    }

    /**
     * Ensure that initialize() has been called
     */
    private fun ensureInitialized() {
        if (!::sharedPreferences.isInitialized) {
            throw IllegalStateException(
                "SoundPreferencesManager must be initialized with context before use. " +
                "Call SoundPreferencesManager.initialize(context) first."
            )
        }
    }
}
