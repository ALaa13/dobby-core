package com.example.dobby.service

import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.user.UserProfileResponse
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val factService: FactService,
    private val roastService: RoastService,
) {

    suspend fun getAllFactsByGuildId(guildId: String): List<UserProfileResponse> {
        return factService.getAllFactsByGuild(guildId)
    }

    suspend fun getAllRoastByGuildId(guildId: String): List<RoastLogDbResponse> {
        return roastService.getGuildRoasts(guildId)
    }

    suspend fun deleteFact(factId: String) {
        factService.deleteUserFact(factId)
    }

    suspend fun purgeGuildFacts(guildId: String) {
        factService.resetGuildFacts(guildId)
    }
}