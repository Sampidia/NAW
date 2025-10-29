package com.naijaayo.worldwide

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ConsumeAsFlow
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    // Initialize database
    initDatabase()

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
    val authService = AuthService()
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(authService.algorithm)
            validate { credential ->
                JWTPrincipal(credential.payload)
            }
        }
    }

    // Configure content negotiation
    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        kotlinx.serialization.json()
    }

    // Configure WebSockets
    install(WebSockets)

    configureRouting(authService)
}

fun Application.configureRouting(authService: AuthService) {
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
                    val friends = transaction {
                        Friends.select { Friends.userId eq userId }
                            .map {
                                Friend(
                                    id = it[Friends.id],
                                    userId = it[Friends.userId],
                                    friendId = it[Friends.friendId],
                                    friendUsername = it[Friends.friendUsername],
                                    friendEmail = it[Friends.friendEmail],
                                    friendAvatarId = it[Friends.friendAvatarId],
                                    status = it[Friends.status],
                                    createdAt = it[Friends.createdAt].toString(),
                                    lastSeen = it[Friends.lastSeen].toString(),
                                    isOnline = it[Friends.isOnline]
                                )
                            }
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

                    transaction {
                        FriendRequests.insert {
                            it[id] = requestId
                            it[fromUserId] = currentUser.id
                            it[toUserId] = request.toUserId
                            it[fromUsername] = currentUser.username
                            it[fromEmail] = currentUser.email
                            it[fromAvatarId] = currentUser.avatarId
                            it[status] = FriendRequestStatus.PENDING
                        }
                    }

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
                    val friendRequest = transaction {
                        FriendRequests.select { FriendRequests.id eq requestId }
                            .singleOrNull()
                    }

                    if (friendRequest == null) {
                        return@put call.respondText("Request not found", status = HttpStatusCode.NotFound)
                    }

                    when (action) {
                        "accept" -> {
                            // Create friendship for both users
                            transaction {
                                val friendship1Id = authService.generateId()
                                val friendship2Id = authService.generateId()

                                Friends.insert {
                                    it[id] = friendship1Id
                                    it[Friends.userId] = friendRequest[FriendRequests.fromUserId]
                                    it[Friends.friendId] = friendRequest[FriendRequests.toUserId]
                                    it[friendUsername] = friendRequest[FriendRequests.fromUsername]
                                    it[friendEmail] = friendRequest[FriendRequests.fromEmail]
                                    it[friendAvatarId] = friendRequest[FriendRequests.fromAvatarId]
                                    it[status] = FriendStatus.ACCEPTED
                                }

                                Friends.insert {
                                    it[id] = friendship2Id
                                    it[Friends.userId] = friendRequest[FriendRequests.toUserId]
                                    it[Friends.friendId] = friendRequest[FriendRequests.fromUserId]
                                    it[friendUsername] = userId // This should be fetched from Users table
                                    it[friendEmail] = "" // This should be fetched from Users table
                                    it[friendAvatarId] = "ayo" // This should be fetched from Users table
                                    it[status] = FriendStatus.ACCEPTED
                                }

                                // Remove request
                                FriendRequests.deleteWhere { FriendRequests.id eq requestId }
                            }
                        }
                        "decline" -> {
                            transaction {
                                FriendRequests.deleteWhere { FriendRequests.id eq requestId }
                            }
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
                    val results = transaction {
                        Users.select {
                            ((Users.username.lowerCase() like "%${query.lowercase()}%") or
                             (Users.email.lowerCase() like "%${query.lowercase()}%")) and
                            (Users.id neq currentUser.id)
                        }.map {
                            User(
                                id = it[Users.id],
                                username = it[Users.username],
                                email = it[Users.email],
                                avatarId = it[Users.avatarId]
                            )
                        }
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
                    val messageId = authService.generateId()

                    transaction {
                        Messages.insert {
                            it[id] = messageId
                            it[fromUserId] = currentUser.id
                            it[toUserId] = message.toUserId
                            it[fromUsername] = currentUser.username
                            it[content] = message.content
                            it[timestamp] = LocalDateTime.now()
                            it[type] = message.type
                            it[gameInvitationRoomId] = message.gameInvitation?.roomId
                            it[gameInvitationHostUsername] = message.gameInvitation?.hostUsername
                            it[gameInvitationGameType] = message.gameInvitation?.gameType
                            it[gameInvitationDifficulty] = message.gameInvitation?.difficulty
                        }
                    }

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
                    val messages = transaction {
                        Messages.select {
                            ((Messages.fromUserId eq userId) and (Messages.toUserId eq friendId)) or
                            ((Messages.fromUserId eq friendId) and (Messages.toUserId eq userId))
                        }.orderBy(Messages.timestamp)
                        .map {
                            Message(
                                id = it[Messages.id],
                                fromUserId = it[Messages.fromUserId],
                                toUserId = it[Messages.toUserId],
                                fromUsername = it[Messages.fromUsername],
                                content = it[Messages.content],
                                timestamp = it[Messages.timestamp].toString(),
                                type = it[Messages.type],
                                gameInvitation = if (it[Messages.gameInvitationRoomId] != null) {
                                    GameInvitation(
                                        roomId = it[Messages.gameInvitationRoomId]!!,
                                        hostUsername = it[Messages.gameInvitationHostUsername]!!,
                                        gameType = it[Messages.gameInvitationGameType]!!,
                                        difficulty = it[Messages.gameInvitationDifficulty]!!
                                    )
                                } else null
                            )
                        }
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
                    val requests = transaction {
                        FriendRequests.select { FriendRequests.toUserId eq userId }
                            .map {
                                FriendRequest(
                                    id = it[FriendRequests.id],
                                    fromUserId = it[FriendRequests.fromUserId],
                                    toUserId = it[FriendRequests.toUserId],
                                    fromUsername = it[FriendRequests.fromUsername],
                                    fromEmail = it[FriendRequests.fromEmail],
                                    fromAvatarId = it[FriendRequests.fromAvatarId],
                                    status = it[FriendRequests.status],
                                    createdAt = it[FriendRequests.createdAt].toString()
                                )
                            }
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
