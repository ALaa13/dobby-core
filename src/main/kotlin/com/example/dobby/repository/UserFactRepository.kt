package com.example.dobby.repository

import com.example.dobby.dto.fact.UserFactCreateRequest
import com.example.dobby.dto.fact.UserFactResponse
import com.example.dobby.supabase.SupabaseUserFactClient
import org.springframework.stereotype.Repository

@Repository
class UserFactRepository(
    private val supabaseUserFactClient: SupabaseUserFactClient
) {
    suspend fun saveFact(fact: UserFactCreateRequest): UserFactResponse {
        return supabaseUserFactClient.insertNewFact(fact)
    }

    suspend fun deleteFactById(factId: String) {
        supabaseUserFactClient.deleteFactById(factId)
    }

    suspend fun deleteFactsByProfileId(profileId: String) {
        supabaseUserFactClient.deleteFactsByProfileId(profileId)
    }
}