package com.naijaayo.worldwide

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
// All other imports are commented out for this diagnostic build
// import io.ktor.server.websocket.*
// import io.ktor.websocket.*
// import kotlinx.coroutines.channels.consumeEach
// import kotlinx.serialization.json.Json
// import io.ktor.server.auth.*
// import io.ktor.server.auth.jwt.*
// import io.ktor.server.request.*
// import io.ktor.serialization.kotlinx.json.*
// import java.util.concurrent.ConcurrentHashMap
// import kotlinx.serialization.Serializable
// import com.auth0.jwt.JWT

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    println("Starting diagnostic module...")

    // All services and plugins are disabled for this test.
    // val mongoService = MongoService()
    // val authService = MongoAuthService(mongoService)

    // install(io.ktor.server.plugins.cors.routing.CORS) { ... }
    // install(Authentication) { ... }
    // install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { ... }
    // install(WebSockets)

    // Only the diagnostic routing is active.
    configureDiagnosticRouting()
}

fun Application.configureDiagnosticRouting() {
    routing {
        // Health check endpoint - THIS IS THE ONLY ACTIVE ROUTE
        get("/") {
            val message = "Diagnostic Test Server is running!"
            println(message) // Log to confirm the route is hit
            call.respondText(message, ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}

/*
fun Application.configureRouting(authService: MongoAuthService, mongoService: MongoService) {
    // ALL ORIGINAL ROUTING IS DISABLED
}
*/
