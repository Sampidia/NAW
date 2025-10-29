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
import com.naijaayo.worldwide.GameResult
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

    fun getCurrentRoom(): Room? = currentRoom
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
                // Save avatar preference locally instead of server call
                com.naijaayo.worldwide.theme.AvatarPreferenceManager.setUserAvatar(avatarId)

                // Immediately update the LiveData to trigger UI updates
                _currentUserAvatar.value = avatarId
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

    fun onGameCompleted(gameState: GameState, isSinglePlayer: Boolean) {
        viewModelScope.launch {
            try {
                // Update user statistics based on game result
                updateUserStats(gameState, isSinglePlayer)

                // Refresh leaderboard data after stats update
                fetchLeaderboard()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateUserStats(gameState: GameState, isSinglePlayer: Boolean) {
        viewModelScope.launch {
            try {
                // Determine game outcome
                val playerWon = gameState.winner == 1
                val playerLost = gameState.winner == 2
                val isDraw = gameState.winner == 0 || gameState.winner == -1

                // Create game result for server submission
                val gameResult = com.naijaayo.worldwide.GameResult(
                    player1Id = currentUserId,
                    player2Id = if (isSinglePlayer) null else "opponent_id", // TODO: Get actual opponent ID
                    player1Score = gameState.player1Score,
                    player2Score = gameState.player2Score,
                    winner = gameState.winner ?: 0, // Default to draw if null
                    isSinglePlayer = isSinglePlayer,
                    gameMode = if (isSinglePlayer) "single" else "multiplayer",
                    completedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())
                )

                try {
                    // Submit game result to server for statistics tracking
                    repository.submitGameResult(gameResult)
                    println("Game result submitted successfully")
                } catch (e: Exception) {
                    println("Failed to submit game result: ${e.message}")
                    // Continue with leaderboard refresh even if submission fails
                }

                // Refresh leaderboard data after game completion
                fetchLeaderboard()

            } catch (e: Exception) {
                println("Error updating user stats: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun getCurrentUserAvatar() {
        viewModelScope.launch {
            try {
                // Load avatar preference from local storage
                val savedAvatar = com.naijaayo.worldwide.theme.AvatarPreferenceManager.getUserAvatar()
                _currentUserAvatar.value = savedAvatar
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
