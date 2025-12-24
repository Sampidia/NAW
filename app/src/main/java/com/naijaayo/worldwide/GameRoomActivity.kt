package com.naijaayo.worldwide

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.network.parsePlayNowGame
import com.naijaayo.worldwide.PlayNowGame
import com.naijaayo.worldwide.PlayNowPlayer
import com.naijaayo.worldwide.theme.AvatarPreferenceManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GameRoomActivity : AppCompatActivity() {

    private lateinit var createRoomButton: Button
    private lateinit var joinRoomButton: Button
    private lateinit var searchRoomInput: EditText
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_game_room)

        // Initialize views
        createRoomButton = findViewById(R.id.createRoomButton)
        joinRoomButton = findViewById(R.id.searchJoinButton)
        searchRoomInput = findViewById(R.id.searchRoomInput)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        backButton = findViewById(R.id.backButton)


        // Set listeners
        createRoomButton.setOnClickListener {
            showLevelSelectionDialog()
        }

        joinRoomButton.setOnClickListener {
            val code = searchRoomInput.text.toString().trim()
            if (code.isNotEmpty()) {
                joinRoom(code)
            } else {
                Toast.makeText(this, "Please enter a room code", Toast.LENGTH_SHORT).show()
            }
        }



        backButton.setOnClickListener {
            finish()
        }
    }

    private fun showLevelSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_level_selection, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.easyButton).setOnClickListener {
            createRoomWithLevel("EASY")
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.mediumButton).setOnClickListener {
            createRoomWithLevel("MEDIUM")
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.hardButton).setOnClickListener {
            createRoomWithLevel("HARD")
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.rulesButton).setOnClickListener {
            showRules()
        }
        
        // Ensure white background as requested
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
        dialog.show()
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



    private fun createRoomWithLevel(level: String) {
        loadingProgressBar.visibility = View.VISIBLE
        createRoomButton.isEnabled = false

        lifecycleScope.launch {
            val user = FirebaseManager.auth.currentUser
            if (user != null) {

                 // check coin balance
                val userProfile = FirebaseManager.getUserProfile(user.uid)
                val coinBalance = (userProfile?.get("coinBalance") as? Long) ?: 0L

                if (coinBalance < 1) {
                    loadingProgressBar.visibility = View.GONE
                    createRoomButton.isEnabled = true
                    
                    val dialog = com.naijaayo.worldwide.ui.TopUpDialog(this@GameRoomActivity, 
                        onBuyCoinClicked = {
                            val intent = Intent(this@GameRoomActivity, com.naijaayo.worldwide.ui.BuyCoinActivity::class.java)
                            startActivity(intent)
                        },
                        onWatchAdClicked = {
                            com.naijaayo.worldwide.ads.AdMobHelper.showRewardedAd(this@GameRoomActivity, 
                                onRewardEarned = { amount ->
                                   lifecycleScope.launch {
                                       val uid = com.naijaayo.worldwide.network.FirebaseManager.auth.currentUser?.uid
                                       if (uid != null) {
                                           val success = com.naijaayo.worldwide.network.FirebaseManager.addCoins(uid, 1) // +1 Coin
                                           if (success) {
                                               Toast.makeText(this@GameRoomActivity, "Watched Ad! +1 Coin", Toast.LENGTH_SHORT).show()
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
                val deductionSuccess = FirebaseManager.deductCoins(user.uid, 1)
                if (!deductionSuccess) {
                     loadingProgressBar.visibility = View.GONE
                     createRoomButton.isEnabled = true
                     Toast.makeText(this@GameRoomActivity, "Insufficient coins or connection error.", Toast.LENGTH_SHORT).show()
                     return@launch
                }
                
                // Fetch latest profile data to ensure name/avatar are correct
                val profile = FirebaseManager.getUserProfile(user.uid)
                val avatarId = profile?.get("avatarId") as? String ?: AvatarPreferenceManager.getUserAvatar()
                val displayName = profile?.get("displayName") as? String ?: "Player" // Fallback

                val player = PlayNowPlayer(user.uid, displayName, avatarId)
                
                // Generate 6-digit room code
                val roomCode = generateRoomCode()
                val roomId = FirebaseManager.createRoom(player, roomCode, level)
                
                navigateToWaitingRoom(roomId, roomCode)
            } else {
                Toast.makeText(this@GameRoomActivity, "You must be logged in.", Toast.LENGTH_SHORT).show()
            }
            loadingProgressBar.visibility = View.GONE
            createRoomButton.isEnabled = true
        }
    }

    private fun joinRoom(roomCode: String) {
        loadingProgressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val user = FirebaseManager.auth.currentUser
            if (user != null) {
                // First, find the room by code
                val roomId = FirebaseManager.findRoomByCode(roomCode.uppercase())
                
                if (roomId == null) {
                    Toast.makeText(this@GameRoomActivity, "Game Room Code Invalid", Toast.LENGTH_SHORT).show()
                    loadingProgressBar.visibility = View.GONE
                    return@launch
                }
                
                // Fetch latest profile data
                val profile = FirebaseManager.getUserProfile(user.uid)
                val avatarId = profile?.get("avatarId") as? String ?: AvatarPreferenceManager.getUserAvatar()
                val displayName = profile?.get("displayName") as? String ?: "Player"

                val player = PlayNowPlayer(user.uid, displayName, avatarId)
                val success = FirebaseManager.joinPrivateRoom(roomId, player)
                
                if (success) {
                    navigateToWaitingRoom(roomId, roomCode.uppercase())
                } else {
                    // Check if room is full or already started
                    val roomSnapshot = FirebaseManager.gamesRef.child(roomId).get().await()
                    val game = parsePlayNowGame(roomSnapshot)
                    
                    if (game != null && game.players.size >= 2) {
                        Toast.makeText(this@GameRoomActivity, "Game Room Full", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@GameRoomActivity, "Cannot join room. Game may have already started.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this@GameRoomActivity, "You must be logged in.", Toast.LENGTH_SHORT).show()
            }
            loadingProgressBar.visibility = View.GONE
        }
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    private fun navigateToWaitingRoom(roomId: String, roomCode: String) {
        val intent = Intent(this, WaitingRoomActivity::class.java)
        intent.putExtra("ROOM_ID", roomId)
        intent.putExtra("ROOM_CODE", roomCode)
        startActivity(intent)
    }
}
