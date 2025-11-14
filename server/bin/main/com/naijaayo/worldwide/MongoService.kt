package com.naijaayo.worldwide

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.time.LocalDateTime

class MongoService {
    private val mongoUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"
    private val databaseName = System.getenv("MONGODB_DATABASE") ?: "naija_ayo"

    private val mongoClient: MongoClient = MongoClient.create(mongoUri)
    private val database: MongoDatabase = mongoClient.getDatabase(databaseName)

    // Collections
    private val users = database.getCollection<MongoUser>("users")
    private val friends = database.getCollection<MongoFriend>("friends")
    private val friendRequests = database.getCollection<MongoFriendRequest>("friend_requests")
    private val messages = database.getCollection<MongoMessage>("messages")
    private val savedGames = database.getCollection<MongoSavedGame>("saved_games")
    private val leaderboard = database.getCollection<MongoLeaderboardEntry>("leaderboard")
    private val rooms = database.getCollection<MongoRoom>("rooms")
    private val gameResults = database.getCollection<MongoGameResult>("game_results")

    // User operations
    suspend fun createUser(user: MongoUser): String {
        users.insertOne(user)
        return user.id
    }

    suspend fun getUserById(id: String): MongoUser? {
        return users.find(Filters.eq("_id", id)).firstOrNull()
    }

    suspend fun getUserByUsername(username: String): MongoUser? {
        return users.find(Filters.eq("username", username)).firstOrNull()
    }

    suspend fun getUserByEmail(email: String): MongoUser? {
        return users.find(Filters.eq("email", email)).firstOrNull()
    }

    suspend fun updateUser(id: String, updates: List<Bson>): Boolean {
        val result = users.updateOne(Filters.eq("_id", id), updates)
        return result.modifiedCount > 0
    }

    suspend fun updateUserLastSeen(id: String) {
        users.updateOne(
            Filters.eq("_id", id),
            listOf(
                Updates.set("lastSeen", LocalDateTime.now().toString()),
                Updates.set("isOnline", true)
            )
        )
    }

    // Friend operations
    suspend fun addFriend(friend: MongoFriend): String {
        friends.insertOne(friend)
        return friend.id
    }

    suspend fun getFriends(userId: String): List<MongoFriend> {
        return friends.find(Filters.eq("userId", userId)).toList()
    }

    suspend fun removeFriend(userId: String, friendId: String): Boolean {
        val result = friends.deleteMany(
            Filters.or(
                Filters.and(Filters.eq("userId", userId), Filters.eq("friendId", friendId)),
                Filters.and(Filters.eq("userId", friendId), Filters.eq("friendId", userId))
            )
        )
        return result.deletedCount > 0
    }

    // Friend request operations
    suspend fun createFriendRequest(request: MongoFriendRequest): String {
        friendRequests.insertOne(request)
        return request.id
    }

    suspend fun getFriendRequestsForUser(userId: String): List<MongoFriendRequest> {
        return friendRequests.find(Filters.eq("toUserId", userId)).toList()
    }

    suspend fun getFriendRequestById(id: String): MongoFriendRequest? {
        return friendRequests.find(Filters.eq("_id", id)).firstOrNull()
    }

    suspend fun updateFriendRequestStatus(id: String, status: FriendRequestStatus): Boolean {
        val result = friendRequests.updateOne(
            Filters.eq("_id", id),
            listOf(Updates.set("status", status))
        )
        return result.modifiedCount > 0
    }

    suspend fun deleteFriendRequest(id: String): Boolean {
        val result = friendRequests.deleteOne(Filters.eq("_id", id))
        return result.deletedCount > 0
    }

    // Message operations
    suspend fun saveMessage(message: MongoMessage): String {
        messages.insertOne(message)
        return message.id
    }

    suspend fun getMessagesBetweenUsers(userId1: String, userId2: String): List<MongoMessage> {
        return messages.find(
            Filters.or(
                Filters.and(Filters.eq("fromUserId", userId1), Filters.eq("toUserId", userId2)),
                Filters.and(Filters.eq("fromUserId", userId2), Filters.eq("toUserId", userId1))
            )
        ).toList().sortedBy { it.timestamp }
    }

    // User search
    suspend fun searchUsers(query: String, currentUserId: String): List<MongoUser> {
        val regex = Regex(query, RegexOption.IGNORE_CASE)
        return users.find(
            Filters.and(
                Filters.ne("_id", currentUserId),
                Filters.or(
                    Filters.regex("username", regex.pattern, "i"),
                    Filters.regex("email", regex.pattern, "i")
                )
            )
        ).toList()
    }

    // Saved games
    suspend fun saveGame(game: MongoSavedGame): String {
        savedGames.insertOne(game)
        return game.id
    }

    suspend fun getSavedGames(userId: String): List<MongoSavedGame> {
        return savedGames.find(Filters.eq("userId", userId)).toList()
    }

    // Leaderboard
    suspend fun createLeaderboardEntry(entry: MongoLeaderboardEntry): String {
        leaderboard.insertOne(entry)
        return entry.id
    }

    suspend fun getLeaderboardEntries(userId: String): List<MongoLeaderboardEntry> {
        return leaderboard.find(Filters.eq("userId", userId)).toList()
    }

    suspend fun updateLeaderboardEntry(id: String, updates: List<Bson>): Boolean {
        val result = leaderboard.updateOne(Filters.eq("_id", id), updates)
        return result.modifiedCount > 0
    }

    // Rooms
    suspend fun createRoom(room: MongoRoom): String {
        rooms.insertOne(room)
        return room.id
    }

    suspend fun getRoomByRoomId(roomId: String): MongoRoom? {
        return rooms.find(Filters.eq("roomId", roomId)).firstOrNull()
    }

    suspend fun updateRoom(id: String, updates: List<Bson>): Boolean {
        val result = rooms.updateOne(Filters.eq("_id", id), updates)
        return result.modifiedCount > 0
    }

    // Game results
    suspend fun saveGameResult(result: MongoGameResult): String {
        gameResults.insertOne(result)
        return result.id
    }

    suspend fun getGameResultsForUser(userId: String): List<MongoGameResult> {
        return gameResults.find(
            Filters.or(
                Filters.eq("player1Id", userId),
                Filters.eq("player2Id", userId)
            )
        ).toList()
    }

    fun close() {
        mongoClient.close()
    }
}
