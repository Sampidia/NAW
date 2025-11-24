package com.naijaayo.worldwide

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.ValueEventListener
import com.naijaayo.worldwide.network.FirebaseManager
import com.naijaayo.worldwide.network.Game
import com.naijaayo.worldwide.theme.AvatarPreferenceManager

class WaitingRoomActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var closeButton: ImageButton
    private lateinit var roomCodeDisplay: TextView
    private lateinit var playerCountText: TextView
    private lateinit var player1Avatar: ImageView
    private lateinit var player1Name: TextView
    private lateinit var removePlayer1Button: ImageButton
    private lateinit var player2Avatar: ImageView
    private lateinit var player2Name: TextView
    private lateinit var removePlayer2Button: ImageButton
    private lateinit var addPlayerButton: Button
    private lateinit var infoBannerText: TextView
    private lateinit var letsPlayButton: Button

    private var roomId: String? = null
    private var roomCode: String? = null
    private var gameListener: ValueEventListener? = null
    private var currentGame: Game? = null
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_waiting_room)

        roomId = intent.getStringExtra("ROOM_ID")
        roomCode = intent.getStringExtra("ROOM_CODE")
        currentUserId = FirebaseManager.auth.currentUser?.uid

        if (roomId == null || currentUserId == null) {
            finish()
            return
        }

        initializeViews()
        setupListeners()
        displayRoomCode()
    }

    override fun onResume() {
        super.onResume()
        listenForGameUpdates()
    }

    override fun onPause() {
        super.onPause()
        gameListener?.let { FirebaseManager.removeListener(it) }
    }

    private fun initializeViews() {
        backButton = findViewById(R.id.backButton)
        closeButton = findViewById(R.id.closeButton)
        roomCodeDisplay = findViewById(R.id.roomCodeDisplay)
        playerCountText = findViewById(R.id.playerCountText)
        player1Avatar = findViewById(R.id.player1Avatar)
        player1Name = findViewById(R.id.player1Name)
        removePlayer1Button = findViewById(R.id.removePlayer1Button)
        player2Avatar = findViewById(R.id.player2Avatar)
        player2Name = findViewById(R.id.player2Name)
        removePlayer2Button = findViewById(R.id.removePlayer2Button)
        addPlayerButton = findViewById(R.id.addPlayerButton)
        infoBannerText = findViewById(R.id.infoBannerText)
        letsPlayButton = findViewById(R.id.letsPlayButton)
    }

    private fun setupListeners() {
        backButton.setOnClickListener { finish() }
        closeButton.setOnClickListener { finish() }

        addPlayerButton.setOnClickListener {
            showAddPlayerDialog()
        }

        removePlayer1Button.setOnClickListener {
            // Only creator can remove players
            removePlayer(0)
        }

        removePlayer2Button.setOnClickListener {
            removePlayer(1)
        }

        letsPlayButton.setOnClickListener {
            startGame()
        }
    }

    private fun displayRoomCode() {
        roomCodeDisplay.text = roomCode ?: "N/A"
    }

    private fun listenForGameUpdates() {
        roomId?.let { id ->
            gameListener = FirebaseManager.listenForGameStateUpdates(id) { game ->
                currentGame = game
                updateUI(game)
            }
        }
    }

    private fun updateUI(game: Game) {
        val players = game.players.values.toList()
        val playerCount = players.size

        // Update player count
        playerCountText.text = "$playerCount/2 players"

        // Update Player 1
        if (players.isNotEmpty()) {
            val p1 = players[0]
            player1Avatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(p1.avatarId ?: "ayo"))
            player1Name.text = p1.displayName
            player1Avatar.visibility = View.VISIBLE
            player1Name.visibility = View.VISIBLE
            
            // Show remove button only if current user is creator and this is not the creator
            removePlayer1Button.visibility = if (currentUserId == game.creatorUid && p1.uid != game.creatorUid) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Update Player 2
        if (players.size > 1) {
            val p2 = players[1]
            player2Avatar.setImageResource(AvatarPreferenceManager.getAvatarPortrait(p2.avatarId ?: "ayo"))
            player2Name.text = p2.displayName
            player2Avatar.visibility = View.VISIBLE
            player2Name.visibility = View.VISIBLE
            
            // Show remove button only if current user is creator
            removePlayer2Button.visibility = if (currentUserId == game.creatorUid) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // Hide add player button when full
            addPlayerButton.visibility = View.GONE

            // Enable Let's Play button
            letsPlayButton.isEnabled = true
            letsPlayButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(android.R.color.holo_green_dark, theme)
            )
            infoBannerText.text = "Ready to play!"
        } else {
            // Show add player button
            addPlayerButton.visibility = View.VISIBLE
            
            // Hide player 2 slot
            player2Avatar.visibility = View.GONE
            player2Name.visibility = View.GONE
            removePlayer2Button.visibility = View.GONE

            // Disable Let's Play button
            letsPlayButton.isEnabled = false
            letsPlayButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
                resources.getColor(android.R.color.darker_gray, theme)
            )
            infoBannerText.text = "Waiting for player."
        }
    }

    private fun showAddPlayerDialog() {
        val options = arrayOf("Search by Username", "Invite Friends", "Share Room Code")
        
        AlertDialog.Builder(this)
            .setTitle("Add Player")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSearchUsernameDialog()
                    1 -> showInviteFriendsDialog()
                    2 -> shareRoomCode()
                }
            }
            .show()
    }

    private fun showSearchUsernameDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter username"
        
        AlertDialog.Builder(this)
            .setTitle("Search Player")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val username = input.text.toString().trim()
                if (username.isNotEmpty()) {
                    searchAndAddPlayer(username)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun searchAndAddPlayer(username: String) {
        // TODO: Implement search by username in FirebaseManager
        Toast.makeText(this, "Searching for $username...", Toast.LENGTH_SHORT).show()
        // For now, show placeholder message
        Toast.makeText(this, "Feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showInviteFriendsDialog() {
        // TODO: Implement friends list with online status
        Toast.makeText(this, "Friends list coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun shareRoomCode() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Room Code", roomCode)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(this, "Room code copied: $roomCode", Toast.LENGTH_LONG).show()
    }

    private fun removePlayer(playerIndex: Int) {
        val players = currentGame?.players?.values?.toList()
        if (players != null && playerIndex < players.size) {
            val playerToRemove = players[playerIndex]
            
            // Only creator can remove players
            if (currentUserId == currentGame?.creatorUid) {
                roomId?.let { id ->
                    // TODO: Implement removePlayerFromRoom in FirebaseManager
                    Toast.makeText(this, "Removing ${playerToRemove.displayName}...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startGame() {
        if (currentGame != null && currentGame!!.players.size == 2) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("GAME_ID", roomId)
            intent.putExtra("IS_MULTIPLAYER", true)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Waiting for opponent...", Toast.LENGTH_SHORT).show()
        }
    }
}
