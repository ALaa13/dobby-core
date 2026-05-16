package com.example.dobby.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import org.springframework.stereotype.Repository

@Repository
class UserRepository(
    private val supabaseClient: SupabaseClient
) {
}