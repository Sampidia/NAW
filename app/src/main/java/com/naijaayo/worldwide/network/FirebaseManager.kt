package com.naijaayo.worldwide.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction
import com.naijaayo.worldwide.leaderboard.LeaderboardEntry
import com.naijaayo.worldwide.model.Friend
import com.naijaayo.worldwide.model.FriendRequest
import com.naijaayo.worldwide.model.User
import kotlinx.coroutines.tasks.await
import com.naijaayo.worldwide.PlayNowGame
import com.naijaayo.worldwide.PlayNowGameState
import com.naijaayo.worldwide.PlayNowPlayer



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
    private val chatsRef = realtimeDatabase.getReference("chats")
    private val usersRef = firestore.collection("users")

    private var onDisconnectHandler: OnDisconnect? = null

    // --- Session Management ---
    // Removed session limit - no longer required

    // --- "Play Now" - Matchmaking ---
    
    // Result of joining matchmaking queue
    sealed class MatchResult {
        data class Matched(val roomId: String, val roomCode: String) : MatchResult()
        data class Waiting(val roomId: String, val roomCode: String) : MatchResult()
        data class Error(val message: String) : MatchResult()
    }
    
    /**
     * Join the matchmaking queue. If another player is waiting, pair with them.
     * Otherwise, add self to queue and wait.
     */
    suspend fun joinMatchmakingQueue(player: PlayNowPlayer): MatchResult {
        return try {
            // 1. Search for an existing public waiting game
            val snapshot = gamesRef.orderByChild("status").equalTo("waiting").get().await()
            
            var matchedRoomId: String? = null
            var matchedRoomCode: String? = null
            
            if (snapshot.exists()) {
                for (child in snapshot.children) {
                    val game = parsePlayNowGame(child)
                    // Filter out games created by self, ensure it's public, has space, AND is a MATCHMAKING game
                    if (game != null && !game.isPrivate && game.players.size < 2 && game.creatorUid != player.uid && game.gameType == "MATCHMAKING") {
                        // Found a suitable game, try to join
                        val joined = joinPrivateRoom(game.roomId!!, player)
                        if (joined) {
                            matchedRoomId = game.roomId
                            matchedRoomCode = game.roomCode
                            
                            // CRITICAL: Automatically start the game for Play Now
                            // Initialize game state
                            val initialGameState = PlayNowGameState(
                                board = List(12) { 4 },
                                nextPlayerUid = game.creatorUid, // Creator starts first
                                winnerUid = null,
                                gameOver = false
                            )
                            
                            val roomRef = gamesRef.child(matchedRoomId!!)
                            roomRef.child("status").setValue("playing").await()
                            roomRef.child("gameState").setValue(initialGameState).await()
                            
                            break
                        }
                    }
                }
            }
            
            if (matchedRoomId != null && matchedRoomCode != null) {
                return MatchResult.Matched(matchedRoomId, matchedRoomCode)
            }
            
            // 2. No suitable game found, create a new public game
            val roomCode = (100000..999999).random().toString()
            val roomId = (100000..999999).random().toString()
            
            val game = PlayNowGame(
                roomId = roomId,
                roomCode = roomCode,
                creatorUid = player.uid,
                players = mapOf(
                    (player.uid ?: "") to player
                ),
                status = "waiting",
                isPrivate = false, // Public game
                level = "EASY",
                gameType = "MATCHMAKING"
            )
            
            gamesRef.child(roomId).setValue(game).await()
            
            // IMPORTANT: Return Waiting so Player 1 waits in dialog
            return MatchResult.Waiting(roomId, roomCode)
            
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Matchmaking error", e)
            MatchResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Listen for when another player matches with this player (for Player 1 waiting)
     */
    fun listenForMatch(roomId: String, onMatchFound: (roomId: String, roomCode: String) -> Unit): ValueEventListener {
        return gamesRef.child(roomId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val game = parsePlayNowGame(snapshot)
                    if (game != null && game.players.size >= 2) {
                        // Match found (someone joined)
                        onMatchFound(game.roomId ?: "", game.roomCode ?: "")
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FirebaseManager", "Match listener cancelled", error.toException())
                }
            })
    }
    
    /**
     * Leave the matchmaking queue (when user cancels)
     */
    fun leaveMatchmakingQueue(uid: String, roomId: String?) {
        if (roomId != null) {
            // Remove the game if we were the creator and waiting
            gamesRef.child(roomId).removeValue()
        }
    }
    
    // Keep old function names for backward compatibility but mark as deprecated
    @Deprecated("Use joinMatchmakingQueue instead", ReplaceWith("joinMatchmakingQueue(player)"))
    suspend fun findMatch(player: PlayNowPlayer) { joinMatchmakingQueue(player) }
    
    @Deprecated("Use listenForMatch instead")


    // --- "Play with Friends" - Room Management ---
    suspend fun createRoom(player: PlayNowPlayer, roomCode: String, level: String = "MEDIUM"): String {
        val roomId = (100000..999999).random().toString() // Generate 6-digit ID
        val game = PlayNowGame(
            roomId = roomId,
            roomCode = roomCode,
            creatorUid = player.uid,
            players = mapOf(player.uid!! to player),
            status = "waiting",
            isPrivate = false, // Public by default now
            level = level,
            gameType = "LOBBY"
        )
        gamesRef.child(roomId).setValue(game).await()
        return roomId
    }

    suspend fun startGame(roomId: String) {
        val roomRef = gamesRef.child(roomId)
        val snapshot = roomRef.get().await()
        val game = parsePlayNowGame(snapshot) ?: return
        
        // Initialize PlayNowGame state with first player's turn
        val firstPlayerUid = game.players.values.firstOrNull()?.uid
        val initialGameState = PlayNowGameState(
            board = List(12) { 4 },
            nextPlayerUid = firstPlayerUid,
            winnerUid = null,
            gameOver = false
        )
        
        roomRef.child("status").setValue("playing").await()
        roomRef.child("gameState").setValue(initialGameState).await()
    }

    suspend fun joinPrivateRoom(roomId: String, player: PlayNowPlayer): Boolean {
        return try {
            val roomRef = gamesRef.child(roomId)
            val snapshot = roomRef.get().await()
            val game = parsePlayNowGame(snapshot)
            
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


    fun listenForPublicRooms(onRoomsUpdated: (rooms: List<PlayNowGame>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = mutableListOf<PlayNowGame>()
                for (child in snapshot.children) {
                    val game = parsePlayNowGame(child)
                    // Show all rooms that are waiting, regardless of private flag (since we want to see created rooms)
                    if (game != null && game.status == "waiting") {
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
    fun listenForGameStateUpdates(roomId: String, onUpdate: (game: PlayNowGame) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val game = parsePlayNowGame(snapshot)
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
            val game = parsePlayNowGame(snapshot) ?: return
            
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
            
            // Determine player number STABLY using creatorUid
            val player1Uid = game.creatorUid
            val player2Uid = game.players.keys.find { it != player1Uid }
            
            android.util.Log.d("FirebaseManager", "makeMove: P1(Creator)=$player1Uid, P2=$player2Uid, Current=$currentUid")
            
            if (player1Uid == null || player2Uid == null) {
                android.util.Log.e("FirebaseManager", "makeMove: CRITICAL - One or both player UIDs are null! P1=$player1Uid, P2=$player2Uid")
                return
            }
            
            val playerNumber = if (currentUid == player1Uid) 1 else 2
            
            val localGameState = com.naijaayo.worldwide.LocalGameState(
                pits = game.gameState.board.toIntArray(),
                player1Score = game.gameState.player1Score, // Use existing scores from Firebase
                player2Score = game.gameState.player2Score,
                currentPlayer = playerNumber,
                gameOver = game.gameState.gameOver,
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
                player1Uid
            } else {
                player2Uid
            }
            
            // Convert back to Firebase GameState
            val newFirebaseState = PlayNowGameState(
                board = newLocalState!!.pits.toList(),
                nextPlayerUid = nextPlayerUid,
                winnerUid = if (newLocalState!!.gameOver) {
                    when (newLocalState!!.winner) {
                        1 -> player1Uid
                        2 -> player2Uid
                        else -> null // Draw
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
            val game = parsePlayNowGame(snapshot)
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

    suspend fun resumeGame(roomCode: String): MatchResult {
        return try {
            // 1. Check if active game exists
            val activeGameSnapshot = gamesRef.child(roomCode).get().await()
            if (activeGameSnapshot.exists()) {
                val game = parsePlayNowGame(activeGameSnapshot)
                if (game != null) {
                    if (game.status == "waiting_resume") {
                        // Game is waiting for second player -> Set to playing
                        gamesRef.child(roomCode).child("status").setValue("playing").await()
                        return MatchResult.Matched(game.roomId ?: roomCode, roomCode)
                    } else if (game.status == "playing") {
                        // Game already playing -> Join
                        return MatchResult.Matched(game.roomId ?: roomCode, roomCode)
                    }
                }
            }

            // 2. If not active, fetch from saved_games
            val savedGameSnapshot = savedGamesRef.child(roomCode).get().await()
            val savedGame = parsePlayNowGame(savedGameSnapshot)

            if (savedGame != null) {
                // 3. Rehydrate: Write back to 'games/{roomCode}'
                // Set status to 'waiting_resume' so first player waits
                val rehydratedGame = savedGame.copy(status = "waiting_resume")
                gamesRef.child(roomCode).setValue(rehydratedGame).await()
                return MatchResult.Waiting(rehydratedGame.roomId ?: roomCode, roomCode)
            } else {
                return MatchResult.Error("Saved game not found")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error resuming game", e)
            return MatchResult.Error(e.message ?: "Unknown error")
        }
    }

    fun listenForResume(roomCode: String, onResume: () -> Unit): ValueEventListener {
        return gamesRef.child(roomCode).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "playing") {
                    onResume()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseManager", "Resume listener cancelled", error.toException())
            }
        })
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
        return try {
            val snapshot = firestore.collection("users")
                .get()
                .await()
            
            val entries = mutableListOf<LeaderboardEntry>()
            for (doc in snapshot.documents) {
                val stats = doc.get("stats") as? Map<String, Any> ?: continue
                val spStats = stats["singlePlayer"] as? Map<String, Any> ?: continue
                
                val wins = (spStats["wins"] as? Long) ?: 0L
                val losses = (spStats["losses"] as? Long) ?: 0L
                val draws = (spStats["draws"] as? Long) ?: 0L
                val totalPoints = (spStats["totalPoints"] as? Long) ?: 0L
                
                // Skip users with no games played
                if (wins + losses + draws == 0L) continue
                
                entries.add(LeaderboardEntry(
                    id = doc.id,
                    displayName = doc.getString("displayName") ?: doc.getString("username") ?: "Unknown",
                    username = doc.getString("username") ?: "",
                    avatarId = doc.getString("avatarId") ?: "ayo",
                    totalPoints = totalPoints,
                    wins = wins,
                    losses = losses,
                    draws = draws
                ))
            }
            
            // Sort by totalPoints descending
            entries.sortedByDescending { it.totalPoints }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error fetching SP leaderboard", e)
            emptyList()
        }
    }

    suspend fun getMultiplayerLeaderboard(): List<LeaderboardEntry> {
        return try {
            val snapshot = firestore.collection("users")
                .get()
                .await()
            
            val entries = mutableListOf<LeaderboardEntry>()
            for (doc in snapshot.documents) {
                val stats = doc.get("stats") as? Map<String, Any> ?: continue
                val mpStats = stats["multiplayer"] as? Map<String, Any> ?: continue
                
                val wins = (mpStats["wins"] as? Long) ?: 0L
                val losses = (mpStats["losses"] as? Long) ?: 0L
                val draws = (mpStats["draws"] as? Long) ?: 0L
                val totalPoints = (mpStats["totalPoints"] as? Long) ?: 0L
                
                // Skip users with no games played
                if (wins + losses + draws == 0L) continue
                
                entries.add(LeaderboardEntry(
                    id = doc.id,
                    displayName = doc.getString("displayName") ?: doc.getString("username") ?: "Unknown",
                    username = doc.getString("username") ?: "",
                    avatarId = doc.getString("avatarId") ?: "ayo",
                    totalPoints = totalPoints,
                    wins = wins,
                    losses = losses,
                    draws = draws
                ))
            }
            
            // Sort by totalPoints descending
            entries.sortedByDescending { it.totalPoints }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error fetching MP leaderboard", e)
            emptyList()
        }
    }

    suspend fun updateSinglePlayerScore(scoreToAdd: Long, displayName: String, avatarId: String) {
        // This is now handled by recordSinglePlayerResult
    }

    suspend fun updateMultiplayerScore(scoreToAdd: Long, displayName: String, avatarId: String) {
        // This is now handled by recordMultiplayerResult
    }
    // --- NetworkGame Statistics Tracking ---
    enum class GameResult {
        WIN, LOSS, DRAW
    }

    suspend fun recordSinglePlayerResult(
        userId: String,
        result: GameResult,
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
                GameResult.WIN -> {
                    singlePlayerStats["wins"] = wins + 1
                    singlePlayerStats["totalPoints"] = totalPoints + 3
                }
                GameResult.LOSS -> {
                    singlePlayerStats["losses"] = losses + 1
                }
                GameResult.DRAW -> {
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

    suspend fun recordMultiplayerResultForSelf(gameId: String, winnerId: String?) {
        val currentUid = auth.currentUser?.uid ?: return
        
        // Determine result for SELF
        val result = when {
            winnerId == null -> GameResult.DRAW
            winnerId == currentUid -> GameResult.WIN
            else -> GameResult.LOSS
        }
        
        // Update ONLY my stats
        updatePlayerMultiplayerStats(currentUid, gameId, result)
    }
    
    private suspend fun updatePlayerMultiplayerStats(userId: String, gameId: String, result: GameResult) {
        val userDocRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            val stats = snapshot.get("stats") as? Map<String, Any> ?: emptyMap()
            val multiplayerStats = stats["multiplayer"] as? MutableMap<String, Any> ?: mutableMapOf()
            
            // IDEMPOTENCY CHECK:
            // Check if this game was already recorded to prevent double counting
            val lastRecordedGameId = multiplayerStats["lastGameId"] as? String
            if (lastRecordedGameId == gameId) {
                // Already recorded, skip update
                return@runTransaction null
            }
            
            val wins = (multiplayerStats["wins"] as? Long) ?: 0L
            val losses = (multiplayerStats["losses"] as? Long) ?: 0L
            val draws = (multiplayerStats["draws"] as? Long) ?: 0L
            val totalPoints = (multiplayerStats["totalPoints"] as? Long) ?: 0L
            
            when (result) {
                GameResult.WIN -> {
                    multiplayerStats["wins"] = wins + 1
                    multiplayerStats["totalPoints"] = totalPoints + 3
                }
                GameResult.LOSS -> {
                    multiplayerStats["losses"] = losses + 1
                }
                GameResult.DRAW -> {
                    multiplayerStats["draws"] = draws + 1
                    multiplayerStats["totalPoints"] = totalPoints + 1
                }
            }
            
            // Save the game ID to prevent future duplicates
            multiplayerStats["lastGameId"] = gameId
            
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

                val entry = mapOf(
                    "displayName" to displayName,
                    "score" to newScore,
                    "avatarId" to avatarId
                )
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
                "coinBalance" to 10L, // Initial coin bonus
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

    // --- Friend System ---

    suspend fun searchUsers(query: String): List<User> {
        return try {
            val usersRef = firestore.collection("users")
            
            // Search by username
            val usernameSnapshot = usersRef
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .limit(10)
                .get()
                .await()
            val usernameResults = usernameSnapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }

            // Search by email
            val emailSnapshot = usersRef
                .whereGreaterThanOrEqualTo("email", query)
                .whereLessThanOrEqualTo("email", query + "\uf8ff")
                .limit(10)
                .get()
                .await()
            val emailResults = emailSnapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }

            // Combine and deduplicate
            val combinedResults = (usernameResults + emailResults)
                .distinctBy { it.id }
                .filter { it.id != auth.currentUser?.uid }

            android.util.Log.d("FirebaseManager", "Search complete. Username hits: ${usernameResults.size}, Email hits: ${emailResults.size}, Final unique: ${combinedResults.size}")
            combinedResults.forEach { 
                android.util.Log.d("FirebaseManager", "Found user: ${it.username}, ID: '${it.id}'") 
            }
            combinedResults
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error searching users", e)
            emptyList()
        }
    }

    suspend fun sendFriendRequest(toUser: User): Boolean {
        val currentUser = auth.currentUser ?: return false
        val fromUid = currentUser.uid

        // Check if a friend request from this user already exists
        try {
            val existingRequests = usersRef.document(toUser.id)
                .collection("friend_requests")
                .whereEqualTo("fromUid", fromUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            
            if (!existingRequests.isEmpty) {
                android.util.Log.d("FirebaseManager", "Friend request already sent to ${toUser.username}")
                return false // Request already exists
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error checking existing requests", e)
        }
        
        // Get current user profile for the request
        val myProfile = getUserProfile(fromUid) ?: return false
        
        android.util.Log.d("FirebaseManager", "Sending friend request to: ${toUser.username} (ID: '${toUser.id}')")

        if (toUser.id.isEmpty()) {
            android.util.Log.e("FirebaseManager", "Cannot send friend request: Target user ID is empty!")
            return false
        }

        val request = FriendRequest(
            id = usersRef.document().id, // Generate ID
            fromUid = fromUid,
            fromUsername = myProfile["username"] as? String ?: "Unknown",
            fromEmail = myProfile["email"] as? String ?: "",
            fromAvatarId = myProfile["avatarId"] as? String ?: "ayo",
            toUid = toUser.id,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )

        return try {
            // Add to recipient's friend_requests subcollection
            val targetPath = "users/${toUser.id}/friend_requests/${request.id}"
            android.util.Log.d("FirebaseManager", "Writing request to path: $targetPath")
            
            usersRef.document(toUser.id).collection("friend_requests")
                .document(request.id)
                .set(request)
                .await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error sending friend request", e)
            false
        }
    }

    fun listenForFriendRequests(onRequestsUpdated: (List<FriendRequest>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null
        
        return usersRef.document(uid).collection("friend_requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("FirebaseManager", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requests = snapshot.toObjects(FriendRequest::class.java)
                    onRequestsUpdated(requests)
                }
            }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Boolean {
        val currentUser = auth.currentUser ?: return false
        val myUid = currentUser.uid

        return try {
            val batch = firestore.batch()

            // 1. Add to my friends
            val myFriendRef = usersRef.document(myUid).collection("friends").document(request.fromUid)
            val myFriendData = Friend(
                id = request.fromUid,
                name = request.fromUsername,
                avatar = request.fromAvatarId
            )
            batch.set(myFriendRef, myFriendData)

            // 2. Add me to their friends
            // Need my details
            val myProfile = getUserProfile(myUid) ?: return false
            val theirFriendRef = usersRef.document(request.fromUid).collection("friends").document(myUid)
            val theirFriendData = Friend(
                id = myUid,
                name = myProfile["username"] as? String ?: "Unknown",
                avatar = myProfile["avatarId"] as? String ?: "ayo"
            )
            batch.set(theirFriendRef, theirFriendData)

            // 3. Delete the request
            val requestRef = usersRef.document(myUid).collection("friend_requests").document(request.id)
            batch.delete(requestRef)

            batch.commit().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error accepting friend request", e)
            false
        }
    }

    suspend fun declineFriendRequest(request: FriendRequest): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            usersRef.document(uid).collection("friend_requests").document(request.id).delete().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error declining friend request", e)
            false
        }
    }

    fun listenForFriends(onFriendsUpdated: (List<Friend>) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null

        return usersRef.document(uid).collection("friends")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("FirebaseManager", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val friends = snapshot.toObjects(Friend::class.java)
                    onFriendsUpdated(friends)
                }
            }
    }

    // --- Chat System ---

    /**
     * Generates a consistent chat ID for two users by sorting their UIDs.
     * This ensures the same chat room is used regardless of who initiates.
     */
    fun getChatId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    /**
     * Sends a message to a chat with a friend.
     */
    suspend fun sendMessage(friendId: String, text: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (text.isBlank()) return false

        return try {
            val chatId = getChatId(currentUid, friendId)
            val messageRef = chatsRef.child(chatId).child("messages").push()
            val messageId = messageRef.key ?: return false

            val message = mapOf(
                "id" to messageId,
                "text" to text.trim(),
                "senderId" to currentUid,
                "timestamp" to System.currentTimeMillis()
            )

            messageRef.setValue(message).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error sending message", e)
            false
        }
    }

    /**
     * Listens for messages in a chat with a friend in real-time.
     */
    fun listenForMessages(
        friendId: String,
        onMessagesUpdated: (List<com.naijaayo.worldwide.model.Message>) -> Unit
    ): ValueEventListener {
        val currentUid = auth.currentUser?.uid ?: ""
        val chatId = getChatId(currentUid, friendId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<com.naijaayo.worldwide.model.Message>()
                for (child in snapshot.children) {
                    val id = child.child("id").getValue(String::class.java) ?: ""
                    val text = child.child("text").getValue(String::class.java) ?: ""
                    val senderId = child.child("senderId").getValue(String::class.java) ?: ""
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    
                    // Game invite fields
                    val isGameInvite = child.child("isGameInvite").getValue(Boolean::class.java) ?: false
                    val roomId = child.child("roomId").getValue(String::class.java) ?: ""
                    val roomCode = child.child("roomCode").getValue(String::class.java) ?: ""
                    val gameLevel = child.child("gameLevel").getValue(String::class.java) ?: ""
                    val inviterUsername = child.child("inviterUsername").getValue(String::class.java) ?: ""

                    messages.add(
                        com.naijaayo.worldwide.model.Message(
                            id = id,
                            text = text,
                            senderId = senderId,
                            timestamp = timestamp,
                            isGameInvite = isGameInvite,
                            roomId = roomId,
                            roomCode = roomCode,
                            gameLevel = gameLevel,
                            inviterUsername = inviterUsername
                        )
                    )
                }
                // Sort by timestamp ascending
                messages.sortBy { it.timestamp }
                onMessagesUpdated(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseManager", "Messages listen cancelled", error.toException())
            }
        }

        chatsRef.child(chatId).child("messages").addValueEventListener(listener)
        return listener
    }

    /**
     * Removes a messages listener.
     */
    fun removeMessagesListener(listener: ValueEventListener, friendId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        val chatId = getChatId(currentUid, friendId)
        chatsRef.child(chatId).child("messages").removeEventListener(listener)
    }

    /**
     * Sends a game invite message to a friend's chat.
     */
    suspend fun sendGameInvite(
        friendId: String,
        roomId: String,
        roomCode: String,
        gameLevel: String,
        inviterUsername: String
    ): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false

        return try {
            val chatId = getChatId(currentUid, friendId)
            val messageRef = chatsRef.child(chatId).child("messages").push()
            val messageId = messageRef.key ?: return false

            val levelText = when (gameLevel) {
                "EASY" -> "easy"
                "HARD" -> "hard"
                else -> "medium"
            }

            val message = mapOf(
                "id" to messageId,
                "text" to "$inviterUsername invites you for a $levelText game:",
                "senderId" to currentUid,
                "timestamp" to System.currentTimeMillis(),
                "isGameInvite" to true,
                "roomId" to roomId,
                "roomCode" to roomCode,
                "gameLevel" to gameLevel,
                "inviterUsername" to inviterUsername
            )

            messageRef.setValue(message).await()
            android.util.Log.d("FirebaseManager", "Game invite sent to $friendId for room $roomCode")
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error sending game invite", e)
            false
        }
    }

    // --- User Online Status ---
    
    /**
     * Set current user as online in Firebase Realtime Database
     */
    fun setUserOnline() {
        val uid = auth.currentUser?.uid ?: return
        val userStatusRef = onlineUsersRef.child(uid)
        
        // Set user as online with timestamp
        userStatusRef.setValue(mapOf(
            "online" to true,
            "lastSeen" to ServerValue.TIMESTAMP
        ))
        
        // Setup onDisconnect to automatically set offline when user disconnects
        onDisconnectHandler = userStatusRef.onDisconnect()
        onDisconnectHandler?.setValue(mapOf(
            "online" to false,
            "lastSeen" to ServerValue.TIMESTAMP
        ))
    }
    
    /**
     * Set current user as offline in Firebase Realtime Database
     */
    fun setUserOffline() {
        val uid = auth.currentUser?.uid ?: return
        val userStatusRef = onlineUsersRef.child(uid)
        
        userStatusRef.setValue(mapOf(
            "online" to false,
            "lastSeen" to ServerValue.TIMESTAMP
        ))
        
        // Cancel the onDisconnect handler
        onDisconnectHandler?.cancel()
        onDisconnectHandler = null
    }
    
    /**
     * Listen for a specific user's online status
     * @return ValueEventListener that should be removed when no longer needed
     */
    fun listenToUserOnlineStatus(uid: String, onStatusChanged: (isOnline: Boolean) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("online").getValue(Boolean::class.java) ?: false
                onStatusChanged(isOnline)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseManager", "Error listening to online status", error.toException())
                onStatusChanged(false)
            }
        }
        
        onlineUsersRef.child(uid).addValueEventListener(listener)
        return listener
    }
    
    /**
     * Remove an online status listener
     */
    fun removeOnlineStatusListener(uid: String, listener: ValueEventListener) {
        onlineUsersRef.child(uid).removeEventListener(listener)
    }
    // --- Coin System ---
    
    suspend fun addCoins(userId: String, amount: Int): Boolean {
        val userRef = firestore.collection("users").document(userId)
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = (snapshot.getLong("coinBalance") ?: 0L).toInt()
                transaction.update(userRef, "coinBalance", currentBalance + amount)
            }.await()
            true
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error adding coins", e)
            false
        }
    }
    
    suspend fun deductCoins(userId: String, amount: Int): Boolean {
        val userRef = firestore.collection("users").document(userId)
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentBalance = (snapshot.getLong("coinBalance") ?: 0L).toInt()
                
                if (currentBalance >= amount) {
                    transaction.update(userRef, "coinBalance", currentBalance - amount)
                    true // Success
                } else {
                    false // Insufficient funds
                }
            }.await() ?: false // Explicitly handle potential null from transaction
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error deducting coins", e)
            false
        }
    }
    
    suspend fun verifyCoupon(code: String): Int? {
        val couponRef = firestore.collection("coupons").document(code)
        val user = auth.currentUser ?: return null
        
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(couponRef)
                
                if (!snapshot.exists()) return@runTransaction null // Invalid code
                
                val usedBy = snapshot.get("usedBy") as? List<String> ?: emptyList()
                if (usedBy.contains(user.uid)) return@runTransaction null // Already used
                
                val amount = (snapshot.getLong("amount") ?: 0L).toInt()
                
                // Add user to usedBy list
                val newUsedBy = usedBy + user.uid
                transaction.update(couponRef, "usedBy", newUsedBy)
                
                // Add coins to user
                val userRef = firestore.collection("users").document(user.uid)
                val userSnapshot = transaction.get(userRef)
                val currentBalance = (userSnapshot.getLong("coinBalance") ?: 0L).toInt()
                transaction.update(userRef, "coinBalance", currentBalance + amount)
                
                amount // Return amount added
            }.await()
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error verifying coupon", e)
            null
        }
    }
    
    suspend fun checkDailyLoginBonus(userId: String): Boolean {
        val userRef = firestore.collection("users").document(userId)
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val lastLoginDate = snapshot.getString("lastLoginDate")
                
                if (lastLoginDate != today) {
                    val currentBalance = (snapshot.getLong("coinBalance") ?: 0L).toInt()
                    transaction.update(userRef, 
                        mapOf(
                            "coinBalance" to currentBalance + 1,
                            "lastLoginDate" to today
                        )
                    )
                    true // Bonus added
                } else {
                    false // Already claimed today
                }
            }.await() ?: false
        } catch (e: Exception) {
            android.util.Log.e("FirebaseManager", "Error checking daily bonus", e)
            false
        }
    }
}

