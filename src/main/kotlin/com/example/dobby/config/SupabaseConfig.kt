package com.example.dobby.config

import com.example.dobby.AppProperties
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.HttpRequestRetry
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SupabaseConfig(
    private val appProperties: AppProperties,
) {
    @OptIn(SupabaseInternal::class)
    @Bean
    fun supabaseClient(): SupabaseClient {
        val supabaseUrl = appProperties.supabase.url
        val supabaseKey = appProperties.supabase.key

        require(supabaseUrl.isNotBlank()) { "Supabase url must not be blank" }
        require(supabaseKey.isNotBlank()) { "Supabase key must not be blank" }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey,
        ) {
            httpConfig {
                install(HttpRequestRetry) {
                    applySharedRetrySettings(this)
                }
            }
            install(Postgrest)
            defaultSerializer =
                KotlinXSerializer(
                    Json {
                        encodeDefaults = true
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
        }
    }
}
