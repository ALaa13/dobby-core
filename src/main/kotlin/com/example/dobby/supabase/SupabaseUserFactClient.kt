package com.example.dobby.supabase

import com.example.dobby.dto.UserFactCreateRequest
import com.example.dobby.dto.UserFactResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.springframework.stereotype.Component

private const val USER_FACTS_TABLE = "user_facts"

@Component
class SupabaseUserFactClient(
    private val supabaseClient: SupabaseClient
) {
    suspend fun insertNewFact(fact: UserFactCreateRequest): UserFactResponse {
        return safeDbCall("Inserting new user fact") {
            supabaseClient.from(USER_FACTS_TABLE)
                .insert(fact) {
                    select()
                }
                .decodeSingle<UserFactResponse>()
        }
    }

    suspend fun deleteFactsByProfileId(profileId: String) {
        safeDbCall("Deleting user fact") {
            supabaseClient.from(USER_FACTS_TABLE)
                .delete {
                    filter {
                        eq("profile_id", profileId)
                    }
                }
        }
    }
}