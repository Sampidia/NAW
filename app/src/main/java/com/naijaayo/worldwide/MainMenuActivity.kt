package com.naijaayo.worldwide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import com.naijaayo.worldwide.leaderboard.LeaderboardActivity
import com.naijaayo.worldwide.theme.NigerianThemeManager
import com.naijaayo.worldwide.sound.BackgroundMusicManager

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_main_menu)

        // Initialize and start background music
        android.util.Log.d("MainMenuActivity", "🎵 Initializing BackgroundMusicManager...")
        BackgroundMusicManager.initialize(this)
        android.util.Log.d("MainMenuActivity", "🎵 Calling startBackgroundMusic()...")
        Handler().postDelayed({
            BackgroundMusicManager.startBackgroundMusic()
            android.util.Log.d("MainMenuActivity", "🎵 Background music initialization completed")
        }, 1000) // Delay 1 second to ensure UI is fully loaded

        findViewById<Button>(R.id.singlePlayerButton).setOnClickListener {
            // Check for saved single player games first
            showResumeGameDialog("single_player")
        }

        findViewById<Button>(R.id.multiplayerButton).setOnClickListener {
            // Check for saved multiplayer games first
            showResumeGameDialog("multiplayer")
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.leaderboardButton).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        findViewById<Button>(R.id.friendsButton).setOnClickListener {
            // Check authentication before opening friends
            if (!com.naijaayo.worldwide.auth.SessionManager.isLoggedIn()) {
                val authDialog = com.naijaayo.worldwide.auth.AuthDialog(this) { userId, username, avatarId ->
                    startActivity(Intent(this, FriendsActivity::class.java))
                }
                authDialog.show()
            } else {
                startActivity(Intent(this, FriendsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reapply theme when activity becomes visible (e.g., after returning from theme settings)
        NigerianThemeManager.applyThemeToActivity(this)
        // Resume background music
        BackgroundMusicManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        // Pause background music when activity is not visible
        BackgroundMusicManager.pauseBackgroundMusic()
    }
    private fun showResumeGameDialog(gameMode: String) {
        // Check if user is authenticated
        if (!com.naijaayo.worldwide.auth.SessionManager.isLoggedIn()) {
            // Show authentication dialog
            val authDialog = com.naijaayo.worldwide.auth.AuthDialog(this) { userId, username, avatarId ->
                // After successful authentication, retry showing resume dialog
                showResumeGameDialog(gameMode)
            }
            authDialog.show()
            return
        }

        // TODO: Fetch saved games from server
        // For now, show empty dialog or "no saved games" message
        val savedGames = emptyList<SavedGame>() // TODO: Replace with actual API call

        val resumeDialog = ResumeGameDialog(
            context = this,
            savedGames = savedGames,
            onGameSelected = { savedGame ->
                // TODO: Load selected game and navigate to MainActivity
                Toast.makeText(this, "Loading saved game...", Toast.LENGTH_SHORT).show()
                // Navigate to game with loaded state
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("isSinglePlayer", gameMode == "single_player")
                    // TODO: Pass saved game data
                }
                startActivity(intent)
            },
            onStartNewGame = {
                // Start new game based on mode
                if (gameMode == "single_player") {
                    // Launch level selection
                    val intent = Intent(this, LevelSelectionActivity::class.java)
                    startActivity(intent)
                } else {
                    // Go to multiplayer lobby
                    startActivity(Intent(this, MultiplayerLobbyActivity::class.java))
                }
            }
        )
        resumeDialog.show()
    }
}
