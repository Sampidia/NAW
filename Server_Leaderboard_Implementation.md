# Server Leaderboard Implementation Guide

## Overview
This document provides a comprehensive technical specification for implementing the server-side infrastructure to support the Naija Ayo Worldwide leaderboard system. Designed specifically for deployment on **Render** with a scalable, production-ready architecture.

## 🏗️ Technology Stack

### Recommended Stack for Render Deployment

**Backend Framework:**
- **Node.js** with Express.js (JavaScript/TypeScript)
- **Java** with Spring Boot (Alternative)
- **Python** with FastAPI (Alternative)

**Database:**
- **PostgreSQL** (Recommended for Render - free tier available)
- **MySQL** (Alternative)

**Additional Services:**
- **Redis** (Optional - for caching and real-time features)
- **WebSocket** support for real-time updates

**Deployment Platform:**
- **Render** - Web Services for backend API
- **Render PostgreSQL** - Managed database service

## 📊 Database Schema Design

### Core Tables

#### 1. Users Table
```sql
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avatar_id VARCHAR(50) DEFAULT 'ayo',
    rating_single_player INTEGER DEFAULT 1000,
    rating_multiplayer INTEGER DEFAULT 1000,
    wins_single_player INTEGER DEFAULT 0,
    losses_single_player INTEGER DEFAULT 0,
    wins_multiplayer INTEGER DEFAULT 0,
    losses_multiplayer INTEGER DEFAULT 0,
    games_played_single_player INTEGER DEFAULT 0,
    games_played_multiplayer INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fcm_token VARCHAR(255)
);

-- Indexes for performance
CREATE INDEX idx_users_rating_single ON users(rating_single_player DESC);
CREATE INDEX idx_users_rating_multi ON users(rating_multiplayer DESC);
CREATE INDEX idx_users_username ON users(username);
```

#### 2. Games Table
```sql
CREATE TABLE games (
    id SERIAL PRIMARY KEY,
    game_id VARCHAR(255) UNIQUE NOT NULL,
    player1_id VARCHAR(255) NOT NULL REFERENCES users(id),
    player2_id VARCHAR(255) REFERENCES users(id), -- NULL for AI games
    player1_score INTEGER NOT NULL,
    player2_score INTEGER NOT NULL,
    winner INTEGER NOT NULL CHECK (winner IN (1, 2, 0)), -- 1=player1, 2=player2, 0=draw
    game_mode VARCHAR(20) NOT NULL CHECK (game_mode IN ('single', 'multiplayer')),
    is_single_player BOOLEAN NOT NULL,
    moves_data JSONB, -- Store game moves if needed
    duration_seconds INTEGER, -- Game duration for analytics
    completed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_games_player1_id ON games(player1_id);
CREATE INDEX idx_games_player2_id ON games(player2_id);
CREATE INDEX idx_games_completed_at ON games(completed_at DESC);
CREATE INDEX idx_games_mode_rating ON games(game_mode, completed_at DESC);
```

#### 3. User_Statistics Table (Optional - for detailed analytics)
```sql
CREATE TABLE user_statistics (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id),
    date DATE NOT NULL,
    games_won_single INTEGER DEFAULT 0,
    games_lost_single INTEGER DEFAULT 0,
    games_won_multi INTEGER DEFAULT 0,
    games_lost_multi INTEGER DEFAULT 0,
    average_score_single DECIMAL(5,2),
    average_score_multi DECIMAL(5,2),
    longest_win_streak INTEGER DEFAULT 0,
    current_win_streak INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, date)
);
```

## 🔌 API Endpoints Specification

### Base URL: `https://your-app-name.onrender.com`

### 1. Game Result Submission
**Endpoint:** `POST /api/games/complete`
**Purpose:** Submit completed game results for statistics tracking

**Request Body:**
```json
{
  "gameId": "optional-game-id",
  "player1Id": "user123",
  "player2Id": "opponent456-or-null-for-ai",
  "player1Score": 25,
  "player2Score": 15,
  "winner": 1,
  "isSinglePlayer": true,
  "gameMode": "single",
  "completedAt": "2025-01-20T14:30:00.000Z"
}
```

**Response:**
```json
{
  "success": true,
  "gameId": "generated-or-submitted-id",
  "updatedStats": {
    "userId": "user123",
    "newRating": 1050,
    "gamesPlayed": 15,
    "wins": 10,
    "losses": 5,
    "winRate": 66.67
  }
}
```

### 2. Leaderboard Retrieval
**Endpoint:** `GET /api/leaderboard`
**Purpose:** Get current leaderboard rankings

