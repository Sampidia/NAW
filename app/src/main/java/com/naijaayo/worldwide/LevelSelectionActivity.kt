package com.naijaayo.worldwide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.sound.BackgroundMusicManager

class LevelSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar
        supportActionBar?.hide()

        setContentView(R.layout.activity_level_selection)

        // Initialize and start background music
        android.util.Log.d("LevelSelectionActivity", "🎵 Initializing BackgroundMusicManager...")
        BackgroundMusicManager.initialize(this)
        android.util.Log.d("LevelSelectionActivity", "🎵 Calling startBackgroundMusic()...")
        Handler().postDelayed({
            BackgroundMusicManager.startBackgroundMusic()
            android.util.Log.d("LevelSelectionActivity", "🎵 Background music initialization completed")
        }, 1000) // Delay 1 second to ensure UI is fully loaded

        findViewById<Button>(R.id.easyButton).setOnClickListener {
            startGame("Easy")
        }

        findViewById<Button>(R.id.mediumButton).setOnClickListener {
            startGame("Medium")
        }

        findViewById<Button>(R.id.hardButton).setOnClickListener {
            startGame("Hard")
        }

        findViewById<Button>(R.id.rulesButton).setOnClickListener {
            showRules()
        }
    }

    private fun startGame(level: String) {
        val gameLevel = when (level) {
            "Easy" -> com.naijaayo.worldwide.GameLevel.EASY
            "Medium" -> com.naijaayo.worldwide.GameLevel.MEDIUM
            "Hard" -> com.naijaayo.worldwide.GameLevel.HARD
            else -> com.naijaayo.worldwide.GameLevel.MEDIUM
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("isSinglePlayer", true)
            putExtra("level", gameLevel.name)
        }
        startActivity(intent)
    }

    private fun showRules() {
        AlertDialog.Builder(this)
            .setTitle("Game Rules")
            .setMessage("Naija Ayo is a traditional African board game.\n\n" +
                "Objective: Capture more seeds than your opponent.\n\n" +
                "Levels:\n" +
                "- Easy: Capture 2 or 3 seeds\n" +
                "- Medium: Capture 3 seeds (standard)\n" +
                "- Hard: Capture 4 seeds\n\n" +
                "Sow seeds counterclockwise. Capture opponent pits that match the level's seed count after sowing if a transition occurs.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme
        NigerianThemeManager.applyThemeToActivity(this)
        // Resume background music
        BackgroundMusicManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        // Pause background music
        BackgroundMusicManager.pauseBackgroundMusic()
    }
}