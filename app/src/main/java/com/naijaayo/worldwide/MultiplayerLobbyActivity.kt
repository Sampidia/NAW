package com.naijaayo.worldwide

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import com.naijaayo.worldwide.auth.AuthDialog
import com.naijaayo.worldwide.auth.SessionManager
import com.naijaayo.worldwide.theme.NigerianThemeManager
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.naijaayo.worldwide.game.GameViewModel
import com.naijaayo.worldwide.ui.LobbyViewModel
import com.naijaayo.worldwide.ui.RoomAdapter
import java.util.UUID

class MultiplayerLobbyActivity : AppCompatActivity() {

    private val lobbyViewModel: LobbyViewModel by viewModels()
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var roomsRecyclerView: RecyclerView
    private lateinit var createRoomButton: Button
    private lateinit var roomAdapter: RoomAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize session manager
        SessionManager.initialize(this)

        // Check authentication before allowing access to multiplayer
        if (!SessionManager.isLoggedIn()) {
            showAuthDialog()
            return
        }

        // Initialize lobby if user is authenticated
        initializeLobby()
    }

    private fun setupRecyclerView() {
        roomAdapter = RoomAdapter(emptyList()) { room ->
            joinRoom(room)
        }
        roomsRecyclerView.adapter = roomAdapter
        roomsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun joinRoom(room: Room) {
        // Use GameViewModel to join existing room
        gameViewModel.createAndJoinRoom(room)

        // Navigate to waiting room immediately for joining existing rooms
        val intent = Intent(this, WaitingRoomActivity::class.java).apply {
            putExtra("roomId", room.roomId)
            putExtra("hostUsername", room.hostUsername)
            putExtra("difficulty", room.difficulty.name)
            putExtra("isHost", false) // Joiner is not host
        }
        startActivity(intent)
    }

    private fun showCreateRoomDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_create_room)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        // Increase dialog width
        val window = dialog.window
        window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(), // 85% of screen width
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )

        val difficultyRadioGroup = dialog.findViewById<RadioGroup>(R.id.difficultyRadioGroup)
        val createButton = dialog.findViewById<Button>(R.id.createButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        createButton.setOnClickListener {
            val selectedDifficulty = when (difficultyRadioGroup.checkedRadioButtonId) {
                R.id.easyRadioButton -> GameLevel.EASY
                R.id.hardRadioButton -> GameLevel.HARD
                else -> GameLevel.MEDIUM
            }

            // Get current user info from session
            val currentUser = SessionManager.getCurrentUser()
            if (currentUser == null) {
                showAuthDialog()
                dialog.dismiss()
                return@setOnClickListener
            }
            val currentUserId = currentUser.id
            val currentUsername = currentUser.username
            val currentAvatarId = currentUser.avatarId

            val newRoom = Room(
                roomId = "room_${UUID.randomUUID()}",
                hostUid = currentUserId,
                hostUsername = currentUsername,
                hostAvatarId = currentAvatarId,
                difficulty = selectedDifficulty,
                type = "public"
            )

            // Directly create room locally first (for immediate UI update)
            // Then use GameViewModel to sync with server
            lobbyViewModel.addRoom(newRoom) // Add to local list immediately

            // Use GameViewModel to create room on server and join via WebSocket
            gameViewModel.createAndJoinRoom(newRoom)

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupNavigation() {
        gameViewModel.navigateToGame.observe(this, Observer {
            // Navigate to waiting room when room is created and joined
            val currentRoom = gameViewModel.getCurrentRoom()
            if (currentRoom != null) {
                val intent = Intent(this, WaitingRoomActivity::class.java).apply {
                    putExtra("roomId", currentRoom.roomId)
                    putExtra("hostUsername", currentRoom.hostUsername)
                    putExtra("difficulty", currentRoom.difficulty.name)
                    putExtra("isHost", true) // Creator is host
                }
                startActivity(intent)
            }
        })
    }

    private fun showAuthDialog() {
        val authDialog = AuthDialog(this) { userId, username, avatarId ->
            // Authentication successful, proceed with normal initialization
            initializeLobby()
        }
        authDialog.show()
    }

    private fun initializeLobby() {
        // Initialize theme manager and apply current theme
        NigerianThemeManager.initialize(this)
        NigerianThemeManager.applyThemeToActivity(this)

        // Hide action bar to show only the logo image
        supportActionBar?.hide()

        setContentView(R.layout.activity_multiplayer_lobby)

        roomsRecyclerView = findViewById(R.id.roomsRecyclerView)
        createRoomButton = findViewById(R.id.createRoomButton)

        setupRecyclerView()
        setupNavigation()

        lobbyViewModel.rooms.observe(this, Observer { rooms ->
            roomAdapter.setData(rooms)
        })

        lobbyViewModel.fetchRooms()

        createRoomButton.setOnClickListener {
            showCreateRoomDialog()
        }
    }

    override fun onResume() {
         super.onResume()
         // Reapply theme when activity becomes visible (e.g., after returning from theme settings)
         NigerianThemeManager.applyThemeToActivity(this)
         // Resume background music
         com.naijaayo.worldwide.sound.BackgroundMusicManager.resumeBackgroundMusic()

         // Only refresh rooms if user is logged in
         if (SessionManager.isLoggedIn()) {
             lobbyViewModel.fetchRooms()
         }
     }
}
