package com.example.dobby.db

import com.example.dobby.dto.UserFactCreateRequest
import com.example.dobby.dto.UserFactResponse
import com.example.dobby.util.Logging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.springframework.stereotype.Component

private const val USER_FACTS_TABLE = "user_facts"

@Component
class SupabaseUserFactClient(
    private val supabaseClient: SupabaseClient
) {
    suspend fun insertNewFact(fact: UserFactCreateRequest): UserFactResponse {
        try {
            return supabaseClient.from(USER_FACTS_TABLE)
                .insert(fact) {
                    select()
                }
                .decodeSingle<UserFactResponse>()
        } catch (e: Exception) {
            Logging.logError("Error inserting user fact: ${e.message}")
            throw e
        }
    }
}