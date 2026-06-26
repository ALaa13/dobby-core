package com.example.dobby.repository

import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.supabase.SupabaseRoast
import org.springframework.stereotype.Repository

@Repository
class RoastRepository(
    private val supabaseRoast: SupabaseRoast
) {

    suspend fun saveRoastResult(guildId: String, channelId: String, result: RoastResult) {
        return supabaseRoast.saveRoastResult(guildId, channelId, result)
    }

    suspend fun getGuildRoasts(guildId: String): List<RoastLogDbResponse> {
        return supabaseRoast.getAllRoastsByGuildId(guildId)
    }
}