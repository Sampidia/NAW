package com.naijaayo.worldwide

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import org.slf4j.event.Level

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    println("Starting diagnostic module with CALL LOGGING...")

    // Install Call Logging to see every incoming request
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    // All other services remain disabled for this test.

    configureDiagnosticRouting()
}

fun Application.configureDiagnosticRouting() {
    routing {
        // Health check endpoint - THIS IS THE ONLY ACTIVE ROUTE
        get("/") {
            val message = "Diagnostic Test Server is running!"
            // This log will only appear if the request is successfully routed to this handler
            println("GET / route was successfully hit.") 
            call.respondText(message, ContentType.Text.Plain, HttpStatusCode.OK)
        }
    }
}

/*
// The original full routing is still disabled.
fun Application.configureRouting(authService: MongoAuthService, mongoService: MongoService) {
    // ...
}
*/
