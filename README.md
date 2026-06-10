# Dobby Backend

A Kotlin/Spring Boot backend service that generates AI-powered roasts for **Discord** and a **Web Dashboard**.
It accepts chat history, enriches requests with stored user facts from Supabase, generates roasts via Google Gemini,
and returns results to your Discord bot or to the Web Dashboard.

### Integrations
- **[Discord Bot](https://github.com/ALaa13/dobby)** — Real-time roasts in your server
- **[Web Dashboard](https://github.com/ALaa13/dobby-web)** — View and manage roasts via browser


## 🎯 What It Does

- **Generate Roasts**: Accepts chat history from Discord and generates contextual roasts using Google Gemini AI
- **Store Facts**: Saves user-specific facts that are used as context for more personalized roasts
- **Async Processing**: Handles roast generation asynchronously and delivers results via callback

## 📋 Prerequisites

- **Java 21** (check with `java -version`)
- **Network access** to Supabase, Google Gemini API, and your Discord bot service
- **API Keys**: Supabase credentials, Google Gemini API key, and Discord OAuth2 credentials

## 🚀 Quick Start

### 1. Clone & Setup

```bash
git clone https://github.com/ALaa13/dobby-core.git
cd dobby-core
```

### 2. Configure Environment

Copy the example environment file and fill in your credentials:

```bash
cp .env.example .env
```

Edit `.env` with your actual values:

```properties
# Core Service
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-supabase-service-key
GEMINI_API_KEY=your-gemini-api-key
DOBBY_BOT_URL=http://localhost:3000
DOBBY_SECURITY_TOKEN=your-shared-secret-token
# Discord OAuth2 (from Discord Developer Portal)
DISCORD_CLIENT_ID=your-client-id
DISCORD_CLIENT_SECRET=your-client-secret
DISCORD_REDIRECT_URI=http://localhost:8080/login/oauth2/code/discord
# JWT Configuration
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=7d
# Development & API Security
SECERTE_DEV_KEY=your-dev-secret-key
BACKEND_API_HEADER=X-API-Key
BACKEND_API_KEY=your-backend-api-key
# Frontend Integration
FRONTEND_URL=http://localhost:4200
```

### 3. Run

```bash
./gradlew bootRun
```

Server starts on `http://localhost:8080/api/v1/`

**Health check:**

```bash
curl http://localhost:8080/api/v1/
# Expected: "Hello, World!"
```

## 📚 Tech Stack

- **Kotlin 2.2** & **Java 21**
- **Spring Boot 4.0** with virtual threads
- **Google Gemini API** for AI roast generation
- **Supabase PostgREST** for data persistence
- **Ktor CIO** HTTP client for Discord bot callbacks
- **Gradle** for build management

## 🔌 API Endpoints

### Generate a Roast

```
POST /api/v1/roast
```

**Request:**

```json
{
  "channelId": "123456789012345678",
  "guildId": "987654321098765432",
  "persona": "Arch Linux user",
  "messages": [
    {
      "author": "111111111111111111",
      "content": "You use a GUI for that?",
      "timestamp": "2026-05-25T16:30:00Z"
    }
  ]
}
```

**Response:** `202 Accepted` (roast generated asynchronously)

### Store a User Fact

```
POST /api/v1/fact
```

**Request:**

```json
{
  "fact": "He'll fix a kernel panic before fixing his posture.",
  "discordUserId": "111111111111111111",
  "guildId": "987654321098765432",
  "displayName": "SomeUser"
}
```

**Response:** `200 OK`

## 🗄️ Database Schema

### Required Supabase Tables

## `user_profiles`

One profile per user per guild.

| Column            | Type           | Notes                   |
|-------------------|----------------|-------------------------|
| `id`              | uuid           | Primary key             |
| `discord_user_id` | string         | Discord user ID         |
| `guild_id`        | string         | Discord guild/server ID |
| `display_name`    | string/null    | Optional                |
| `created_at`      | timestamp      | Auto-set by Supabase    |
| `updated_at`      | timestamp/null | Auto-set by Supabase    |

## `user_facts`

Facts linked to user profiles.

| Column               | Type           | Notes                         |
|----------------------|----------------|-------------------------------|
| `id`                 | uuid           | Primary key                   |
| `profile_id`         | uuid           | References `user_profiles.id` |
| `fact_text`          | string         | The fact                      |
| `source`             | enum           | "USER_SUBMISSION"             |
| `confidence_score`   | smallint/null  | Default: 80                   |
| `roastability_score` | smallint/null  | Default: 20                   |
| `created_at`         | timestamp      | Auto-set by Supabase          |
| `updated_at`         | timestamp/null | Auto-set by Supabase          |

**Note:** Create a relationship in Supabase between these tables for embedded selection.

## ⚙️ Configuration

### Environment Variables Reference

| Variable                | Required | Purpose                              |
|-------------------------|----------|--------------------------------------|
| `SUPABASE_URL`          | Yes      | Your Supabase project URL            |
| `SUPABASE_KEY`          | Yes      | Supabase service key                 |
| `GEMINI_API_KEY`        | Yes      | Google Gemini API key                |
| `DOBBY_BOT_URL`         | Yes      | Base URL of your Discord bot service |
| `DOBBY_SECURITY_TOKEN`  | Yes      | Shared token for internal callbacks  |
| `DISCORD_CLIENT_ID`     | Yes      | Discord OAuth2 client ID             |
| `DISCORD_CLIENT_SECRET` | Yes      | Discord OAuth2 client secret         |
| `DISCORD_REDIRECT_URI`  | Yes      | Discord OAuth2 redirect URI          |
| `JWT_SECRET`            | Yes      | Secret key for JWT signing           |
| `JWT_EXPIRATION`        | Yes      | JWT expiration time (e.g., 7d, 7h)   |
| `SECERTE_DEV_KEY`       | Yes      | Development secret key               |
| `BACKEND_API_HEADER`    | Yes      | API header name (e.g., X-API-Key)    |
| `BACKEND_API_KEY`       | Yes      | API key value                        |
| `FRONTEND_URL`          | Yes      | Frontend application URL (for CORS)  |

**⚠️ Do not commit real `.env` values to version control.**

### AI Prompt

Edit `ai_prompt.txt` in the repository root to customize roast behavior. If missing, falls back to:
`"You are a roast bot."`

### Project Structure

```
src/main/kotlin/com/example/dobby
├── DobbyApplication.kt          # Spring Boot entry point
├── config/                      # Gemini, Supabase, HTTP config
├── controller/                  # HTTP API controllers
├── dto/                         # Request/response models
├── exception/                   # Error handling
├── repository/                  # Supabase wrappers
├── service/                     # Business logic
└── util/                        # Helpers
```

## 🏗️ Build & Deploy

### Build JAR

```bash
./gradlew build
```

Output: `build/libs/dobby-core-0.0.1-SNAPSHOT.jar`

### Run JAR

```bash
java -jar build/libs/dobby-core-0.0.1-SNAPSHOT.jar
```

## 📡 Discord Bot Integration

Your Discord bot must expose this endpoint to receive roast results:

```
POST {DOBBY_BOT_URL}/api/internal/deliver
```

**Headers:**

```
Content-Type: application/json
X-Internal-Token: {DOBBY_SECURITY_TOKEN}
```

**Body:**

```json
{
  "channelId": "123456789012345678",
  "content": "generated roast or error message",
  "success": true
}
```

## 🐛 Troubleshooting

| Problem                          | Solution                                                                                                             |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------|
| `JAVA_HOME is not set`           | Install Java 21 and set `export JAVA_HOME=/path/to/jdk-21`                                                           |
| `Supabase url must not be blank` | Check `.env` exists with all required Supabase variables filled                                                      |
| Gemini prompt file not found     | Create `ai_prompt.txt` in repo root or update `gemini.prompt.file` in `application.properties`                       |
| Discord doesn't receive results  | Verify `DOBBY_BOT_URL`, `/api/internal/deliver` endpoint exists, and `DOBBY_SECURITY_TOKEN` matches                  |
| Facts not in roasts              | Ensure `discord_user_id` matches message author, `guild_id` matches request, and Supabase relationship is configured |
| Missing environment variables    | Run `cp .env.example .env` and fill in all required values                                                           |

## ℹ️ Notes

- Roast generation is **asynchronous** — `202 Accepted` only confirms the job was queued
- Internal callbacks use `DOBBY_SECURITY_TOKEN` — incoming endpoints require proper API key authentication
- Virtual threads are enabled for better concurrency
- Gemini models with "flash" are preferred; models fail over to backups with 15-minute cooldowns
- Always use `.env.example` as a reference for required variables
