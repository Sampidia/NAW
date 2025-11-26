package com.naijaayo.worldwide.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.naijaayo.worldwide.leaderboard.LeaderboardEntry
import kotlinx.coroutines.tasks.await

// --- Data classes for Realtime Database game structure ---

@IgnoreExtraProperties
data class GameState(
    val board: List<Int> = List(12) { 4 },
    val nextPlayerUid: String? = null,
    val winnerUid: String? = null,
    val isGameOver: Boolean = false
)

@IgnoreExtraProperties
data class Player(
    val uid: String? = null,
    val displayName: String? = null,
    val avatarId: String? = null
)

@IgnoreExtraProperties
data class Game(
    val roomId: String? = null,
    val roomCode: String? = null,
    val creatorUid: String? = null,
    val players: Map<String, Player> = emptyMap(),
    val status: String = "waiting",
    val gameState: GameState = GameState(),
    val isPrivate: Boolean = false,
    val level: String = "MEDIUM"
)

object FirebaseManager {

    // --- Firebase Instances ---
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val realtimeDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    // --- Database References ---
    private val onlineUsersRef = realtimeDatabase.getReference("status/online_users")
    private val matchmakingPoolRef = realtimeDatabase.getReference("matchmaking_pool")
    internal val gamesRef = realtimeDatabase.getReference("games")

    private var onDisconnectHandler: OnDisconnect? = null

    // --- Session Management ---
    suspend fun joinGameSession(): Boolean { /* ... implementation from before ... */ return false }
    fun leaveGameSession() { /* ... */ }

    // --- "Play Now" - Matchmaking ---
    suspend fun findMatch(player: Player) { /* ... */ }
    fun listenForGameMatch(uid: String, onMatchFound: (gameId: String) -> Unit): ValueEventListener { 
        /* ... */ 
        return gamesRef.addValueEventListener(object: ValueEventListener{ 
            override fun onDataChange(snapshot: DataSnapshot) {} 
            override fun onCancelled(error: DatabaseError) {}
        }) 
    }
    fun cancelMatchmaking(uid: String) { /* ... */ }

    // --- "Play with Friends" - Room Management ---
    suspend fun createPrivateRoom(player: Player, roomCode: String, level: String = "MEDIUM"): String {
        val roomId = (100000..999999).random().toString() // Generate 6-digit ID
        val game = Game(
            roomId = roomId,
            roomCode = roomCode,
            creatorUid = player.uid,
            players = mapOf(player.uid!! to player),
            status = "waiting",
            isPrivate = true,
            level = level
        )
        gamesRef.child(roomId).setValue(game).await()
        return roomId
    }

    suspend fun startGame(roomId: String) {
        val roomRef = gamesRef.child(roomId)
        val snapshot = roomRef.get().await()
        val game = snapshot.getValue(Game::class.java) ?: return
        
        // Initialize game state with first player's turn
        val firstPlayerUid = game.players.values.firstOrNull()?.uid
        val initialGameState = GameState(
            board = List(12) { 4 },
            nextPlayerUid = firstPlayerUid,
            winnerUid = null,
            isGameOver = false
        )
        
        roomRef.child("status").setValue("playing").await()
        roomRef.child("gameState").setValue(initialGameState).await()
    }