**Query Parameters:**
- `mode` (optional): `single` or `multiplayer` (default: both)
- `limit` (optional): Number of results (default: 50, max: 100)
- `offset` (optional): Pagination offset (default: 0)

**Response:**
```json
{
  "singlePlayerLeaderboard": [
    {
      "rank": 1,
      "userId": "user123",
      "username": "NaijaPlayer",
      "avatarId": "ayo",
      "rating": 1250,
      "gamesPlayed": 25,
      "wins": 18,
      "losses": 7,
      "winRate": 72.0
    }
  ],
  "multiplayerLeaderboard": [
    {
      "rank": 1,
      "userId": "user456",
      "username": "AyoMaster",
      "avatarId": "fatima",
      "rating": 1450,
      "gamesPlayed": 30,
      "wins": 22,
      "losses": 8,
      "winRate": 73.33
    }
  ],
  "lastUpdated": "2025-01-20T14:30:00.000Z"
}
```

### 3. User Statistics
**Endpoint:** `GET /api/users/{userId}/stats`
**Purpose:** Get detailed statistics for a specific user

**Response:**
```json
{
  "userId": "user123",
  "username": "NaijaPlayer",
  "avatarId": "ayo",
  "singlePlayerStats": {
    "rating": 1250,
    "gamesPlayed": 25,
    "wins": 18,
    "losses": 7,
    "winRate": 72.0,
    "averageScore": 22.5,
    "bestScore": 35,
    "currentStreak": 3,
    "longestStreak": 7
  },
  "multiplayerStats": {
    "rating": 1100,
    "gamesPlayed": 15,
    "wins": 8,
    "losses": 7,
    "winRate": 53.33,
    "averageScore": 18.2,
    "bestScore": 28,
    "currentStreak": 1,
    "longestStreak": 4
  },
  "lastActive": "2025-01-20T14:30:00.000Z"
}
```

### 4. Avatar Update
**Endpoint:** `POST /api/users/{userId}/avatar`
**Purpose:** Update user's avatar selection

**Request Body:**
```json
{
  "avatarId": "fatima"
}
```

## 🧮 Statistics Calculation Algorithm

### Elo Rating System Implementation

**Single-Player Rating Calculation:**
```kotlin
fun calculateNewRating(currentRating: Int, gameResult: GameResult): Int {
    // AI difficulty factor (can be adjusted)
    val aiStrength = 1000 // Base AI rating

    val expectedScore = 1.0 / (1.0 + Math.pow(10.0, (aiStrength - currentRating) / 400.0))
    val actualScore = when (gameResult.winner) {
        1 -> 1.0 // Player won
        2 -> 0.0 // AI won
        else -> 0.5 // Draw
    }

    val kFactor = when (currentRating) {
        in 0..1199 -> 32
        in 1200..1799 -> 24
        else -> 16
    }

    val newRating = currentRating + kFactor * (actualScore - expectedScore)
    return Math.round(newRating).toInt()
}
```

**Multiplayer Rating Calculation:**
```kotlin
fun calculateNewRatings(player1Rating: Int, player2Rating: Int, player1Won: Boolean): Pair<Int, Int> {
    val expectedScore1 = 1.0 / (1.0 + Math.pow(10.0, (player2Rating - player1Rating) / 400.0))
    val expectedScore2 = 1.0 / (1.0 + Math.pow(10.0, (player1Rating - player2Rating) / 400.0))

    val actualScore1 = if (player1Won) 1.0 else 0.0
    val actualScore2 = 1.0 - actualScore1

    val kFactor1 = getKFactor(player1Rating)
    val kFactor2 = getKFactor(player2Rating)

    val newRating1 = Math.round(player1Rating + kFactor1 * (actualScore1 - expectedScore1)).toInt()
    val newRating2 = Math.round(player2Rating + kFactor2 * (actualScore2 - expectedScore2)).toInt()

    return Pair(newRating1, newRating2)
}

fun getKFactor(rating: Int): Int {
    return when {
        rating < 1200 -> 32
        rating < 1800 -> 24
        else -> 16
    }
}
```

## 🚀 Render Deployment Guide

### 1. Backend Service Setup

**Create New Web Service on Render:**
1. Connect your GitHub repository
2. Select **Node.js** as runtime
3. Set build command: `npm install`
4. Set start command: `npm start`
5. Add environment variables:
   ```
   DATABASE_URL=postgresql://username:password@hostname:5432/database
   PORT=10000
   NODE_ENV=production
   ```

### 2. PostgreSQL Database Setup

**Create Managed Database on Render:**
1. Add **PostgreSQL** database
2. Note the external database URL
3. Use this URL in your application configuration

