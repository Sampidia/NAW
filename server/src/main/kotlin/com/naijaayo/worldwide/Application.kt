package com.naijaayo.worldwide

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import com.auth0.jwt.JWT

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Initialize MongoDB
    val mongoService = MongoService()

    // Configure CORS
    install(io.ktor.server.plugins.cors.routing.CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHost("ayo.sampidia.com", schemes = listOf("https"))
        allowHost("localhost:8080", schemes = listOf("http"))
        allowCredentials = true
    }

    // Configure authentication
    val authService = MongoAuthService(mongoService)
    install(Authentication) {
        jwt("auth-jwt") {
            val jwtVerifier = JWT.require(authService.algorithm)
                .withAudience("naija-ayo-users")
                .withIssuer("naija-ayo-worldwide")
                .build()
            verifier(jwtVerifier)
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
        }
    }

    // Configure content negotiation
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        json(Json)
    }

    // Configure WebSockets
    install(WebSockets)

    configureRouting(authService, mongoService)
}

fun Application.configureRouting(authService: MongoAuthService, mongoService: MongoService) {
    val rooms = ConcurrentHashMap<String, Room>()
    val gameStates = ConcurrentHashMap<String, GameState>()
    val userSessions = ConcurrentHashMap<String, DefaultWebSocketSession>()

    routing {
        // Authentication endpoints
        post("/auth/register") {
            try {
                val request = call.receive<RegisterRequest>()
                val result = authService.registerUser(request.username, request.email, request.password)

                result.onSuccess { user ->
                    val token = authService.generateToken(user)
                    call.respond(AuthResponse(token, user))
                }.onFailure { error ->
                    call.respondText(error.message ?: "Registration failed", status = HttpStatusCode.BadRequest)
                }
            } catch (e: Exception) {
                call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
            }
        }

        post("/auth/login") {
            try {
                val request = call.receive<LoginRequest>()
                val result = authService.loginUser(request.emailOrUsername, request.password)

                result.onSuccess { user ->
                    val token = authService.generateToken(user)
                    call.respond(AuthResponse(token, user))
                }.onFailure { error ->
                    call.respondText(error.message ?: "Login failed", status = HttpStatusCode.Unauthorized)
                }
            } catch (e: Exception) {
                call.respondText("Invalid request", status = HttpStatusCode.BadRequest)
            }
        }

        // Friend management endpoints
        authenticate("auth-jwt") {
            // Get friends list
            get("/users/{userId}/friends") {
                val userId = call.parameters["userId"] ?: return@get call.respondText("Missing userId", status = HttpStatusCode.BadRequest)

                try {
                    val mongoFriends = mongoService.getFriends(userId)
                    val friends = mongoFriends.map {
                        Friend(
                            id = it.id,
                            userId = it.userId,
                            friendId = it.friendId,
                            friendUsername = it.friendUsername,
                            friendEmail = it.friendEmail,
                            friendAvatarId = it.friendAvatarId,
                            status = it.status,
                            createdAt = it.createdAt,
                            lastSeen = it.lastSeen,
                            isOnline = it.isOnline
                        )
                    }
                    call.respond(friends)
                } catch (e: Exception) {
                    call.respondText("Failed to get friends", status = HttpStatusCode.InternalServerError)
                }
            }

            // Send friend request
            post("/users/{userId}/friend-requests") {
                val userId = call.parameters["userId"] ?: return@post call.respondText("Missing userId", status = HttpStatusCode.BadRequest)
                val currentUser = authService.getCurrentUser(call) ?: return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)

                try {
                    val request = call.receive<FriendRequest>()
                    val requestId = authService.generateId()

                    val mongoRequest = MongoFriendRequest(
                        fromUserId = currentUser.id,
                        toUserId = request.toUserId,
                        fromUsername = currentUser.username,
                        fromEmail = currentUser.email,
                        fromAvatarId = currentUser.avatarId,
                        status = FriendRequestStatus.PENDING
                    )
                    mongoService.createFriendRequest(mongoRequest)

                    call.respondText("Friend request sent", status = HttpStatusCode.Created)
                } catch (e: Exception) {
                    call.respondText("Failed to send friend request", status = HttpStatusCode.InternalServerError)
                }
            }

            // Accept/decline friend request
            put("/users/{userId}/friend-requests/{requestId}") {
                val userId = call.parameters["userId"] ?: return@put call.respondText("Missing userId", status = HttpStatusCode.BadRequest)
                val requestId = call.parameters["requestId"] ?: return@put call.respondText("Missing requestId", status = HttpStatusCode.BadRequest)
                val action = call.receive<Map<String, String>>()["action"]

                try {
                    val mongoRequest = mongoService.getFriendRequestById(requestId)

                    if (mongoRequest == null) {
                        return@put call.respondText("Request not found", status = HttpStatusCode.NotFound)
                    }

                    when (action) {
                        "accept" -> {
                            // Get user details for the accepting user
                            val acceptingUser = mongoService.getUserById(userId) ?: return@put call.respondText("User not found", status = HttpStatusCode.NotFound)

                            // Create friendship for both users
                            val friendship1 = MongoFriend(
                                userId = mongoRequest.fromUserId,
                                friendId = mongoRequest.toUserId,
                                friendUsername = mongoRequest.fromUsername,
                                friendEmail = mongoRequest.fromEmail,
                                friendAvatarId = mongoRequest.fromAvatarId,
                                status = FriendStatus.ACCEPTED,
                                createdAt = System.currentTimeMillis()
                            )

                            val friendship2 = MongoFriend(
                                userId = mongoRequest.toUserId,
                                friendId = mongoRequest.fromUserId,
                                friendUsername = acceptingUser.username,
                                friendEmail = acceptingUser.email,
                                friendAvatarId = acceptingUser.avatarId,
                                status = FriendStatus.ACCEPTED,
                                createdAt = System.currentTimeMillis()
                            )

                            mongoService.addFriend(friendship1)
                            mongoService.addFriend(friendship2)

                            // Remove request
                            mongoService.deleteFriendRequest(requestId)
                        }
                        "decline" -> {
                            mongoService.deleteFriendRequest(requestId)
                        }
                    }

                    call.respondText("Friend request processed", status = HttpStatusCode.OK)
                } catch (e: Exception) {
                    call.respondText("Failed to process friend request", status = HttpStatusCode.InternalServerError)
                }
            }

            // Search users
            get("/users/search") {
                val query = call.request.queryParameters["q"] ?: return@get call.respond(emptyList<User>())
                val currentUser = authService.getCurrentUser(call) ?: return@get call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)

                try {
                    val mongoUsers = mongoService.searchUsers(query, currentUser.id)
                    val results = mongoUsers.map {
                        User(
                            id = it.id,
                            username = it.username,
                            email = it.email,
                            avatarId = it.avatarId
                        )
                    }
                    call.respond(results)
                } catch (e: Exception) {
                    call.respondText("Search failed", status = HttpStatusCode.InternalServerError)
                }
            }

            // Send message
            post("/users/{userId}/messages") {
                val userId = call.parameters["userId"] ?: return@post call.respondText("Missing userId", status = HttpStatusCode.BadRequest)
                val currentUser = authService.getCurrentUser(call) ?: return@post call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)

                try {
                    val message = call.receive<Message>()

                    val mongoMessage = MongoMessage(
                        fromUserId = currentUser.id,
                        toUserId = message.toUserId,
                        fromUsername = currentUser.username,
                        content = message.content,
                        type = message.type,
                        gameInvitationRoomId = message.gameInvitation?.roomId,
                        gameInvitationHostUsername = message.gameInvitation?.hostUsername,
                        gameInvitationGameType = message.gameInvitation?.gameType,
                        gameInvitationDifficulty = message.gameInvitation?.difficulty
                    )
                    mongoService.saveMessage(mongoMessage)

                    call.respondText("Message sent", status = HttpStatusCode.Created)
                } catch (e: Exception) {
                    call.respondText("Failed to send message", status = HttpStatusCode.InternalServerError)
                }
            }

            // Get messages with friend
            get("/users/{userId}/messages/{friendId}") {
                val userId = call.parameters["userId"] ?: return@get call.respondText("Missing userId", status = HttpStatusCode.BadRequest)
                val friendId = call.parameters["friendId"] ?: return@get call.respondText("Missing friendId", status = HttpStatusCode.BadRequest)

                try {
                    val mongoMessages = mongoService.getMessagesBetweenUsers(userId, friendId)
                    val messages = mongoMessages.map {
                        Message(
                            id = it.id,
                            fromUserId = it.fromUserId,
                            toUserId = it.toUserId,
                            fromUsername = it.fromUsername,
                            content = it.content,
                            timestamp = it.timestamp,
                            type = it.type,
                            gameInvitation = if (it.gameInvitationRoomId != null) {
                                GameInvitation(
                                    roomId = it.gameInvitationRoomId!!,
                                    hostUsername = it.gameInvitationHostUsername!!,
                                    gameType = it.gameInvitationGameType!!,
                                    difficulty = it.gameInvitationDifficulty!!
                                )
                            } else null
                        )
                    }
                    call.respond(messages)
                } catch (e: Exception) {
                    call.respondText("Failed to get messages", status = HttpStatusCode.InternalServerError)
                }
            }

            // Get friend requests
            get("/users/{userId}/friend-requests") {
                val userId = call.parameters["userId"] ?: return@get call.respondText("Missing userId", status = HttpStatusCode.BadRequest)

                try {
                    val mongoRequests = mongoService.getFriendRequestsForUser(userId)
                    val requests = mongoRequests.map {
                        FriendRequest(
                            id = it.id,
                            fromUserId = it.fromUserId,
                            toUserId = it.toUserId,
                            fromUsername = it.fromUsername,
                            fromEmail = it.fromEmail,
                            fromAvatarId = it.fromAvatarId,
                            status = it.status,
                            createdAt = it.createdAt
                        )
                    }
                    call.respond(requests)
                } catch (e: Exception) {
                    call.respondText("Failed to get friend requests", status = HttpStatusCode.InternalServerError)
                }
            }
        }

        // WebSocket for real-time messaging
        webSocket("/ws/messages/{userId}") {
            val userId = call.parameters["userId"] ?: return@webSocket

            try {
                userSessions[userId] = this

                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // Handle real-time message updates and broadcasting
                        // For now, just acknowledge receipt
                    }
                }
            } finally {
                userSessions.remove(userId)
            }
        }

        // Existing multiplayer game endpoints would go here...
    }
}

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val emailOrUsername: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: AuthUser
)
