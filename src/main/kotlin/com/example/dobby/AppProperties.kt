package com.example.dobby


import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration


@ConfigurationProperties
data class AppProperties(
    val supabase: Supabase = Supabase(),
    val gemini: Gemini = Gemini(),
    val discord: Discord = Discord(),
    val jwt: Jwt = Jwt(),
    val dev: Dev = Dev(),
    val frontend: Frontend = Frontend(),
    val encryption: Encryption = Encryption(),
    val app: App = App()
) {
    data class Supabase(
        var url: String = "",
        var key: String = ""
    )

    data class Gemini(
        var apiKey: String = "",
        var promptFilePath: String = "ai_prompt.txt"
    )

    data class Discord(
        var clientId: String = "",
        var clientSecret: String = "",
        var redirectUri: String = ""
    )

    data class Jwt(
        var secret: String = "",
        var expiration: Duration = Duration.ofHours(1)
    )

    data class Dev(
        var secretKey: String = ""
    )

    data class Encryption(
        var secretKey: String = ""
    )

    data class Frontend(
        var url: String = ""
    )

    data class App(
        var security: Security = Security()
    ) {
        data class Security(
            var apiKeyHeader: String = "",
            var apiKeySecret: String = ""
        )
    }
}