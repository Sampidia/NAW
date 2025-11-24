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
    val isPrivate: Boolean = false
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
    suspend fun createPrivateRoom(player: Player, roomCode: String): String {
        val roomId = (100000..999999).random().toString() // Generate 6-digit ID
        val game = Game(
            roomId = roomId,
            roomCode = roomCode,
            creatorUid = player.uid,
            players = mapOf(player.uid!! to player),
            status = "waiting",
            isPrivate = true
        )
        gamesRef.child(roomId).setValue(game).await()
        return roomId
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

    suspend fun makeMove(roomId: String, pitIndex: Int) { /* ... */ }
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
        return try {
            var email = input
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                // Input is likely a username, look up the email
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("username", input)
                    .limit(1)
                    .get()
                    .await()
                
                if (!querySnapshot.isEmpty) {
                    email = querySnapshot.documents[0].getString("email") ?: return false
                } else {
                    return false // Username not found
                }
            }
            
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
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
