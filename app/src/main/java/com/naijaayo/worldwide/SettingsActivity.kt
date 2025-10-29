package com.naijaayo.worldwide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.naijaayo.worldwide.theme.NigerianThemeManager

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_settings)

        // Profile button - navigate to existing ProfileActivity
        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Theme button - navigate to theme settings
        findViewById<Button>(R.id.themeButton).setOnClickListener {
            startActivity(Intent(this, ThemeSettingsActivity::class.java))
        }

        // Board Color button - navigate to board settings
        findViewById<Button>(R.id.boardColorButton).setOnClickListener {
            startActivity(Intent(this, BoardSettingsActivity::class.java))
        }

        // Sound button - navigate to sound settings
        findViewById<Button>(R.id.soundButton).setOnClickListener {
            startActivity(Intent(this, SoundSettingsActivity::class.java))
        }
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible (e.g., after returning from theme settings)
         NigerianThemeManager.applyThemeToActivity(this)
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
     }
}
