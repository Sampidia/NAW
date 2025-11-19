package com.naijaayo.worldwide

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.ktor.server.auth.jwt.JWTPrincipal
import java.util.*

class AuthService(private val dbService: DatabaseService) {

    private val jwtSecret = System.getenv("JWT_SECRET") ?: "your-secret-key-change-in-production"
    private val jwtIssuer = "naija-ayo-worldwide"
    private val jwtAudience = "naija-ayo-users"
    private val algorithm = Algorithm.HMAC256(jwtSecret)

    private suspend fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(12, password.toCharArray())
    }

    private suspend fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    fun generateToken(user: AuthUser): String {
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

    fun validateToken(token: String): AuthUser? {
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

    private fun generateId(): String {
        return UUID.randomUUID().toString()
    }

    suspend fun registerUser(username: String, email: String, password: String): Result<AuthUser> {
        return try {
            val existingUser = dbService.getUserByUsername(username) ?: dbService.getUserByEmail(email)
            if (existingUser != null) {
                return Result.failure(Exception("Username or email already exists"))
            }

            val userId = generateId()
            val passwordHash = hashPassword(password)

            dbService.createUser(userId, username, email, passwordHash)

            // Create initial leaderboard entries for single player and multiplayer
            val currentTime = java.time.LocalDateTime.now()
            dbService.createLeaderboardEntry(
                LeaderboardEntry(
                    id = generateId(),
                    userId = userId,
                    username = username,
                    avatarId = "ayo",
                    gameMode = "single_player",
                    eloRating = 1200,
                    gamesPlayed = 0,
                    gamesWon = 0,
                    gamesLost = 0,
                    gamesDrawn = 0,
                    winStreak = 0,
                    bestWinStreak = 0,
                    lastPlayed = currentTime
                )
            )

            dbService.createLeaderboardEntry(
                LeaderboardEntry(
                    id = generateId(),
                    userId = userId,
                    username = username,
                    avatarId = "ayo",
                    gameMode = "multiplayer",
                    eloRating = 1200,
                    gamesPlayed = 0,
                    gamesWon = 0,
                    gamesLost = 0,
                    gamesDrawn = 0,
                    winStreak = 0,
                    bestWinStreak = 0,
                    lastPlayed = currentTime
                )
            )

            Result.success(AuthUser(userId, username, email, "ayo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(emailOrUsername: String, password: String): Result<AuthUser> {
        return try {
            val user = dbService.getUserByUsername(emailOrUsername) ?: dbService.getUserByEmail(emailOrUsername)
                ?: return Result.failure(Exception("User not found"))

            val passwordHash = dbService.getPasswordHash(user.id)
                ?: return Result.failure(Exception("User has no password set"))

            if (!verifyPassword(password, passwordHash)) {
                return Result.failure(Exception("Invalid password"))
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