**Database Configuration Example (Node.js):**
```javascript
const { Client } = require('pg');

const client = new Client({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false }
});

module.exports = client;
```

### 3. Environment Configuration

**Required Environment Variables:**
```bash
# Database
DATABASE_URL=your-postgresql-connection-string

# Application
PORT=10000
NODE_ENV=production

# Optional: CORS (for development)
ALLOWED_ORIGINS=https://your-android-app-domain.com

# Optional: JWT Secret (if implementing authentication)
JWT_SECRET=your-secret-key-here
```

## 🔒 Security Considerations

### CORS Configuration
```javascript
const corsOptions = {
  origin: function (origin, callback) {
    const allowedOrigins = [
      'https://your-android-app-domain.com',
      'http://localhost:3000', // For development
      'https://your-frontend-domain.com' // If you have a web version
    ];

    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true
};

app.use(cors(corsOptions));
```

### Input Validation
```javascript
// Validate game result submission
const gameResultSchema = Joi.object({
  player1Id: Joi.string().required(),
  player2Id: Joi.string().allow(null),
  player1Score: Joi.number().integer().min(0).required(),
  player2Score: Joi.number().integer().min(0).required(),
  winner: Joi.number().integer().valid(0, 1, 2).required(),
  isSinglePlayer: Joi.boolean().required(),
  gameMode: Joi.string().valid('single', 'multiplayer').required(),
  completedAt: Joi.date().iso().required()
});
```

## 📱 Android App Integration

### Updated API Service Interface

**File:** `app/src/main/java/com/naijaayo/worldwide/network/ApiService.kt`

```kotlin
interface ApiService {
    // Existing endpoints...

    @POST("/api/games/complete")
    suspend fun submitGameResult(@Body gameResult: GameResult): GameResult

    @GET("/api/leaderboard")
    suspend fun getLeaderboard(
        @Query("mode") mode: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LeaderboardResponse

    @GET("/api/users/{userId}/stats")
    suspend fun getUserStats(@Path("userId") userId: String): UserStatsResponse
}
```

### Additional Data Classes

**File:** `common/src/main/kotlin/com/naijaayo/worldwide/Game.kt`

```kotlin
data class LeaderboardResponse(
    val singlePlayerLeaderboard: List<LeaderboardEntry>,
    val multiplayerLeaderboard: List<LeaderboardEntry>,
    val lastUpdated: String
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val avatarId: String,
    val rating: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double
)

data class UserStatsResponse(
    val userId: String,
    val username: String,
    val avatarId: String,
    val singlePlayerStats: PlayerStats,
    val multiplayerStats: PlayerStats,
    val lastActive: String
)

data class PlayerStats(
    val rating: Int,
    val gamesPlayed: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val averageScore: Double,
    val bestScore: Int,
    val currentStreak: Int,
    val longestStreak: Int
)
```

## 🔄 Real-Time Updates (Optional)

### WebSocket Integration for Live Leaderboard

**Server-Side WebSocket:**
```javascript
const WebSocket = require('ws');

const wss = new WebSocket.Server({ server });

// Broadcast leaderboard updates
function broadcastLeaderboardUpdate() {
  const updateData = {
    type: 'leaderboard_update',
    timestamp: new Date().toISOString(),
    leaderboard: getCurrentLeaderboard()
  };

  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(JSON.stringify(updateData));
    }
  });
}
```

**Android Integration:**
```kotlin
// In GameViewModel.kt
private fun setupWebSocketConnection() {
    // Connect to server WebSocket
    // Listen for leaderboard updates
    // Update LiveData when updates received
}
```

## 🧪 Testing & Development

### Local Development Setup

**1. Start Local PostgreSQL:**
```bash
docker run -d \
  --name postgres-dev \
  -e POSTGRES_PASSWORD=development123 \
  -e POSTGRES_DB=naijaayo_dev \
  -p 5432:5432 \
  postgres:13
```

**2. Environment Variables for Development:**
```bash
DATABASE_URL=postgresql://localhost:5432/naijaayo_dev
PORT=3001
NODE_ENV=development
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001
```

**3. Test Data Seeding:**
```sql
-- Insert test users
INSERT INTO users (id, username, email, avatar_id) VALUES
('user1', 'TestPlayer1', 'test1@email.com', 'ayo'),
('user2', 'TestPlayer2', 'test2@email.com', 'fatima');

-- Insert test game results
INSERT INTO games (game_id, player1_id, player1_score, player2_score, winner, game_mode, is_single_player, completed_at) VALUES
('game1', 'user1', 25, 15, 1, 'single', true, CURRENT_TIMESTAMP);
```

## 📊 Database Migration Scripts

