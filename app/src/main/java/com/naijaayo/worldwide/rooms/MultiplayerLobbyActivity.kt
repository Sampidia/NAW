package com.naijaayo.worldwide.rooms

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.naijaayo.worldwide.R

class MultiplayerLobbyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer_lobby)
    }

    override fun onResume() {
        super.onResume()
        // Resume background music
        com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
    }
}
