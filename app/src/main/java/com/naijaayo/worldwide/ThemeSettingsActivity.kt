package com.naijaayo.worldwide

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.theme.NigerianTheme
import com.naijaayo.worldwide.theme.NigerianThemeManager

class ThemeSettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var themeAdapter: NigerianThemeAdapter
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_theme_settings)

        // Initialize views
        recyclerView = findViewById(R.id.themeRecyclerView)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Setup RecyclerView with grid layout (2 columns)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        themeAdapter = NigerianThemeAdapter(NigerianThemeManager.getAllThemes()) { selectedTheme ->
            onThemeSelected(selectedTheme)
        }
        recyclerView.adapter = themeAdapter

        // Setup buttons
        saveButton.setOnClickListener {
            saveSelectedTheme()
        }

        cancelButton.setOnClickListener {
            // Reset selection to currently active theme before canceling
            val activeTheme = NigerianThemeManager.getActiveTheme()
            if (activeTheme != null) {
                themeAdapter.updateSelectedTheme(activeTheme.id)
            }
            finish() // Go back without saving
        }
    }

    private fun onThemeSelected(theme: NigerianTheme) {
        if (theme.isAvailable) {
            // Update adapter to show selection
            themeAdapter.updateSelectedTheme(theme.id)
        } else {
            Toast.makeText(this, "This theme is coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSelectedTheme() {
        val selectedThemeId = themeAdapter.getSelectedThemeId()
        if (selectedThemeId != null) {
            NigerianThemeManager.setActiveTheme(selectedThemeId)
            NigerianThemeManager.saveThemePreference()

            // Immediately apply the new theme to current activity
            NigerianThemeManager.applyThemeToActivity(this)

            Toast.makeText(this, "Theme saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Please select a theme first!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible (e.g., after returning from other screens)
         NigerianThemeManager.applyThemeToActivity(this)
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()
     }
}
