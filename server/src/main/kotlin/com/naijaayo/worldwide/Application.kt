package com.naijaayo.worldwide

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init()
    val dbService = DatabaseService()
    val authService = AuthService(dbService)

    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }

    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JWT.require(authService.algorithm).build())
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    install(ContentNegotiation) {
        json()
    }

    configureRouting(authService)
}

fun Application.configureRouting(authService: AuthService) {
    routing {
        get("/") {
            call.respondText("Naija Ayo Server is running.")
        }

        post("/register") {
            val user = call.receive<ServerAuthRequest>()
            val result = authService.registerUser(user.username, user.email, user.password)
            result.fold(
                onSuccess = { authUser ->
                    val token = authService.generateToken(authUser)
                    call.respond(HttpStatusCode.Created, ServerAuthResponse(token))
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                }
            )
        }

        post("/login") {
            val user = call.receive<ServerAuthRequest>()
            val result = authService.loginUser(user.email, user.password)
            result.fold(
                onSuccess = { authUser ->
                    val token = authService.generateToken(authUser)
                    call.respond(HttpStatusCode.OK, ServerAuthResponse(token))
                },
                onFailure = { error ->
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to error.message))
                }
            )
        }
    }
}

@Serializable
data class ServerAuthRequest(val username: String = "", val email: String, val password: String)

@Serializable
data class ServerAuthResponse(val token: String)
