package com.naijaayo.worldwide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.naijaayo.worldwide.leaderboard.LeaderboardActivity
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.network.Player
import com.naijaayo.worldwide.sound.BackgroundMusicManager
import com.naijaayo.worldwide.theme.NigerianThemeManager
import kotlinx.coroutines.launch
import com.naijaayo.worldwide.FriendsActivity
import com.naijaayo.worldwide.GameRoomActivity
import com.bumptech.glide.Glide
import android.widget.ImageView

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        supportActionBar?.hide()

        setContentView(R.layout.activity_main_menu)

        val appLogo = findViewById<ImageView>(R.id.appLogo)
        Glide.with(this).load(R.raw.logo_animate).into(appLogo)

        // Initialize and start background music
        BackgroundMusicManager.initialize(this)
        Handler().postDelayed({ BackgroundMusicManager.startBackgroundMusic() }, 1000)

        findViewById<Button>(R.id.singlePlayerButton).setOnClickListener {
            // TODO: Re-implement single player logic if needed
            startActivity(Intent(this, LevelSelectionActivity::class.java))
        }

        findViewById<Button>(R.id.multiplayerButton).setOnClickListener {
             if (FirebaseManager.auth.currentUser == null) {
                Toast.makeText(this, "Please log in to play multiplayer.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ProfileActivity::class.java))
                return@setOnClickListener
            }
            showMultiplayerOptionsDialog()
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.leaderboardButton).setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }

        findViewById<Button>(R.id.friendsButton).setOnClickListener {
             if (FirebaseManager.auth.currentUser == null) {
                Toast.makeText(this, "Please log in to see friends.", Toast.LENGTH_SHORT).show()
                 startActivity(Intent(this, ProfileActivity::class.java))
            } else {
                startActivity(Intent(this, FriendsActivity::class.java))
            }
        }
    }

    private fun showMultiplayerOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_multiplayer_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.playNowButton).setOnClickListener {
            dialog.dismiss()
            startGameNow()
        }

        dialogView.findViewById<Button>(R.id.playWithFriendsButton).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, GameRoomActivity::class.java))
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun startGameNow() {
        lifecycleScope.launch {
            // Show a "searching" dialog
            val searchingDialog = AlertDialog.Builder(this@MainMenuActivity)
                .setTitle("Searching for Opponent...")
                .setCancelable(false)
                .setNegativeButton("Cancel") { dialog, _ ->
                    FirebaseManager.auth.currentUser?.uid?.let { FirebaseManager.cancelMatchmaking(it) }
                    dialog.dismiss()
                }
                .create()
            searchingDialog.show()

            // 1. Join the general game session to be counted in the 98 users
            val sessionJoined = FirebaseManager.joinGameSession()
            if (!sessionJoined) {
                searchingDialog.dismiss()
                Toast.makeText(this@MainMenuActivity, "Servers are full, please try again later.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val currentUser = FirebaseManager.auth.currentUser!!
            // You should fetch the real profile data here
            val player = Player(uid = currentUser.uid, displayName = currentUser.displayName ?: "Player", avatarId = "ayo")

            // 2. Listen for a match
            val matchListener = FirebaseManager.listenForGameMatch(currentUser.uid) { gameId ->
                searchingDialog.dismiss()
                // Match found! Navigate to the game screen
                val intent = Intent(this@MainMenuActivity, MainActivity::class.java).apply {
                    putExtra("GAME_ID", gameId)
                }
                startActivity(intent)
                // Make sure to remove the listener once the match is found
                FirebaseManager.removeListener(FirebaseManager.listenForGameMatch(currentUser.uid){})
            }
            
            // 3. Enter the matchmaking pool
            FirebaseManager.findMatch(player)

            // Clean up listener if the user cancels
            searchingDialog.setOnDismissListener {
                 FirebaseManager.removeListener(matchListener)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NigerianThemeManager.applyThemeToActivity(this)
        BackgroundMusicManager.resumeBackgroundMusic()
    }

    override fun onPause() {
        super.onPause()
        BackgroundMusicManager.pauseBackgroundMusic()
    }
}
