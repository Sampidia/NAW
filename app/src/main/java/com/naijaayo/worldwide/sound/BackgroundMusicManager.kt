package com.naijaayo.worldwide.sound

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Manages background music playback using MediaPlayer
 * Provides a singleton interface for playing background music across activities
 */
object BackgroundMusicManager {

    private const val TAG = "BackgroundMusicManager"

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: MusicTrack? = null
    private var isInitialized = false
    private var playbackPosition = 0
    private var appContext: Context? = null

    /**
     * Initialize the background music manager
     */
    fun initialize(context: Context) {
        if (isInitialized) return

        appContext = context.applicationContext
        SoundPreferencesManager.initialize(context)
        isInitialized = true
        Log.d(TAG, "🎵 BackgroundMusicManager initialized")
    }

    /**
      * Start playing background music (always enabled)
      */
     fun startBackgroundMusic() {
         Log.d(TAG, "🎵 startBackgroundMusic() called")

         if (!isInitialized) {
             Log.w(TAG, "⚠️ BackgroundMusicManager not initialized")
             return
         }

         // Prevent unnecessary switching if already playing the correct track
         if (isPlaying()) {
             val currentTrackId = currentTrack?.id
             val selectedTrackId = SoundPreferencesManager.getSelectedMusicTrack()
             if (currentTrackId == selectedTrackId) {
                 Log.d(TAG, "🎵 Already playing the selected track: ${currentTrack?.displayName}")
                 return
             }
         }

         var selectedTrackId = SoundPreferencesManager.getSelectedMusicTrack()
         Log.d(TAG, "🎵 Current selected track ID: '$selectedTrackId'")

         if (selectedTrackId == "none" || selectedTrackId.isEmpty()) {
             Log.d(TAG, "🎵 No music track selected, setting default to afro_beat")
             selectedTrackId = "afro_beat"
             SoundPreferencesManager.setSelectedMusicTrack(selectedTrackId)
         }

         Log.d(TAG, "🎵 Will play track: $selectedTrackId")

        // Find the track by ID and play it
        val musicTracks = getAllMusicTracks()
        val track = musicTracks.find { it.id == selectedTrackId }

        Log.d(TAG, "🎵 Found track: ${track?.displayName}, hasAudio: ${track?.hasAudio}")

        if (track != null && track.hasAudio) {
            Log.d(TAG, "🎵 Playing track: ${track.displayName}")
            playTrack(track)
        } else {
            Log.w(TAG, "🎵 Track not found or no audio: $selectedTrackId")
        }
    }

    /**
     * Stop background music
     */
    fun stopBackgroundMusic() {
        mediaPlayer?.let { player ->
            playbackPosition = player.currentPosition
            player.stop()
            player.release()
            mediaPlayer = null
            currentTrack = null
            Log.d(TAG, "⏹️ Background music stopped")
        }
    }

    /**
     * Pause background music
     */
    fun pauseBackgroundMusic() {
        mediaPlayer?.let { player ->
            playbackPosition = player.currentPosition
            player.pause()
            Log.d(TAG, "⏸️ Background music paused at position: $playbackPosition")
        }
    }

    /**
     * Resume background music
     */
    fun resumeBackgroundMusic() {
        mediaPlayer?.let { player ->
            if (playbackPosition > 0) {
                player.seekTo(playbackPosition)
            }
            player.start()
            Log.d(TAG, "▶️ Background music resumed")
        }
    }

    /**
     * Switch to a different music track
     */
    fun switchTrack(track: MusicTrack) {
        if (track.hasAudio) {
            stopBackgroundMusic()
            playTrack(track)
            SoundPreferencesManager.setSelectedMusicTrack(track.id)
            Log.d(TAG, "🔄 Switched to track: ${track.displayName}")
        } else {
            Log.d(TAG, "🎵 Track has no audio: ${track.displayName}")
        }
    }

    /**
     * Update volume based on background music volume setting
     */
    fun updateVolume() {
        mediaPlayer?.let { player ->
            val volume = SoundPreferencesManager.getBackgroundMusicVolumeFloat()
            player.setVolume(volume, volume)
            Log.d(TAG, "🔊 Background volume updated to: ${volume * 100}%")
        }
    }

    /**
     * Check if background music is currently playing
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    /**
     * Get currently playing track
     */
    fun getCurrentTrack(): MusicTrack? = currentTrack

    /**
     * Release resources
     */
    fun release() {
        stopBackgroundMusic()
        isInitialized = false
        Log.d(TAG, "🗑️ BackgroundMusicManager released")
    }

    private fun playTrack(track: MusicTrack) {
        try {
            val context = getApplicationContext() ?: return
            val audioResId = getRawResourceId(track.audioResName!!)

            if (audioResId == 0) {
                Log.e(TAG, "❌ Audio resource not found: ${track.audioResName}")
                return
            }

            val mediaPlayer = MediaPlayer.create(context, audioResId)
            this.mediaPlayer = mediaPlayer
            this.currentTrack = track

            // Set volume based on preferences
            updateVolume()

            // Set looping
            mediaPlayer.isLooping = true

            // Set listeners
            mediaPlayer.setOnPreparedListener {
                it.start()
                Log.d(TAG, "🎵 Playing: ${track.displayName}")
            }

            mediaPlayer.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "❌ MediaPlayer error: what=$what, extra=$extra")
                false
            }

            mediaPlayer.setOnCompletionListener {
                Log.d(TAG, "🔄 Track completed (should loop): ${track.displayName}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing track: ${track.displayName}", e)
        }
    }

    private fun getRawResourceId(resourceName: String): Int {
        return try {
            val context = getApplicationContext()
            context?.resources?.getIdentifier(resourceName, "raw", context.packageName) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting resource ID for: $resourceName", e)
            0
        }
    }

    private fun getApplicationContext(): Context? {
        return appContext
    }

    /**
     * Get all available music tracks
     */
    fun getAllMusicTracks(): List<MusicTrack> {
        return listOf(
            MusicTrack(
                id = "afro_beat",
                displayName = "Afro Beat",
                drawableResName = "afro_beat",
                audioResName = "afro_beat",
                isAvailable = true
            ),
            MusicTrack(
                id = "afro_echoes",
                displayName = "Afro Echoes",
                drawableResName = "afro_echoes",
                audioResName = "afro_echoes",
                isAvailable = true
            ),
            MusicTrack(
                id = "afro_naija",
                displayName = "Afro Naija",
                drawableResName = "afro_naija",
                audioResName = "afro_naija",
                isAvailable = true
            ),
            MusicTrack(
                id = "afro_vibration",
                displayName = "Afro Vibration",
                drawableResName = "afro_vibration",
                audioResName = "afro_vibration",
                isAvailable = true
            ),
            MusicTrack(
                id = "lagos_vibe",
                displayName = "Lagos Vibe",
                drawableResName = "lagos_vibe",
                audioResName = "lagos_vibe",
                isAvailable = true
            ),
            MusicTrack(
                id = "soon_background",
                displayName = "Coming Soon",
                drawableResName = "soon_background",
                audioResName = null,
                isAvailable = false
            )
        )
    }
}
