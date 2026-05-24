package com.example.dobby.repository

import com.example.dobby.db.SupabaseUserFactClient
import com.example.dobby.dto.UserFactCreateRequest
import com.example.dobby.dto.UserFactResponse
import org.springframework.stereotype.Repository

@Repository
class UserFactRepository(
    private val supabaseUserFactClient: SupabaseUserFactClient
) {
    suspend fun saveFact(fact: UserFactCreateRequest): UserFactResponse {
        return supabaseUserFactClient.insertNewFact(fact)
    }
}