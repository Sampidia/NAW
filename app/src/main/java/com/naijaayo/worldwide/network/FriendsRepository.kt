package com.naijaayo.worldwide.network

import com.naijaayo.worldwide.Friend
import com.naijaayo.worldwide.FriendRequest
import com.naijaayo.worldwide.Message
import com.naijaayo.worldwide.User

class FriendsRepository(private val apiService: FriendsApiService) {

    suspend fun loadFriends(userId: String): Result<List<Friend>> {
        return try {
            val friends = apiService.getFriends(userId)
            Result.success(friends)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadFriendRequests(userId: String): Result<List<FriendRequest>> {
        return try {
            val requests = apiService.getFriendRequests(userId)
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(fromUserId: String, request: FriendRequest): Result<Unit> {
        return try {
            val response = apiService.sendFriendRequest(fromUserId, request)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send friend request: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(userId: String, requestId: String, accept: Boolean): Result<Unit> {
        return try {
            val response = apiService.respondToFriendRequest(userId, requestId, mapOf("accept" to accept))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to respond to friend request: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            val users = apiService.searchUsers(query)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(userId: String, message: Message): Result<Unit> {
        return try {
            val response = apiService.sendMessage(userId, message)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send message: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadMessages(userId: String, friendId: String): Result<List<Message>> {
        return try {
            val messages = apiService.getMessages(userId, friendId)
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}