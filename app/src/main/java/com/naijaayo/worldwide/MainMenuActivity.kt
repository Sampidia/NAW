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
            val currentUser = FirebaseManager.auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this@MainMenuActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Fetch user profile for avatar and display name
            val profile = FirebaseManager.getUserProfile(currentUser.uid)
            val avatarId = profile?.get("avatarId") as? String ?: "ayo"
            val displayName = profile?.get("username") as? String 
                ?: profile?.get("displayName") as? String 
                ?: currentUser.displayName 
                ?: "Player"

            val player = PlayNowPlayer(
                uid = currentUser.uid, 
                displayName = displayName, 
                avatarId = avatarId
            )

            // Show styled waiting dialog
            val dialogView = layoutInflater.inflate(R.layout.dialog_matchmaking_waiting, null)
            val waitingDialog = AlertDialog.Builder(this@MainMenuActivity)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            
            waitingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            waitingDialog.show()

            // Set up cancel button
            var matchListener: com.google.firebase.database.ValueEventListener? = null
            var currentRoomId: String? = null
            
            dialogView.findViewById<Button>(R.id.cancelButton).setOnClickListener {
                // Leave queue and dismiss
                FirebaseManager.leaveMatchmakingQueue(currentUser.uid, currentRoomId)
                matchListener?.let { FirebaseManager.removeListener(it) }
                waitingDialog.dismiss()
            }

            // Try to join matchmaking
            val result = FirebaseManager.joinMatchmakingQueue(player)

            when (result) {
                is FirebaseManager.MatchResult.Matched -> {
                    // Immediately matched with another player
                    waitingDialog.dismiss()
                    navigateToGame(result.roomId, result.roomCode)
                }
                
                is FirebaseManager.MatchResult.Waiting -> {
                    // Waiting for another player - listen for match
                    currentRoomId = result.roomId
                    matchListener = FirebaseManager.listenForMatch(result.roomId) { roomId, roomCode ->
                        waitingDialog.dismiss()
                        navigateToGame(roomId, roomCode)
                    }
                }
                is FirebaseManager.MatchResult.Error -> {
                    waitingDialog.dismiss()
                    Toast.makeText(this@MainMenuActivity, "Matchmaking error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToGame(roomId: String, roomCode: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ROOM_ID", roomId)
            putExtra("ROOM_CODE", roomCode)
            putExtra("GAME_ID", roomId) // For backward compatibility
            putExtra("MIC_ENABLED", false) // Default to false for Play Now
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        NigerianThemeManager.applyThemeToActivity(this)
        BackgroundMusicManager.resumeBackgroundMusic()
        
        // Set user as online when they are actively using the app
        if (FirebaseManager.auth.currentUser != null) {
            FirebaseManager.setUserOnline()
        }
    }

    override fun onPause() {
        super.onPause()
        BackgroundMusicManager.pauseBackgroundMusic()
    }
    
    override fun onStop() {
        super.onStop()
        // Set user as offline when they leave the app or put it in background
        if (FirebaseManager.auth.currentUser != null) {
            FirebaseManager.setUserOffline()
        }
    }
}
