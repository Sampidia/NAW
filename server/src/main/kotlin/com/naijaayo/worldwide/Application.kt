package com.naijaayo.worldwide

import com.google.gson.Gson
import io.ktor.http.* 
import io.ktor.serialization.gson.gson
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.json.JSONObject
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

// In-memory store for our game rooms and connections
val rooms = ConcurrentHashMap<String, Room>()
val users = ConcurrentHashMap<String, User>()
val roomConnections = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

fun Application.module() {
    install(ContentNegotiation) {
        gson()
    }
    install(WebSockets)

    configureRouting()
}

fun Application.configureRouting() {
    val gson = Gson()

    routing {
        staticFiles("/assets", File("assets"))

        get("/") {
            call.respondText("Hello, Naija Ayo Worldwide!")
        }

        route("/api") {
             get("/rooms") {
                call.respond(rooms.values.toList())
            }

            post("/rooms") {
                val room = call.receive<Room>().copy(state = GameState())
                rooms[room.roomId] = room
                roomConnections[room.roomId] = mutableListOf()
                call.respond(room)
            }

            get("/leaderboard") {
                call.respond(users.values.toList().sortedByDescending { it.rating })
            }

            post("/users/{uid}/avatar") {
                val uid = call.parameters["uid"]
                if (uid == null) {
                    call.respond(HttpStatusCode.BadRequest, "User ID cannot be null.")
                    return@post
                }
                val avatarChoice = call.receive<Avatar>()
                val user = users.getOrPut(uid) { 
                    User(uid = uid, username = "Player $uid", email = "$uid@example.com") 
                }
                val updatedUser = user.copy(avatarId = avatarChoice.avatarId)
                users[uid] = updatedUser
                call.respond(HttpStatusCode.OK)
            }
        }

        webSocket("/ws") {
            var currentRoomId: String? = null
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        val json = JSONObject(text)
                        when (json.getString("type")) {
                            "join_room" -> {
                                val roomId = json.getString("roomId")
                                currentRoomId = roomId
                                roomConnections[roomId]?.add(this)
                                println("Player joined room $roomId. Total connections: ${roomConnections[roomId]?.size}")
                                rooms[roomId]?.state?.let {
                                    val stateUpdateEvent = mapOf("type" to "state_update", "data" to it)
                                    val stateUpdateJson = gson.toJson(stateUpdateEvent)
                                    outgoing.send(Frame.Text(stateUpdateJson))
                                }
                            }
                            "play_move" -> {
                                val roomId = json.getString("roomId")
                                val pitIndex = json.getInt("pitIndex")
                                val room = rooms[roomId]
                                val gameState = room?.state

                                if (room != null && gameState != null) {
                                    val newState = playMove(gameState, pitIndex)
                                    rooms[roomId] = room.copy(state = newState)
                                    
                                    val stateUpdateEvent = mapOf("type" to "state_update", "data" to newState)
                                    val stateUpdateJson = gson.toJson(stateUpdateEvent)
                                    
                                    roomConnections[roomId]?.forEach { session ->
                                        session.send(Frame.Text(stateUpdateJson))
                                    }
                                } 
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("WebSocket Error: ${e.localizedMessage}")
            } finally {
                currentRoomId?.let {
                    roomConnections[it]?.remove(this)
                    println("Player left room $it. Total connections: ${roomConnections[it]?.size}")
                }
            }
        }
    }
}
