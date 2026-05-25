# dobby-core

`dobby-core` is a Kotlin/Spring Boot backend service for a Discord roast bot. It accepts Discord chat history, enriches
the roast request with stored user facts from Supabase, generates a roast through Google Gemini, and sends the result
back to the Discord bot service through an internal callback.

The service also exposes an endpoint for saving facts about Discord users. Those facts are later used as memory context
when generating roasts.

## Tech Stack

- Kotlin 2.2
- Java 21
- Spring Boot 4.0
- Gradle wrapper
- Google Gemini API via `com.google.genai:google-genai`
- Supabase PostgREST via `postgrest-kt`
- Ktor CIO HTTP client for callbacks to the Discord bot

## Project Layout

```text
src/main/kotlin/com/example/dobby
├── DobbyApplication.kt              # Spring Boot entry point
├── config/                          # Gemini, Supabase, HTTP, and JSON configuration
├── controller/                      # HTTP API controllers
├── dto/                             # Request/response and Supabase DTOs
├── exception/                       # Validation error handler
├── repository/                      # Repository wrappers around Supabase clients
├── service/                         # Roast, fact, Gemini, and callback logic
├── supabase/                        # Direct Supabase table access
└── util/                            # Simple logging helper
```

## What The Service Does

### Roast flow

1. A Discord bot sends chat history to `POST /api/v1/roast`.
2. The API returns `202 Accepted` immediately.
3. `RoastService` continues work asynchronously in a background coroutine.
4. The service extracts unique Discord user IDs from the submitted messages.
5. For each user, it looks up a matching profile in Supabase by `discord_user_id` and `guild_id`.
6. If facts exist for those users, they are formatted as memory context.
7. `GeminiService` builds a prompt using:
    - `ai_prompt.txt`
    - optional request `persona`
    - Supabase memory facts
    - submitted Discord messages
8. Gemini generates the roast.
9. `DiscordCallbackClient` posts the result to:

```text
{DOBBY_BOT_URL}/api/internal/deliver
```

The callback includes the configured `DOBBY_SECURITY_TOKEN` in the `X-Internal-Token` header.

If roast generation fails, the service reports a failed result back to the bot callback endpoint with `success: false`.

### Fact flow

1. A client sends a user fact to `POST /api/v1/fact`.
2. The service finds or creates a Supabase `user_profiles` row for the Discord user and guild.
3. It inserts a row into `user_facts`.
4. Future roast requests for that user/guild can include this fact as memory context.

### Gemini model selection

On application startup, `GeminiModelManager` asks the Gemini API for available models that support `generateContent`. It
prefers Gemini model names containing `flash`, and ignores vision models.

If discovery fails, it falls back to:

```text
gemini-2.5-flash
gemini-2.5-pro
```

If a selected model fails during generation, it is put on a 15-minute cooldown and another available model can be used
for later requests.

## Requirements

- Java 21 installed and available through `JAVA_HOME`
- Network access to:
    - Supabase
    - Google Gemini API
    - the Discord bot callback service
- Valid Supabase project URL/key
- Valid Gemini API key
- A running Discord bot service that can receive internal delivery callbacks

Check Java with:

```bash
java -version
```

The application is configured to use the Java 21 toolchain in `build.gradle.kts`.

## Environment Variables

The app imports `.env` as a Spring properties file through:

```properties
spring.config.import=optional:file:.env[.properties]
```

Create a `.env` file in the repository root:

```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-supabase-service-or-api-key
GEMINI_API_KEY=your-gemini-api-key
DOBBY_BOT_URL=http://localhost:3000
DOBBY_SECURITY_TOKEN=shared-internal-token
```

Environment variable details:

| Variable               | Required | Purpose                                                          |
|------------------------|----------|------------------------------------------------------------------|
| `SUPABASE_URL`         | Yes      | Supabase project URL.                                            |
| `SUPABASE_KEY`         | Yes      | Supabase key used by the backend.                                |
| `GEMINI_API_KEY`       | Yes      | API key used to create the Google Gemini client.                 |
| `DOBBY_BOT_URL`        | Yes      | Base URL of the Discord bot service that receives roast results. |
| `DOBBY_SECURITY_TOKEN` | Yes      | Shared token sent to the bot callback in `X-Internal-Token`.     |

Do not commit real `.env` values.

## Supabase Tables

The code expects these tables and column names.

### `user_profiles`

Used to store one profile per Discord user per guild.

Expected columns based on the DTOs:

| Column            | Type                  | Notes                             |
|-------------------|-----------------------|-----------------------------------|
| `id`              | string/uuid           | Primary key returned by Supabase. |
| `discord_user_id` | string                | Discord user ID.                  |
| `guild_id`        | string                | Discord guild/server ID.          |
| `display_name`    | string/null           | Optional display name.            |
| `created_at`      | string/timestamp      | Returned by Supabase.             |
| `updated_at`      | string/timestamp/null | Returned by Supabase.             |

The service queries profiles with:

```text
discord_user_id = request user ID
guild_id = request guild ID
```

### `user_facts`

Used to store facts attached to a profile.

Expected columns based on the DTOs:

| Column               | Type                  | Notes                                 |
|----------------------|-----------------------|---------------------------------------|
| `id`                 | string/uuid           | Primary key returned by Supabase.     |
| `profile_id`         | string/uuid           | References `user_profiles.id`.        |
| `fact_text`          | string                | The stored fact.                      |
| `source`             | string/enum           | Currently saved as `USER_SUBMISSION`. |
| `confidence_score`   | smallint/null         | Currently inserted as `80`.           |
| `roastability_score` | smallint/null         | Currently inserted as `20`.           |
| `created_at`         | string/timestamp      | Returned by Supabase.                 |
| `updated_at`         | string/timestamp/null | Returned by Supabase.                 |

