package com.naijaayo.worldwide.network

import com.naijaayo.worldwide.Avatar
import com.naijaayo.worldwide.Room
import com.naijaayo.worldwide.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("/api/rooms")
    suspend fun getRooms(): List<Room>

    @POST("/api/rooms")
    suspend fun createRoom(@Body room: Room): Room

    @GET("/api/leaderboard")
    suspend fun getLeaderboard(): List<User>

    @POST("/api/users/{uid}/avatar")
    suspend fun updateAvatar(@Path("uid") userId: String, @Body avatar: Avatar)

}
