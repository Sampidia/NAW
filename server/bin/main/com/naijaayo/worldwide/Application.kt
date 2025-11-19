package com.naijaayo.worldwide

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

// All other imports are removed for this focused test

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    println("Starting incremental diagnostic module (Step 1: MongoService)")

    // STEP 1: Initialize MongoService.
    // If the server crashes with this line uncommented, the problem is inside MongoService initialization.
    // If the server runs, MongoService is not the direct cause.
    val mongoService = MongoService()
    println("MongoService instance has been created.")

    // All other services, plugins, and complex routes remain disabled for this test.
    // val authService = MongoAuthService(mongoService)
    // install(CORS) { ... }
    // install(Authentication) { ... }

    routing {
        get("/") {
            call.respondText(
                "Incremental Test (Step 1) is RUNNING. MongoService was initialized.",
                ContentType.Text.Plain,
                HttpStatusCode.OK
            )
        }
    }
    println("Routing has been configured.")
}
