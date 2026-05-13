package com.example.dobby.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * Configuration class for setting up the Supabase client within a Spring application context.
 *
 * This class initializes and provides a configured instance of the Supabase client,
 * ensuring that the required parameters, such as the Supabase URL and key, are correctly
 * provided through application properties.
 *
 * @constructor Injects the Supabase URL and key from the application properties.
 *
 * @property supabaseUrl The URL of the Supabase instance to connect to. This value is injected
 * from the property `${supabase.url}`.
 * @property supabaseKey The API key used for authenticating with the Supabase instance. This value
 * is injected from the property `${supabase.key}`.
 *
 * @throws IllegalArgumentException If the Supabase URL or key provided is blank or missing.
 */
@Configuration
class SupabaseConfig(
    @Value($$"${supabase.url}") private val supabaseUrl: String,
    @Value($$"${supabase.key}") private val supabaseKey: String
) {
    @Bean
    fun supabaseClient(): SupabaseClient {
        require(supabaseUrl.isNotBlank()) { "Supabase url must not be blank" }
        require(supabaseKey.isNotBlank()) { "Supabase key must not be blank" }

        return createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseKey
        )
        {
            install(Postgrest)
        }
    }
}