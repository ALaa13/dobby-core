package com.example.dobby.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

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