### Initial Schema Migration
```sql
-- Migration: 001_initial_schema.sql

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    avatar_id VARCHAR(50) DEFAULT 'ayo',
    rating_single_player INTEGER DEFAULT 1000,
    rating_multiplayer INTEGER DEFAULT 1000,
    wins_single_player INTEGER DEFAULT 0,
    losses_single_player INTEGER DEFAULT 0,
    wins_multiplayer INTEGER DEFAULT 0,
    losses_multiplayer INTEGER DEFAULT 0,
    games_played_single_player INTEGER DEFAULT 0,
    games_played_multiplayer INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fcm_token VARCHAR(255)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_users_rating_single ON users(rating_single_player DESC);
CREATE INDEX IF NOT EXISTS idx_users_rating_multi ON users(rating_multiplayer DESC);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Create games table
CREATE TABLE IF NOT EXISTS games (
    id SERIAL PRIMARY KEY,
    game_id VARCHAR(255) UNIQUE NOT NULL,
    player1_id VARCHAR(255) NOT NULL REFERENCES users(id),
    player2_id VARCHAR(255) REFERENCES users(id),
    player1_score INTEGER NOT NULL,
    player2_score INTEGER NOT NULL,
    winner INTEGER NOT NULL CHECK (winner IN (1, 2, 0)),
    game_mode VARCHAR(20) NOT NULL CHECK (game_mode IN ('single', 'multiplayer')),
    is_single_player BOOLEAN NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for games
CREATE INDEX IF NOT EXISTS idx_games_player1_id ON games(player1_id);
CREATE INDEX IF NOT EXISTS idx_games_completed_at ON games(completed_at DESC);
```

## 🚀 Deployment Checklist

### Pre-Deployment
- [ ] Database schema created and tested
- [ ] API endpoints implemented and tested
- [ ] Environment variables configured
- [ ] CORS settings verified
- [ ] Error handling implemented
- [ ] Input validation added
- [ ] Test data seeded

### Render-Specific Setup
- [ ] GitHub repository connected to Render
- [ ] PostgreSQL database provisioned
- [ ] Web service configured with correct build/start commands
- [ ] Database URL added to environment variables
- [ ] Domain name configured (optional)

### Post-Deployment Verification
- [ ] API endpoints responding correctly
- [ ] Database connection working
- [ ] Leaderboard updates functioning
- [ ] Android app can submit game results
- [ ] Leaderboard displays updated data

## 📈 Monitoring & Analytics

### Application Monitoring
- **Render Dashboard**: Monitor service uptime and logs
- **Database Performance**: Monitor query performance and connection pool
- **Error Tracking**: Implement logging for failed API calls

### Analytics Events to Track
- Game completion rates
- User engagement metrics
- Rating distribution analysis
- Popular game modes

## 🔧 Maintenance & Updates

### Database Backups
- Render PostgreSQL provides automatic backups
- Consider additional backup strategies for production data

### Version Updates
- Monitor for Render platform updates
- Keep dependencies updated for security
- Test application after framework updates

---

## 📞 Support & Troubleshooting

**Common Issues:**
1. **Database Connection Errors**: Verify DATABASE_URL and credentials
2. **CORS Issues**: Check ALLOWED_ORIGINS configuration
3. **Port Binding**: Ensure PORT environment variable is set correctly
4. **Memory Issues**: Monitor application memory usage on Render

**Debugging Tips:**
- Check Render service logs for errors
- Verify database connectivity with simple queries
- Test API endpoints with tools like Postman
- Monitor application metrics in Render dashboard

This implementation provides a solid foundation for the Naija Ayo Worldwide leaderboard system with scalability, performance, and maintainability in mind.

1 = app\src\main\res\drawable\one_seed.png
2 = app\src\main\res\drawable\two_seed.png
3 = app\src\main\res\drawable\three_seed.png
4 = app\src\main\res\drawable\four_seed.png
5 = app\src\main\res\drawable\five_seed.png
6 = app\src\main\res\drawable\six_seed.png
7 = app\src\main\res\drawable\seven_seed.png
8 = app\src\main\res\drawable\eight_seed.png
9 = app\src\main\res\drawable\nine_seed.png
10 = app\src\main\res\drawable\ten_seed.png
11= app\src\main\res\drawable\eleven_seed.png
12 = app\src\main\res\drawable\twelve_seed.png
11 = app\src\main\res\drawable\eleven_seed.png
12 = app\src\main\res\drawable\twelve_seed.png
13 = app\src\main\res\drawable\thirteen_seed.png
14 = app\src\main\res\drawable\fourteen_seed.png
15 and above = app\src\main\res\drawable\fifteen_seed.png 