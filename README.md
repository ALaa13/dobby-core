# Dobby Backend

![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-Pub%2FSub-DC382D?logo=redis&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

A Kotlin/Spring Boot backend service that generates AI-powered roasts for **Discord** and a **Web Dashboard**. It
accepts chat history, enriches requests with stored user facts from Supabase, generates roasts via Google Gemini, and
delivers results in real-time via **Redis Pub/Sub**.

### Related Repositories

| Project                                              | Description                                                    |
|------------------------------------------------------|----------------------------------------------------------------|
| [Discord Bot](https://github.com/ALaa13/dobby)       | Subscribes to Redis and delivers roasts in your Discord server |
| [Web Dashboard](https://github.com/ALaa13/dobby-web) | Browser-based UI to view and manage roasts                     |

---

## Architecture

```
Discord Bot / Web Dashboard
         │
         │  POST /api/v1/roast
         ▼
┌─────────────────────┐
│   Dobby Backend     │  ──── Fetch user facts ──▶  Supabase
│   (Spring Boot)     │  ──── Generate roast  ──▶  Google Gemini
└─────────────────────┘
         │
         │  Publish to Redis channel: roast-delivery
         ▼
      Redis
         │
         │  Subscribe
         ▼
   Discord Bot  ──▶  Discord Channel
```

**Request lifecycle:**

1. Client sends chat history to `POST /api/v1/roast`
2. Backend responds immediately with `202 Accepted` (job is queued)
3. Service fetches stored user facts from Supabase for context
4. Gemini generates a personalized roast asynchronously
5. Result is published to the `roast-delivery` Redis channel
6. Discord bot (or other consumers) receive and deliver it in real-time

---

## Tech Stack

| Layer         | Technology                                          |
|---------------|-----------------------------------------------------|
| Language      | Kotlin 2.2 on Java 21                               |
| Framework     | Spring Boot 4.0 with virtual threads                |
| AI            | Google Gemini API                                   |
| Database      | Supabase (PostgREST)                                |
| Messaging     | Redis Pub/Sub                                       |
| Build         | Gradle (wrapper included — no local install needed) |
| Rate Limiting | Bucket4j (Token Bucket, in-memory)                  |

---

## Prerequisites

- **Java 21** — verify with `java -version`
- **Redis** — local or remote instance
- **API credentials** — Supabase, Google Gemini, and Discord OAuth2 (see [Configuration](#configuration))

> **Note:** Gradle is bundled via the wrapper (`./gradlew`). You do not need to install it separately.

---

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/ALaa13/dobby-core.git
cd dobby-core
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` with your credentials. See the full [Environment Variables Reference](#environment-variables-reference)
below.

### 3. Create the AI prompt file

```bash
cp ai_prompt.txt.example ai_prompt.txt
```

Customize `ai_prompt.txt` to define the bot's personality and roasting rules. If this file is missing, the system falls
back to a default prompt: `"You are a roast bot."`

### 4. Start Redis

```bash
# Using Docker (recommended)
docker run -d -p 6379:6379 redis:latest

# macOS with Homebrew
brew services start redis
```

### 5. Run the application

```bash
./gradlew bootRun
```

The server starts at `http://localhost:8080/api/v1/`

**Verify it's running:**

```bash
curl http://localhost:8080/api/v1/
# Expected: "Dobby Backend API is running smoothly."
```

---

## Running with Docker Compose (Recommended)

Docker Compose spins up both the Spring Boot service and a pre-configured Redis instance together, with no manual Redis
setup required.

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/) installed and running
- A fully populated `.env` file at the project root

### Start the stack

```bash
docker compose up -d --build
```

This command:

- Builds the Kotlin application inside a secure multi-stage container
- Fetches and starts Redis
- Links both services on a shared virtual network
- Runs everything in the background

### Stop the stack

```bash
docker compose down
```

---

## API Endpoints

The API is documented with **OpenAPI 3.0**. When the application is running locally, explore all endpoints interactively
via Swagger:

- **Swagger UI:** [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)
- **Raw OpenAPI spec:** [http://localhost:8080/api/v1/openapi.yaml](http://localhost:8080/api/v1/openapi.yaml)

To update the spec, edit:

```
src/main/resources/static/openapi.yaml
```

### Core Endpoints

| Method | Endpoint              | Auth    | Description                                           |
|--------|-----------------------|---------|-------------------------------------------------------|
| `POST` | `/api/v1/roast`       | API Key | Queues async roast generation; returns `202 Accepted` |
| `POST` | `/api/v1/fact`        | API Key | Stores a user fact for future roast context           |
| `GET`  | `/api/v1/logs/stream` | JWT     | Real-time SSE log stream for the web dashboard        |

> Roast generation is **asynchronous** — `202 Accepted` confirms the job was queued. The result arrives via Redis
> Pub/Sub, not in the HTTP response.

---

## Rate Limiting

API Key-authenticated endpoints are protected by an in-memory Token Bucket (via **Bucket4j**).

| Setting         | Value                              |
|-----------------|------------------------------------|
| Limit           | 10 requests per minute per API Key |
| Refill strategy | Greedy (~1 token every 6 seconds)  |

When the limit is exceeded, the API returns:

```json
{
  "status": 429,
  "error": "Too Many Requests",
  "message": "API Key rate limit exceeded. Please throttle your requests."
}
```

---

## Configuration

### Environment Variables Reference

| Variable                | Required | Description                                                                  |
|-------------------------|----------|------------------------------------------------------------------------------|
| `SUPABASE_URL`          | Yes      | Your Supabase project URL                                                    |
| `SUPABASE_KEY`          | Yes      | Supabase service key                                                         |
| `GEMINI_API_KEY`        | Yes      | Google Gemini API key                                                        |
| `DISCORD_CLIENT_ID`     | Yes      | Discord OAuth2 client ID                                                     |
| `DISCORD_CLIENT_SECRET` | Yes      | Discord OAuth2 client secret                                                 |
| `DISCORD_REDIRECT_URI`  | Yes      | OAuth2 redirect URI (e.g. `http://localhost:8080/login/oauth2/code/discord`) |
| `JWT_SECRET`            | Yes      | Secret key for signing JWTs                                                  |
| `JWT_EXPIRATION`        | Yes      | JWT TTL (e.g. `7d`, `12h`)                                                   |
| `BACKEND_API_HEADER`    | Yes      | API header name (e.g. `X-API-Key`)                                           |
| `BACKEND_API_KEY`       | Yes      | API key value for bot-to-backend auth                                        |
| `FRONTEND_URL`          | Yes      | Frontend URL for CORS (e.g. `http://localhost:4200`)                         |
| `REDIS_HOST`            | Yes      | Redis hostname or IP                                                         |
| `REDIS_PORT`            | Yes      | Redis port (default: `6379`)                                                 |
| `REDIS_PASSWORD`        | Yes      | Redis password (leave blank if none)                                         |
| `DOBBY_BOT_URL`         | Yes      | Discord bot service URL                                                      |
| `DOBBY_SECURITY_TOKEN`  | Yes      | Shared secret for backend-to-bot calls                                       |
| `SECRET_DEV_KEY`        | No       | Dev key for manual token generation                                          |

Full example: see `.env.example` in the repository root.

---

## Database Schema

Both tables live in your **Supabase** project. Create a foreign key relationship between them so the backend can use
embedded selection.

### `user_profiles`

One row per user per Discord guild.

| Column            | Type             | Notes                     |
|-------------------|------------------|---------------------------|
| `id`              | uuid             | Primary key               |
| `discord_user_id` | string           | Discord user snowflake ID |
| `guild_id`        | string           | Discord guild/server ID   |
| `display_name`    | string / null    | Optional                  |
| `created_at`      | timestamp        | Auto-set by Supabase      |
| `updated_at`      | timestamp / null | Auto-set by Supabase      |

### `user_facts`

Facts linked to a user profile, used as context for roast generation.

| Column               | Type             | Notes                            |
|----------------------|------------------|----------------------------------|
| `id`                 | uuid             | Primary key                      |
| `profile_id`         | uuid             | Foreign key → `user_profiles.id` |
| `fact_text`          | string           | The fact content                 |
| `source`             | enum             | `"USER_SUBMISSION"`              |
| `confidence_score`   | smallint / null  | Default: `80`                    |
| `roastability_score` | smallint / null  | Default: `20`                    |
| `created_at`         | timestamp        | Auto-set by Supabase             |
| `updated_at`         | timestamp / null | Auto-set by Supabase             |

---

## Redis Integration

### Channel: `roast-delivery`

| Role       | Component      | Behavior                                     |
|------------|----------------|----------------------------------------------|
| Publisher  | `RoastService` | Publishes after Gemini returns the result    |
| Subscriber | Discord Bot    | Receives and delivers to the Discord channel |

### Message format

```json
{
  "channelId": "123456789012345678",
  "content": "generated roast or error message",
  "success": true
}
```

---

## Project Structure

```
src/main/kotlin/com/example/dobby
├── DobbyApplication.kt       # Spring Boot entry point
├── config/                   # Gemini, Supabase, Redis, HTTP clients
├── controller/               # REST API controllers
├── dto/                      # Request / response models
├── exception/                # Global error handling
├── queue/                    # Redis Pub/Sub publishers & subscribers
├── llm/                      # Gemini API adapter & port interface
├── repository/               # Supabase data access wrappers
├── service/                  # Business logic (RoastService, FactService…)
├── supabase/                 # Supabase client configuration
└── logging/                  # SSE log emitter
```

---

## Testing

The test suite covers service logic, async background tasks, and web layer slices using **MockK** and **KotlinX
Coroutines Test**.

```bash
./gradlew test
```

Run this before opening a PR or deploying.

---

## Build & Deploy

### Build a JAR

```bash
./gradlew build
# Output: build/libs/dobby-core-0.0.1-SNAPSHOT.jar
```

### Run the JAR directly

```bash
java -jar build/libs/dobby-core-0.0.1-SNAPSHOT.jar
```

---

## Troubleshooting

| Problem                        | Solution                                                                                                                                |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| JAVA_HOME is not set           | Install Java 21 and set `export JAVA_HOME=/path/to/jdk-21`                                                                              |
| Supabase url must not be blank | Ensure `.env` exists with all Supabase variables filled in                                                                              |
| Gemini prompt file not found   | Create `ai_prompt.txt` in the repo root (copy from `ai_prompt.txt.example`)                                                             |
| Redis connection refused       | Verify Redis is running on the configured host/port; check `REDIS_HOST`, `REDIS_PORT` and `REDIS_PASSWORD`                              |
| Facts not appearing in roasts  | Confirm `discord_user_id` matches the message author, `guild_id` matches the request, and the Supabase table relationship is configured |
| JWT rejected / 401 errors      | Check `JWT_SECRET` matches across services and that `JWT_EXPIRATION` is set correctly                                                   |
| Missing env variables          | Run `cp .env.example .env` and fill in all required fields                                                                              |
| Gemini model unavailable       | Flash models fail over to backups automatically with a 15-minute cooldown per model                                                     |

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit your changes: `git commit -m "feat: describe your change"`
4. Push and open a Pull Request

Please ensure all tests pass (`./gradlew test`) before submitting.

---

## License

This project is licensed under the [MIT License](LICENSE).