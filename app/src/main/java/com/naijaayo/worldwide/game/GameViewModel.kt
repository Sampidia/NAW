package com.naijaayo.worldwide.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.naijaayo.worldwide.Room
import com.naijaayo.worldwide.User
import com.naijaayo.worldwide.Avatar
import com.naijaayo.worldwide.GameState
import com.naijaayo.worldwide.repository.GameRepository
import com.naijaayo.worldwide.util.SingleLiveEvent
import kotlinx.coroutines.launch
import org.json.JSONObject

class GameViewModel : ViewModel() {

    private val repository = GameRepository
    private val gson = Gson()

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _leaderboard = MutableLiveData<List<User>>()
    val leaderboard: LiveData<List<User>> = _leaderboard

    // Current user avatar
    private val _currentUserAvatar = MutableLiveData<String>()
    val currentUserAvatar: LiveData<String> = _currentUserAvatar

    // Event to signal navigation to the game screen
    private val _navigateToGame = SingleLiveEvent<Void>()
    val navigateToGame: LiveData<Void> = _navigateToGame

    private var currentRoom: Room? = null
    private val currentUserId = "user1"

    init {
        viewModelScope.launch {
            _gameState.postValue(GameState())
            repository.connectSocket()
            listenForGameUpdates()
        }
    }

    fun createAndJoinRoom(room: Room) {
        viewModelScope.launch {
            try {
                repository.awaitSocketConnection()
                val newRoom = repository.createRoom(room)
                currentRoom = newRoom
                val joinRoomData = JSONObject().apply {
                    put("roomId", newRoom.roomId)
                }
                repository.emitSocketEvent("join_room", joinRoomData)

                // Only navigate after the room is successfully created and joined
                _navigateToGame.call()
            } catch (e: Exception) {
                e.printStackTrace()
                // Here you would typically show an error to the user
            }
        }
    }

    fun playMove(pitIndex: Int) {
        val currentState = _gameState.value
        // Check for currentRoom is now critical, as single player won't have one.
        if (currentRoom != null && currentState != null && isValidMove(pitIndex, currentState)) {
            val moveData = JSONObject().apply {
                put("roomId", currentRoom!!.roomId)
                put("pitIndex", pitIndex)
            }
            repository.emitSocketEvent("play_move", moveData)
        }
    }

    fun updateUserAvatar(avatarId: String) {
        viewModelScope.launch {
            try {
                repository.updateAvatar(currentUserId, Avatar(avatarId))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchLeaderboard() {
        viewModelScope.launch {
            try {
                _leaderboard.value = repository.getLeaderboard()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getCurrentUserAvatar() {
        viewModelScope.launch {
            try {
                // For now, use a default avatar since we don't have a getCurrentUser method
                // In a real implementation, this would fetch from the repository/server
                _currentUserAvatar.value = "ayo" // Default avatar
            } catch (e: Exception) {
                _currentUserAvatar.value = "ayo" // Default fallback
            }
        }
    }

    fun refreshCurrentUserAvatar() {
        getCurrentUserAvatar()
    }

    private fun listenForGameUpdates() {
        viewModelScope.launch {
            repository.onSocketEvent("state_update") { stateJson ->
                val event = gson.fromJson(stateJson.toString(), Map::class.java)
                val data = event["data"]
                val newState = gson.fromJson(gson.toJson(data), GameState::class.java)
                _gameState.postValue(newState)
            }
        }
    }

    private fun isValidMove(pitIndex: Int, gameState: GameState): Boolean {
        val player = gameState.currentPlayer
        val isPlayer1 = player == 1
        val isPlayer1Pit = pitIndex in 0..5
        val isPlayer2Pit = pitIndex in 6..11

        return !gameState.gameOver && gameState.pits[pitIndex] > 0 && ((isPlayer1 && isPlayer1Pit) || (!isPlayer1 && isPlayer2Pit))
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnectSocket()
    }
}