The profile lookup selects related facts with:

```text
*, user_facts(*)
```

That means Supabase needs a relationship between `user_profiles` and `user_facts` for embedded selection to work.

## AI Prompt

The default prompt file is configured in `src/main/resources/application.properties`:

```properties
gemini.prompt.file=ai_prompt.txt
```

At startup, `GeminiService` loads `ai_prompt.txt` from the repository root. If the file is missing or cannot be read,
the service falls back to:

```text
You are a roast bot.
```

Edit `ai_prompt.txt` to change the roast persona/rules without changing Kotlin code.

## Running Locally

From the repository root:

```bash
./gradlew bootRun
```

By default, Spring Boot starts on port `8080`.

Health check:

```bash
curl http://localhost:8080/
```

Expected response:

```text
Hello, World!
```

## Building

```bash
./gradlew build
```

The runnable jar is produced under:

```text
build/libs/
```

Run the jar with:

```bash
java -jar build/libs/dobby-core-0.0.1-SNAPSHOT.jar
```

Make sure the required environment variables are available when running the jar.

## Testing

Run tests with:

```bash
./gradlew test
```

The current test suite contains a Spring context load test:

```text
src/test/kotlin/com/example/dobby/DobbyApplicationTests.kt
```

Because the application context creates Gemini and Supabase clients, tests may require valid environment values unless
test-specific configuration is added.

## API Reference

### `GET /`

Basic health/root endpoint.

Response:

```text
Hello, World!
```

### `POST /api/v1/roast`

Starts an asynchronous roast job.

Response status:

```text
202 Accepted
```

Request body:

```json
{
  "channelId": "123456789012345678",
  "guildId": "987654321098765432",
  "persona": "league of legends",
  "messages": [
    {
      "author": "111111111111111111",
      "content": "that take was terrible",
      "timestamp": "2026-05-25T16:30:00Z"
    },
    {
      "author": "222222222222222222",
      "content": "let him cook",
      "timestamp": "2026-05-25T16:31:00Z"
    }
  ]
}
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/roast \
  -H 'Content-Type: application/json' \
  -d '{
    "channelId": "123456789012345678",
    "guildId": "987654321098765432",
    "persona": "league of legends",
    "messages": [
      {
        "author": "111111111111111111",
        "content": "that take was terrible",
        "timestamp": "2026-05-25T16:30:00Z"
      }
    ]
  }'
```

The HTTP response has no body. The final result is delivered to the Discord bot callback endpoint as:

```json
{
  "channelId": "123456789012345678",
  "content": "generated roast text",
  "success": true
}
```

### `POST /api/v1/fact`

Stores a fact about a Discord user in Supabase.

Request body:

```json
{
  "fact": "Always rage quits after losing lane",
  "discordUserId": "111111111111111111",
  "guildId": "987654321098765432",
  "displayName": "SomeUser"
}
```

Example:

```bash
curl -X POST http://localhost:8080/api/v1/fact \
  -H 'Content-Type: application/json' \
  -d '{
    "fact": "Always rage quits after losing lane",
    "discordUserId": "111111111111111111",
    "guildId": "987654321098765432",
    "displayName": "SomeUser"
  }'
```

Response:

```text
200 OK
```

The endpoint currently returns no response body.

## Discord Bot Callback Contract

`dobby-core` expects the bot service to expose:

```text
POST {DOBBY_BOT_URL}/api/internal/deliver
```

Headers:

```text
Content-Type: application/json
X-Internal-Token: {DOBBY_SECURITY_TOKEN}
```

Body:

```json
{
  "channelId": "123456789012345678",
  "content": "generated roast text or error message",
  "success": true
}
```

## Operational Notes

- `/api/v1/roast` is asynchronous. A `202 Accepted` response only means the job was accepted, not that Gemini generation
  succeeded.
- Incoming API endpoints do not currently check an auth token. The configured `DOBBY_SECURITY_TOKEN` is only used when
  calling back to the bot service.
- Logging is currently written with `println` through `Logging.kt`.
- Gemini API retry options are configured for HTTP `408` and `429`, with 3 attempts.
- Spring virtual threads are enabled with `spring.threads.virtual.enabled=true`.
- The Gradle wrapper should be used instead of a system Gradle install.

## Troubleshooting

### `JAVA_HOME is not set and no 'java' command could be found`

Install Java 21 and export `JAVA_HOME`.

Example:

```bash
export JAVA_HOME=/path/to/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
```

Then retry:

```bash
./gradlew bootRun
```

### `Supabase url must not be blank` or `Supabase key must not be blank`

Check that `.env` exists in the repository root and includes:

```properties
SUPABASE_URL=...
SUPABASE_KEY=...
```

### Gemini prompt file not found

Make sure `ai_prompt.txt` exists in the repository root, or update:

```properties
gemini.prompt.file=ai_prompt.txt
```

### Roast request succeeds but Discord does not receive a result

Check:

- `DOBBY_BOT_URL` points to the running Discord bot service.
- The bot exposes `POST /api/internal/deliver`.
- The bot expects the same `DOBBY_SECURITY_TOKEN`.
- The backend logs for callback failures.

### Facts are not included in roasts

Check:

- The `discord_user_id` in `user_profiles` matches the `author` field in roast messages.
- The `guild_id` matches the roast request `guildId`.
- `user_facts.profile_id` references the matching `user_profiles.id`.
- Supabase embedded selection works for `*, user_facts(*)`.
