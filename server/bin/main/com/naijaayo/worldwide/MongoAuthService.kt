package com.naijaayo.worldwide

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.server.auth.jwt.*
import java.util.*

class MongoAuthService(private val mongoService: MongoService) {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
    private val jwtIssuer = "naija-ayo-worldwide"
    private val jwtAudience = "naija-ayo-users"
    val algorithm = Algorithm.HMAC256(jwtSecret)

    suspend fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    suspend fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    suspend fun generateToken(user: AuthUser): String {
        return JWT.create()
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("userId", user.id)
            .withClaim("username", user.username)
            .withClaim("email", user.email)
            .withClaim("avatarId", user.avatarId)
            .withExpiresAt(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) // 7 days
            .sign(algorithm)
    }

    suspend fun validateToken(token: String): AuthUser? {
        return try {
            val verifier = JWT.require(algorithm)
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .build()

            val decoded = verifier.verify(token)
            AuthUser(
                id = decoded.getClaim("userId").asString(),
                username = decoded.getClaim("username").asString(),
                email = decoded.getClaim("email").asString(),
                avatarId = decoded.getClaim("avatarId").asString()
            )
        } catch (e: JWTVerificationException) {
            null
        }
    }

    suspend fun registerUser(username: String, email: String, password: String): Result<AuthUser> {
        return try {
            // Check if username or email already exists
            val existingUserByUsername = mongoService.getUserByUsername(username)
            val existingUserByEmail = mongoService.getUserByEmail(email)

            if (existingUserByUsername != null || existingUserByEmail != null) {
                return Result.failure(Exception("Username or email already exists"))
            }

            val passwordHash = hashPassword(password)
            val userId = generateId()

            val user = MongoUser(
                id = userId,
                username = username,
                email = email,
                passwordHash = passwordHash,
                avatarId = "ayo"
            )

            mongoService.createUser(user)

            // Create leaderboard entries
            val singlePlayerEntry = MongoLeaderboardEntry(
                userId = userId,
                username = username,
                avatarId = "ayo",
                gameMode = "single_player"
            )

            val multiplayerEntry = MongoLeaderboardEntry(
                userId = userId,
                username = username,
                avatarId = "ayo",
                gameMode = "multiplayer"
            )

            mongoService.createLeaderboardEntry(singlePlayerEntry)
            mongoService.createLeaderboardEntry(multiplayerEntry)

            Result.success(AuthUser(userId, username, email, "ayo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(emailOrUsername: String, password: String): Result<AuthUser> {
        return try {
            val user = mongoService.getUserByUsername(emailOrUsername) ?: mongoService.getUserByEmail(emailOrUsername)

            if (user == null) {
                return Result.failure(Exception("User not found"))
            }

            if (!verifyPassword(password, user.passwordHash)) {
                return Result.failure(Exception("Invalid password"))
            }

            // Update last seen and online status
            mongoService.updateUserLastSeen(user.id)

            Result.success(AuthUser(
                id = user.id,
                username = user.username,
                email = user.email,
                avatarId = user.avatarId
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(call: io.ktor.server.application.ApplicationCall): AuthUser? {
        return try {
            val principal = call.principal<JWTPrincipal>()
            principal?.let {
                val id = it.getClaim("userId", String::class) ?: return@getCurrentUser null
                val username = it.getClaim("username", String::class) ?: return@getCurrentUser null
                val email = it.getClaim("email", String::class) ?: return@getCurrentUser null
                val avatarId = it.getClaim("avatarId", String::class) ?: return@getCurrentUser null

                AuthUser(id, username, email, avatarId)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserById(userId: String): AuthUser? {
        return mongoService.getUserById(userId)?.let {
            AuthUser(
                id = it.id,
                username = it.username,
                email = it.email,
                avatarId = it.avatarId
            )
        }
    }

    fun generateId(): String {
        return UUID.randomUUID().toString()
    }
}