// Helper for manual parsing to avoid ClassMapper private field issues
fun parsePlayNowGame(snapshot: com.google.firebase.database.DataSnapshot): PlayNowGame? {
    if (!snapshot.exists()) return null
    return try {
        val roomId = snapshot.child("roomId").getValue(String::class.java)
        val roomCode = snapshot.child("roomCode").getValue(String::class.java)
        val creatorUid = snapshot.child("creatorUid").getValue(String::class.java)
        val status = snapshot.child("status").getValue(String::class.java) ?: "waiting"
        val level = snapshot.child("level").getValue(String::class.java) ?: "MEDIUM"
        val gameType = snapshot.child("gameType").getValue(String::class.java) ?: "LOBBY"
        val navigationAction = snapshot.child("navigationAction").getValue(String::class.java)
        
        // Handle 'private' field name mismatch (Firebase 'private' vs Kotlin 'isPrivate')
        val isPrivate = snapshot.child("private").getValue(Boolean::class.java) 
             ?: snapshot.child("isPrivate").getValue(Boolean::class.java) 
             ?: false
        
        // Players map
        val playersMap = mutableMapOf<String, PlayNowPlayer>()
        snapshot.child("players").children.forEach { pSnapshot ->
            val player = pSnapshot.getValue(PlayNowPlayer::class.java)
            if (player != null && player.uid != null) {
                playersMap[player.uid!!] = player
            }
        }
        
        // GameState
        val gameStateSnapshot = snapshot.child("gameState")
        val gameState = if (gameStateSnapshot.exists()) {
             gameStateSnapshot.getValue(PlayNowGameState::class.java) ?: PlayNowGameState()
        } else {
             PlayNowGameState()
        }

        PlayNowGame(
            roomId = roomId,
            roomCode = roomCode,
            creatorUid = creatorUid,
            players = playersMap,
            status = status,
            isPrivate = isPrivate,
            level = level,
            gameType = gameType,
            gameState = gameState,
            navigationAction = navigationAction
        )
    } catch (e: Exception) {
        android.util.Log.e("FirebaseManager", "Manual parse failed", e)
        null
    }
}
