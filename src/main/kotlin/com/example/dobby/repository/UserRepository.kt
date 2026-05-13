package com.example.dobby.repository

import com.example.dobby.model.PersonCreateRequest
import com.example.dobby.model.PersonResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.springframework.stereotype.Repository


@Repository
class UserRepository(
    private val supabaseClient: SupabaseClient
) {

    private val table = "person"

    suspend fun getUsers(): List<PersonResponse> {
        val response = supabaseClient
            .from(table)
            .select()
        return response.decodeList<PersonResponse>()
    }

    suspend fun insertUser(person: PersonCreateRequest): PersonResponse {
        val response = supabaseClient.from(table).insert(person) { select() }
        return response.decodeSingle<PersonResponse>()
    }
}