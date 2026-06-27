package com.example.dobby.repository

import com.example.dobby.dto.fact.UserFactCreateRequest
import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.supabase.SupabaseUserFact
import org.springframework.stereotype.Repository

@Repository
class UserFactRepository(
    private val supabaseUserFact: SupabaseUserFact,
) {
    suspend fun saveFact(fact: UserFactCreateRequest): UserFactResponse = supabaseUserFact.insertNewFact(fact)

    suspend fun deleteFactById(factId: String) {
        supabaseUserFact.deleteFactById(factId)
    }

    suspend fun deleteFactsByProfileId(profileId: String) {
        supabaseUserFact.deleteFactsByProfileId(profileId)
    }
}
