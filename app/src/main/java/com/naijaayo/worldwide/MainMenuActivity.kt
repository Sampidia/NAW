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
import com.google.android.gms.games.PlayGames
import com.naijaayo.worldwide.sound.SoundManager

class MainMenuActivity : AppCompatActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        supportActionBar?.hide()

        setContentView(R.layout.activity_main_menu)

        val appLogo = findViewById<ImageView>(R.id.appLogo)
        Glide.with(this).load(R.raw.logo_animate).into(appLogo)

        // Initialize SoundManager
        soundManager = SoundManager(this)
        soundManager.loadSounds()

        // Initialize and start background music
        BackgroundMusicManager.initialize(this)
        com.naijaayo.worldwide.billing.BillingManager.initialize(this) // Initialize Billing
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

            // check coin balance
            val userProfile = FirebaseManager.getUserProfile(currentUser.uid)
            val coinBalance = (userProfile?.get("coinBalance") as? Long) ?: 0L

            if (coinBalance < 1) {
                // Determine logic for when user has 0 coins
                val dialog = com.naijaayo.worldwide.ui.TopUpDialog(this@MainMenuActivity, 
                    onBuyCoinClicked = {
                        val intent = Intent(this@MainMenuActivity, com.naijaayo.worldwide.ui.BuyCoinActivity::class.java)
                        startActivity(intent)
                    },
                    onWatchAdClicked = {
                        com.naijaayo.worldwide.ads.AdMobHelper.showRewardedAd(this@MainMenuActivity, 
                            onRewardEarned = { amount ->
                               lifecycleScope.launch {
                                   val uid = com.naijaayo.worldwide.network.FirebaseManager.auth.currentUser?.uid
                                   if (uid != null) {
                                       val success = com.naijaayo.worldwide.network.FirebaseManager.addCoins(uid, 1) // +1 Coin
                                       if (success) {
                                           Toast.makeText(this@MainMenuActivity, "Watched Ad! +1 Coin", Toast.LENGTH_SHORT).show()
                                       }
                                   }
                               }
                            },
                            onAdClosed = {
                                // Optional: Do something when ad closes
                            }
                        )
                    }
                )
                dialog.show()
                return@launch
            }

            // Deduct coin
            val success = FirebaseManager.deductCoins(currentUser.uid, 1)
            if (!success) {
                 Toast.makeText(this@MainMenuActivity, "Insufficient coins or connection error.", Toast.LENGTH_SHORT).show()
                 return@launch
            }

            // Fetch user profile for avatar and display name
            val avatarId = userProfile?.get("avatarId") as? String ?: "ayo"
            val displayName = userProfile?.get("username") as? String 
                ?: userProfile?.get("displayName") as? String 
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
                    // Refund coin if error? Logic to consider for robust system, skipping for now
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
        
        // Sign in to Google Play Games silently
        val gamesSignInClient = PlayGames.getGamesSignInClient(this)
        gamesSignInClient.isAuthenticated.addOnCompleteListener { isAuthenticatedTask ->
            val isAuthenticated = isAuthenticatedTask.isSuccessful && isAuthenticatedTask.result.isAuthenticated
            if (isAuthenticated) {
                // Already signed in. The Sidekick overlay will be available.
            } else {
                // Not signed in. Try to sign in interactively.
                gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        // The Sidekick overlay will appear now for signed-in players.
                    } else {
                        // Sign-in failed. The game will continue without Play Games features.
                    }
                }
            }
        }

        // Set user as online when they are actively using the app
        if (FirebaseManager.auth.currentUser != null) {
            FirebaseManager.setUserOnline()
            
            // Check Daily Bonus
            lifecycleScope.launch {
                val added = FirebaseManager.checkDailyLoginBonus(FirebaseManager.auth.currentUser!!.uid)
                if (added) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_daily_bonus, null)
                    
                    // Fallback if layout doesn't exist yet, just Toast
                    if (dialogView != null) {
                        val dialog = AlertDialog.Builder(this@MainMenuActivity)
                            .setView(dialogView)
                            .create()
                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        dialogView.findViewById<Button>(R.id.collectButton)?.setOnClickListener { dialog.dismiss() }
                        dialog.show()
                        
                        // Play coin sound
                        soundManager.playCoinSound()
                    } else {
                        Toast.makeText(this@MainMenuActivity, "Daily Bonus! +1 Coin", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
