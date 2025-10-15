package com.naijaayo.worldwide.repository

import com.google.gson.Gson
import com.naijaayo.worldwide.Avatar
import com.naijaayo.worldwide.Room
import com.naijaayo.worldwide.User
import com.naijaayo.worldwide.network.ApiService
import com.naijaayo.worldwide.network.RetrofitClient
import com.naijaayo.worldwide.network.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GameRepository {

    private val api: ApiService = RetrofitClient.instance
    private val socketManager = SocketManager()
    private val gson = Gson()

    // Socket Functions
    fun connectSocket() = socketManager.connect()

    suspend fun awaitSocketConnection() = socketManager.awaitConnection()

    fun disconnectSocket() = socketManager.disconnect()

    fun onSocketEvent(eventName: String, listener: (Any) -> Unit) {
        socketManager.on(eventName, listener)
    }

    fun emitSocketEvent(eventName: String, data: JSONObject) {
        socketManager.emit(eventName, data)
    }

    // API Functions
    suspend fun createRoom(room: Room): Room = withContext(Dispatchers.IO) {
        api.createRoom(room)
    }

    suspend fun getRooms(): List<Room> = withContext(Dispatchers.IO) {
        api.getRooms()
    }

    suspend fun getLeaderboard(): List<User> = withContext(Dispatchers.IO) {
        api.getLeaderboard()
    }

    suspend fun updateAvatar(userId: String, avatar: Avatar) = withContext(Dispatchers.IO) {
        api.updateAvatar(userId, avatar)
    }
}
