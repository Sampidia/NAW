package com.naijaayo.worldwide.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.naijaayo.worldwide.R

/**
 * Sound event types for contextual volume adjustment
 */
enum class SoundEventType {
    SEED_ADDED,     // Seeds added to pit
    SEED_REMOVED,   // Seeds removed from pit
    MULTIPLE_SEEDS  // Multiple seeds in one event
}

/**
 * Manages sound effects for the Naija Ayo game
 * Provides low-latency audio playback for game events
 */
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private var isEnabled = true
    private var masterVolume = 0.7f

    init {
        // Configure audio attributes for game sounds
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Initialize SoundPool with game-optimized settings
        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Allow up to 5 simultaneous sounds
            .setAudioAttributes(audioAttributes)
            .build()
    }

    /**
     * Load all sound effects into memory
     */
    fun loadSounds() {
        try {
            android.util.Log.d("SoundLoad", "🔄 Starting sound loading...")

            soundMap["wood"] = soundPool.load(context, R.raw.wood_sound, 1)
            android.util.Log.d("SoundLoad", "✅ Wood sound loaded: ${soundMap["wood"]}")

            soundMap["capture"] = soundPool.load(context, R.raw.capture_sound, 1)
            android.util.Log.d("SoundLoad", "✅ Capture sound loaded: ${soundMap["capture"]}")

            soundMap["click"] = soundPool.load(context, R.raw.click_sound, 1)
            android.util.Log.d("SoundLoad", "✅ Click sound loaded: ${soundMap["click"]}")

            soundMap["win"] = soundPool.load(context, R.raw.win_sound, 1)
            android.util.Log.d("SoundLoad", "✅ Win sound loaded: ${soundMap["win"]}")

            // Verify all sounds loaded successfully
            val allLoaded = soundMap.size == 4 && !soundMap.values.contains(0)
            android.util.Log.d("SoundLoad", "🎵 All sounds loaded successfully: $allLoaded (total: ${soundMap.size})")

            if (!allLoaded) {
                android.util.Log.w("SoundLoad", "⚠️ Some sounds may have failed to load!")
                soundMap.forEach { (name, id) ->
                    android.util.Log.d("SoundLoad", "   $name: $id")
                }
            }

            Log.d("SoundManager", "All sounds loaded successfully")
        } catch (e: Exception) {
            Log.e("SoundManager", "Error loading sounds", e)
            android.util.Log.e("SoundLoad", "❌ Error loading sounds: ${e.message}", e)
        }
    }

    /**
     * Play wood sound effect for seed interactions with contextual volume
     * @param volume Volume multiplier (0.0f to 1.0f)
     * @param eventType Type of interaction for contextual volume adjustment
     */
    fun playWoodSound(volume: Float = 0.7f, eventType: SoundEventType = SoundEventType.SEED_ADDED) {
        if (!isEnabled) return

        val soundId = soundMap["wood"]
        if (soundId != null) {
            // Apply contextual volume adjustment based on event type
            val contextualVolume = when (eventType) {
                SoundEventType.SEED_ADDED -> volume * 1.0f  // Normal volume for additions
                SoundEventType.SEED_REMOVED -> volume * 0.8f // Slightly quieter for removals
                SoundEventType.MULTIPLE_SEEDS -> volume * 1.2f // Louder for multiple seed events
            }

            val actualVolume = (contextualVolume * masterVolume).coerceIn(0f, 1f)
            soundPool.play(soundId, actualVolume, actualVolume, 1, 0, 1.0f)
        }
    }

    /**
     * Play capture sound effect for pit captures
     */
    fun playCaptureSound() {
        if (!isEnabled) {
            android.util.Log.d("SoundDebug", "❌ Capture sound blocked - sound disabled")
            return
        }

        val soundId = soundMap["capture"]
        if (soundId != null) {
            // Enhanced volume validation - ensure minimum volume for game sounds
            val baseVolume = masterVolume.coerceIn(0.1f, 1f) // Minimum 10% volume for game feedback
            val finalVolume = if (masterVolume > 0.0f) baseVolume else 0.7f // Use default if master volume is 0

            android.util.Log.d("SoundDebug", "🔊 Playing capture sound (master: $masterVolume, final: $finalVolume)")
            soundPool.play(soundId, finalVolume, finalVolume, 2, 0, 1.0f)
        } else {
            android.util.Log.e("SoundDebug", "❌ Capture sound not loaded!")
        }
    }

    /**
     * Play click sound effect for UI interactions
     */
    fun playClickSound() {
        if (!isEnabled) return

        val soundId = soundMap["click"]
        if (soundId != null) {
            val volume = (0.5f * masterVolume).coerceIn(0f, 1f)
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    /**
     * Play win sound effect for game completion
     */
    fun playWinSound() {
        if (!isEnabled) return

        val soundId = soundMap["win"]
        if (soundId != null) {
            val volume = masterVolume.coerceIn(0f, 1f)
            soundPool.play(soundId, volume, volume, 3, 0, 1.0f)
        }
    }

    /**
     * Enable or disable all sound effects
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Set master volume for all sounds
     * @param volume Volume level (0.0f to 1.0f)
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
    }

    /**
     * Check if sounds are currently enabled
     */
    fun isSoundEnabled(): Boolean = isEnabled

    /**
     * Get current master volume
     */
    fun getMasterVolume(): Float = masterVolume

    /**
     * Release sound resources
     */
    fun release() {
        try {
            soundPool.release()
            soundMap.clear()
            Log.d("SoundManager", "Sound resources released")
        } catch (e: Exception) {
            Log.e("SoundManager", "Error releasing sound resources", e)
        }
    }
}
