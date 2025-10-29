package com.naijaayo.worldwide.network

import com.naijaayo.worldwide.Friend
import com.naijaayo.worldwide.FriendRequest
import com.naijaayo.worldwide.Message
import com.naijaayo.worldwide.User
import retrofit2.Response
import retrofit2.http.*

interface FriendsApiService {

    @GET("users/{userId}/friends")
    suspend fun getFriends(@Path("userId") userId: String): List<Friend>

    @GET("users/{userId}/friend-requests")
    suspend fun getFriendRequests(@Path("userId") userId: String): List<FriendRequest>

    @POST("users/{userId}/friend-requests")
    suspend fun sendFriendRequest(@Path("userId") userId: String, @Body request: FriendRequest): Response<Unit>

    @PUT("users/{userId}/friend-requests/{requestId}")
    suspend fun respondToFriendRequest(
        @Path("userId") userId: String,
        @Path("requestId") requestId: String,
        @Body action: Map<String, Boolean>
    ): Response<Unit>

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<User>

    @POST("users/{userId}/messages")
    suspend fun sendMessage(@Path("userId") userId: String, @Body message: Message): Response<Unit>

    @GET("users/{userId}/messages/{friendId}")
    suspend fun getMessages(@Path("userId") userId: String, @Path("friendId") friendId: String): List<Message>
}