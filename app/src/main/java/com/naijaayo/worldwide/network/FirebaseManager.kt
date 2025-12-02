package com.naijaayo.worldwide.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.naijaayo.worldwide.leaderboard.LeaderboardEntry
import kotlinx.coroutines.tasks.await



object FirebaseManager {

    // --- Firebase Instances ---
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val realtimeDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }

    // --- Database References ---
    private val onlineUsersRef = realtimeDatabase.getReference("status/online_users")
    private val matchmakingPoolRef = realtimeDatabase.getReference("matchmaking_pool")
    internal val gamesRef = realtimeDatabase.getReference("games")
    private val savedGamesRef = realtimeDatabase.getReference("saved_games")
    private val usersRef = firestore.collection("users")

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
    suspend fun createRoom(player: Player, roomCode: String, level: String = "MEDIUM"): String {
        val roomId = (100000..999999).random().toString() // Generate 6-digit ID
        val NetworkGame = NetworkGame(
            roomId = roomId,
            roomCode = roomCode,
            creatorUid = player.uid,
            players = mapOf(player.uid!! to player),
            status = "waiting",
            isPrivate = false, // Public by default now
            level = level
        )
        gamesRef.child(roomId).setValue(NetworkGame).await()
        return roomId
    }

    suspend fun startGame(roomId: String) {
        val roomRef = gamesRef.child(roomId)
        val snapshot = roomRef.get().await()
        val NetworkGame = snapshot.getValue(NetworkGame::class.java) ?: return
        
        // Initialize NetworkGame state with first player's turn
        val firstPlayerUid = NetworkGame.players.values.firstOrNull()?.uid
        val initialGameState = GameState(
            board = List(12) { 4 },
            nextPlayerUid = firstPlayerUid,
            winnerUid = null,
            gameOver = false
        )
        
        roomRef.child("status").setValue("playing").await()
        roomRef.child("gameState").setValue(initialGameState).await()
    }

    suspend fun joinPrivateRoom(roomId: String, player: Player): Boolean {
        return try {
            val roomRef = gamesRef.child(roomId)
            val snapshot = roomRef.get().await()
            val NetworkGame = snapshot.getValue(NetworkGame::class.java)
            
            if (NetworkGame != null && NetworkGame.status == "waiting" && NetworkGame.players.size < 2) {
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


    fun listenForPublicRooms(onRoomsUpdated: (rooms: List<NetworkGame>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = mutableListOf<NetworkGame>()
                for (child in snapshot.children) {
                    val NetworkGame = child.getValue(NetworkGame::class.java)
                    // Show all rooms that are waiting, regardless of private flag (since we want to see created rooms)
                    if (NetworkGame != null && NetworkGame.status == "waiting") {
                        rooms.add(NetworkGame)
                    }
                }
                onRoomsUpdated(rooms)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        gamesRef.addValueEventListener(listener)
        return listener
    }

    // --- In-NetworkGame Logic ---
    fun listenForGameStateUpdates(roomId: String, onUpdate: (NetworkGame: NetworkGame) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val NetworkGame = snapshot.getValue(NetworkGame::class.java)
                if (NetworkGame != null) {
                    onUpdate(NetworkGame)
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
            val NetworkGame = snapshot.getValue(NetworkGame::class.java) ?: return
            
            // Get current user
            val currentUid = auth.currentUser?.uid ?: return
            
            // Verify it's the player's turn
            if (NetworkGame.gameState.nextPlayerUid != currentUid) {
                android.util.Log.w("FirebaseManager", "Not player's turn")
                return
            }
            
            // Convert Firebase GameState to Local GameState
            val level = when (NetworkGame.level) {
                "EASY" -> com.naijaayo.worldwide.GameLevel.EASY
                "HARD" -> com.naijaayo.worldwide.GameLevel.HARD
                else -> com.naijaayo.worldwide.GameLevel.MEDIUM
            }
            
            // Determine player number (1 or 2)
            val playersList = NetworkGame.players.values.toList()
            val playerNumber = if (playersList.getOrNull(0)?.uid == currentUid) 1 else 2
            
            val localGameState = com.naijaayo.worldwide.LocalGameState(
                pits = NetworkGame.gameState.board.toIntArray(),
                player1Score = NetworkGame.gameState.player1Score, // Use existing scores from Firebase
                player2Score = NetworkGame.gameState.player2Score,
                currentPlayer = playerNumber,
                gameOver = NetworkGame.gameState.gameOver,
                level = level
            )
            
            // Use LocalGameEngine to calculate the move
            val gameEngine = com.naijaayo.worldwide.game.LocalGameEngine()
            val newLocalState = gameEngine.makeMove(localGameState, pitIndex, playerNumber)
            
            if (newLocalState != null) {
                android.util.Log.d("FirebaseManager", "Move calculated. GameOver: ${newLocalState.gameOver}, P1: ${newLocalState.player1Score}, P2: ${newLocalState.player2Score}")
                android.util.Log.d("FirebaseManager", "Board: ${newLocalState.pits.contentToString()}")
            }
            
            
            if (newLocalState == null) {
                android.util.Log.w("FirebaseManager", "Invalid move")
                return
            }
            
            // Determine next player
            val nextPlayerUid = if (newLocalState!!.currentPlayer == 1) {
                (playersList.getOrNull(0) as? Player)?.uid
            } else {
                (playersList.getOrNull(1) as? Player)?.uid
            }
            
            // Convert back to Firebase GameState
            val newFirebaseState = GameState(
                board = newLocalState!!.pits.toList(),
                nextPlayerUid = nextPlayerUid,
                winnerUid = if (newLocalState!!.gameOver) {
                    when (newLocalState!!.winner) {
                        1 -> (playersList.getOrNull(0) as? Player)?.uid
                        2 -> (playersList.getOrNull(1) as? Player)?.uid
                        else -> null
                    }
                } else null,
                gameOver = newLocalState!!.gameOver,
                lastMovePitIndex = pitIndex,
                lastMovePlayerUid = currentUid,
                player1Score = newLocalState!!.player1Score,
                player2Score = newLocalState!!.player2Score
            )
            
            // Update Firebase
            roomRef.child("gameState").setValue(newFirebaseState).await()
            android.util.Log.d("FirebaseManager", "Move completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error making move", e)
        }
    }
    fun removeListener(listener: ValueEventListener, ref: DatabaseReference? = null) { (ref ?: gamesRef).removeEventListener(listener) }

    // --- Save / Resume Game Logic ---

    fun saveGame(roomCode: String, onComplete: (Boolean, String) -> Unit) {
        // 1. Fetch current game state from 'games/{roomCode}'
        gamesRef.child(roomCode).get().addOnSuccessListener { snapshot ->
            val game = snapshot.getValue(NetworkGame::class.java)
            if (game != null) {
                // 2. Save to 'saved_games/{roomCode}'
                savedGamesRef.child(roomCode).setValue(game).addOnSuccessListener {
                    // 3. Add to both players' saved game lists in Firestore
                    val players = game.players.values.toList()
                    val batch = firestore.batch()
                    
                    players.forEach { player ->
                        player.uid?.let { uid ->
                            val savedGameRef = usersRef.document(uid).collection("saved_games_list").document(roomCode)
                            val savedGameInfo = mapOf(
                                "roomCode" to roomCode,
                                "player1Name" to (players.getOrNull(0)?.displayName ?: "Player 1"),
                                "player1Avatar" to (players.getOrNull(0)?.avatarId ?: "char_ayo_portrait"),
                                "player2Name" to (players.getOrNull(1)?.displayName ?: "Player 2"),
                                "player2Avatar" to (players.getOrNull(1)?.avatarId ?: "char_ayo_portrait"),
                                "timestamp" to System.currentTimeMillis()
                            )
                            batch.set(savedGameRef, savedGameInfo)
                        }
                    }
                    
                    batch.commit().addOnSuccessListener {
                        onComplete(true, "Game saved successfully")
                    }.addOnFailureListener { e ->
                        onComplete(false, "Failed to update player lists: ${e.message}")
                    }
                }.addOnFailureListener { e ->
                    onComplete(false, "Failed to save game data: ${e.message}")
                }
            } else {
                onComplete(false, "Game not found")
            }
        }.addOnFailureListener { e ->
            onComplete(false, "Failed to fetch game: ${e.message}")
        }
    }

    suspend fun resumeGame(roomCode: String): Boolean {
        return try {
            // 1. Check if active game exists
            val activeGameSnapshot = gamesRef.child(roomCode).get().await()
            if (activeGameSnapshot.exists()) {
                // Game is already active, just join it
                return true
            }

            // 2. If not active, fetch from saved_games
            val savedGameSnapshot = savedGamesRef.child(roomCode).get().await()
            val savedGame = savedGameSnapshot.getValue(NetworkGame::class.java)

            if (savedGame != null) {
                // 3. Rehydrate: Write back to 'games/{roomCode}'
                // Set status to 'playing' just in case it was 'waiting' or something else
                val rehydratedGame = savedGame.copy(status = "playing")
                gamesRef.child(roomCode).setValue(rehydratedGame).await()
                return true
            } else {
                return false // Saved game not found
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error resuming game", e)
            return false
        }
    }

    suspend fun getSavedGames(): List<Map<String, Any>> {
        return try {
            val uid = auth.currentUser?.uid ?: return emptyList()
            val snapshot = usersRef.document(uid).collection("saved_games_list")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            emptyList()
        }
    }


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
    // --- NetworkGame Statistics Tracking ---
    enum class MatchResult {
        WIN, LOSS, DRAW
    }

    suspend fun recordSinglePlayerResult(
        userId: String,
        result: MatchResult,
        displayName: String,
        avatarId: String
    ) {
        val userDocRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val stats = snapshot.get("stats") as? Map<String, Any> ?: emptyMap()
            val singlePlayerStats = stats["singlePlayer"] as? MutableMap<String, Any> ?: mutableMapOf()
            
            val wins = (singlePlayerStats["wins"] as? Long) ?: 0L
            val losses = (singlePlayerStats["losses"] as? Long) ?: 0L
            val draws = (singlePlayerStats["draws"] as? Long) ?: 0L
            val totalPoints = (singlePlayerStats["totalPoints"] as? Long) ?: 0L
            
            when (result) {
                MatchResult.WIN -> {
                    singlePlayerStats["wins"] = wins + 1
                    singlePlayerStats["totalPoints"] = totalPoints + 3
                }
                MatchResult.LOSS -> {
                    singlePlayerStats["losses"] = losses + 1
                }
                MatchResult.DRAW -> {
                    singlePlayerStats["draws"] = draws + 1
                    singlePlayerStats["totalPoints"] = totalPoints + 1
                }
            }
            
            val updatedStats = stats.toMutableMap()
            updatedStats["singlePlayer"] = singlePlayerStats
            
            transaction.update(userDocRef, "stats", updatedStats)
            null
        }.await()
    }

    suspend fun recordMultiplayerResult(
        player1Id: String,
        player2Id: String,
        winnerId: String?, // null for draw
        player1Name: String,
        player2Name: String,
        player1Avatar: String,
        player2Avatar: String
    ) {
        // Determine results for both players
        val player1Result = when {
            winnerId == null -> MatchResult.DRAW
            winnerId == player1Id -> MatchResult.WIN
            else -> MatchResult.LOSS
        }
        
        val player2Result = when {
            winnerId == null -> MatchResult.DRAW
            winnerId == player2Id -> MatchResult.WIN
            else -> MatchResult.LOSS
        }
        
        // Update both players' stats
        updatePlayerMultiplayerStats(player1Id, player1Result)
        updatePlayerMultiplayerStats(player2Id, player2Result)
    }
    
    private suspend fun updatePlayerMultiplayerStats(userId: String, result: MatchResult) {
        val userDocRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val stats = snapshot.get("stats") as? Map<String, Any> ?: emptyMap()
            val multiplayerStats = stats["multiplayer"] as? MutableMap<String, Any> ?: mutableMapOf()
            
            val wins = (multiplayerStats["wins"] as? Long) ?: 0L
            val losses = (multiplayerStats["losses"] as? Long) ?: 0L
            val draws = (multiplayerStats["draws"] as? Long) ?: 0L
            val totalPoints = (multiplayerStats["totalPoints"] as? Long) ?: 0L
            
            when (result) {
                MatchResult.WIN -> {
                    multiplayerStats["wins"] = wins + 1
                    multiplayerStats["totalPoints"] = totalPoints + 3
                }
                MatchResult.LOSS -> {
                    multiplayerStats["losses"] = losses + 1
                }
                MatchResult.DRAW -> {
                    multiplayerStats["draws"] = draws + 1
                    multiplayerStats["totalPoints"] = totalPoints + 1
                }
            }
            
            val updatedStats = stats.toMutableMap()
            updatedStats["multiplayer"] = multiplayerStats
            
            transaction.update(userDocRef, "stats", updatedStats)
            null
        }.await()
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

            firestore.runTransaction { transaction ->
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

    suspend fun resetPassword(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error sending reset password email", e)
            false
        }
    }
}
