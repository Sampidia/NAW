package com.naijaayo.worldwide

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
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
            val newRoom = Room(
                roomId = "room_${UUID.randomUUID()}",
                hostUid = "user1", // Replace with actual user ID
                type = "public"
            )
            gameViewModel.createAndJoinRoom(newRoom)
        }
    }

    private fun setupRecyclerView() {
        roomAdapter = RoomAdapter(emptyList()) { room ->
            joinRoom(room)
        }
        roomsRecyclerView.adapter = roomAdapter
        roomsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun joinRoom(room: Room) {
        gameViewModel.createAndJoinRoom(room)
    }

    private fun setupNavigation() {
        gameViewModel.navigateToGame.observe(this, Observer {
            startGameActivity()
        })
    }

    private fun startGameActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("isSinglePlayer", false)
        }
        startActivity(intent)
    }
}
