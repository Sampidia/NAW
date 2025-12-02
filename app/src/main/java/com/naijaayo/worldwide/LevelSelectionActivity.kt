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
import com.bumptech.glide.Glide
import android.widget.ImageView

class LevelSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar
        supportActionBar?.hide()

        setContentView(R.layout.activity_level_selection)

        val appLogo = findViewById<ImageView>(R.id.appLogo)
        Glide.with(this).load(R.raw.logo_animate).into(appLogo)

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_game_rules, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val rulesContent = dialogView.findViewById<android.widget.TextView>(R.id.rulesContent)
        val rulesText = "Naija Ayo is a traditional African board game.<br><br>" +
                "<b>Objective:</b> Capture more seeds than your opponent.<br><br>" +
                "<b>Levels:</b><br>" +
                "- Easy: Capture 2 or 3 seeds<br>" +
                "- Medium: Capture 3 seeds (standard)<br>" +
                "- Hard: Capture 4 seeds<br><br>" +
                "Sow seeds counterclockwise. Capture opponent pits that match the level's seed count after sowing if a transition occurs."
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            rulesContent.text = android.text.Html.fromHtml(rulesText, android.text.Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            rulesContent.text = android.text.Html.fromHtml(rulesText)
        }

        dialogView.findViewById<android.widget.Button>(R.id.okButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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