    suspend fun joinPrivateRoom(roomId: String, player: Player): Boolean {
        return try {
            val roomRef = gamesRef.child(roomId)
            val snapshot = roomRef.get().await()
            val game = snapshot.getValue(Game::class.java)
            
            if (game != null && game.status == "waiting" && game.players.size < 2) {
                roomRef.child("players").child(player.uid!!).setValue(player).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun findRoomByCode(roomCode: String): String? {
        return try {
            val snapshot = gamesRef.orderByChild("roomCode").equalTo(roomCode).get().await()
            if (snapshot.exists()) {
                snapshot.children.firstOrNull()?.key
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error finding room by code", e)
            null
        }
    }


    fun listenForPublicRooms(onRoomsUpdated: (rooms: List<Game>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = mutableListOf<Game>()
                for (child in snapshot.children) {
                    val game = child.getValue(Game::class.java)
                    if (game != null && game.isPrivate) {
                        rooms.add(game)
                    }
                }
                onRoomsUpdated(rooms)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.addValueEventListener(listener)
        return listener
    }

    // --- In-Game Logic ---
    fun listenForGameStateUpdates(roomId: String, onUpdate: (game: Game) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = snapshot.getValue(Game::class.java)
                if (game != null) {
                    onUpdate(game)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.child(roomId).addValueEventListener(listener)
        return listener
    }

    suspend fun makeMove(roomId: String, pitIndex: Int) {
        try {
            val roomRef = gamesRef.child(roomId)
            val snapshot = roomRef.get().await()
            val game = snapshot.getValue(Game::class.java) ?: return
            
            // Get current user
            val currentUid = auth.currentUser?.uid ?: return
            
            // Verify it's the player's turn
            if (game.gameState.nextPlayerUid != currentUid) {
                android.util.Log.w("FirebaseManager", "Not player's turn")
                return
            }
            
            // Convert Firebase GameState to Local GameState
            val level = when (game.level) {
                "EASY" -> com.naijaayo.worldwide.GameLevel.EASY
                "HARD" -> com.naijaayo.worldwide.GameLevel.HARD
                else -> com.naijaayo.worldwide.GameLevel.MEDIUM
            }
            
            // Determine player number (1 or 2)
            val playersList = game.players.values.toList()
            val playerNumber = if (playersList.getOrNull(0)?.uid == currentUid) 1 else 2
            
            val localGameState = com.naijaayo.worldwide.LocalGameState(
                pits = game.gameState.board.toIntArray(),
                player1Score = 0, // Scores tracked separately in multiplayer
                player2Score = 0,
                currentPlayer = playerNumber,
                gameOver = game.gameState.isGameOver,
                level = level
            )
            
            // Use LocalGameEngine to calculate the move
            val gameEngine = com.naijaayo.worldwide.game.LocalGameEngine()
            val newLocalState = gameEngine.makeMove(localGameState, pitIndex, playerNumber)
            
            if (newLocalState == null) {
                android.util.Log.w("FirebaseManager", "Invalid move")
                return
            }
            
            // Determine next player
            val nextPlayerUid = if (newLocalState.currentPlayer == 1) {
                playersList.getOrNull(0)?.uid
            } else {
                playersList.getOrNull(1)?.uid
            }
            
            // Convert back to Firebase GameState
            val newFirebaseState = GameState(
                board = newLocalState.pits.toList(),
                nextPlayerUid = nextPlayerUid,
                winnerUid = if (newLocalState.gameOver) {
                    when (newLocalState.winner) {
                        1 -> playersList.getOrNull(0)?.uid
                        2 -> playersList.getOrNull(1)?.uid
                        else -> null
                    }
                } else null,
                isGameOver = newLocalState.gameOver
            )
            
            // Update Firebase
            roomRef.child("gameState").setValue(newFirebaseState).await()
            android.util.Log.d("FirebaseManager", "Move completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error making move", e)
        }
    }
    fun removeListener(listener: ValueEventListener, ref: DatabaseReference? = null) { (ref ?: gamesRef).removeEventListener(listener) }

    // --- New, Specific Leaderboard Logic ---

    suspend fun getSinglePlayerLeaderboard(): List<LeaderboardEntry> {
        return getLeaderboardFromCollection("leaderboard_sp")
    }

    suspend fun getMultiplayerLeaderboard(): List<LeaderboardEntry> {
        return getLeaderboardFromCollection("leaderboard_mp")
    }

    suspend fun updateSinglePlayerScore(scoreToAdd: Long, displayName: String, avatarId: String) {
        updateScoreInCollection("leaderboard_sp", scoreToAdd, displayName, avatarId)
    }

    suspend fun updateMultiplayerScore(scoreToAdd: Long, displayName: String, avatarId: String) {
        updateScoreInCollection("leaderboard_mp", scoreToAdd, displayName, avatarId)
    }

    // Private helper functions to avoid code duplication
    private suspend fun getLeaderboardFromCollection(collectionName: String): List<LeaderboardEntry> {
        return try {
            val snapshot = firestore.collection(collectionName)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            snapshot.toObjects(LeaderboardEntry::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun updateScoreInCollection(collectionName: String, scoreToAdd: Long, displayName: String, avatarId: String) {
        auth.currentUser?.let { user ->
            val userDocRef = firestore.collection(collectionName).document(user.uid)

            firestore.runTransaction {
                transaction ->
                val snapshot = transaction.get(userDocRef)
                val currentScore = snapshot.getLong("score") ?: 0L
                val newScore = currentScore + scoreToAdd

                val entry = LeaderboardEntry(displayName, newScore, avatarId)
                transaction.set(userDocRef, entry)
                null // Transactions must return null
            }.await()
        }
    }

    // --- User Profile Management ---
    suspend fun getUserProfile(uid: String): Map<String, Any>? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun registerUser(email: String, password: String, username: String): Boolean {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return false
            
            // Save initial profile with username
            val profile = mapOf(
                "username" to username,
                "displayName" to username, // Use username as display name by default
                "email" to email,
                "avatarId" to "ayo", // Default avatar
                "score" to 0L
            )
            saveUserProfile(uid, profile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginUser(input: String, password: String): Boolean {
        android.util.Log.d("FirebaseManager", "=== LOGIN ATTEMPT START ===")
        android.util.Log.d("FirebaseManager", "Input: $input")
        return try {
            var email = input
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                // Input is likely a username, look up the email
                android.util.Log.d("FirebaseManager", "Input is NOT an email, treating as username")
                android.util.Log.d("FirebaseManager", "Querying Firestore for username: $input")
                
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("username", input)
                    .limit(1)
                    .get()
                    .await()
                
                android.util.Log.d("FirebaseManager", "Query completed. isEmpty: ${querySnapshot.isEmpty}")
                android.util.Log.d("FirebaseManager", "Query size: ${querySnapshot.size()}")
                
                if (!querySnapshot.isEmpty) {
                    email = querySnapshot.documents[0].getString("email") ?: ""
                    android.util.Log.d("FirebaseManager", "Found email for username: $email")
                    
                    if (email.isEmpty()) {
                        android.util.Log.e("FirebaseManager", "Email field is empty in user document")
                        return false
                    }
                } else {
                    android.util.Log.e("FirebaseManager", "Username not found in Firestore: $input")
                    android.util.Log.e("FirebaseManager", "Make sure the user was registered with this username")
                    return false // Username not found
                }
            } else {
                android.util.Log.d("FirebaseManager", "Input is an email address")
            }
            
            android.util.Log.d("FirebaseManager", "Attempting Firebase Auth login with email: $email")
            auth.signInWithEmailAndPassword(email, password).await()
            android.util.Log.d("FirebaseManager", "Login successful!")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Login failed with exception: ${e.javaClass.simpleName}")
            android.util.Log.e("FirebaseManager", "Error message: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun saveUserProfile(uid: String, data: Map<String, Any>): Boolean {
        return try {
            firestore.collection("users").document(uid).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveUserAvatar(uid: String, avatarId: String): Boolean {
        return saveUserProfile(uid, mapOf("avatarId" to avatarId))
    }
}
