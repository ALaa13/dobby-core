package com.example.dobby.supabase

import com.example.dobby.dto.fact.UserFactCreateRequest
import com.example.dobby.dto.fact.UserFactResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.springframework.stereotype.Component

private const val USER_FACTS_TABLE = "user_facts"

@Component
class SupabaseUserFact(
    private val supabaseClient: SupabaseClient,
) {
    suspend fun insertNewFact(fact: UserFactCreateRequest): UserFactResponse =
        safeDbCall("Inserting new user fact") {
            supabaseClient
                .from(USER_FACTS_TABLE)
                .insert(fact) {
                    select()
                }.decodeSingle<UserFactResponse>()
        }

    suspend fun deleteFactById(factId: String) {
        safeDbCall("Deleting user fact") {
            supabaseClient
                .from(USER_FACTS_TABLE)
                .delete {
                    filter {
                        eq("id", factId)
                    }
                }
        }
    }

    suspend fun deleteFactsByProfileId(profileId: String) {
        safeDbCall("Deleting user fact") {
            supabaseClient
                .from(USER_FACTS_TABLE)
                .delete {
                    filter {
                        eq("profile_id", profileId)
                    }
                }
        }
    }
}
