package com.example.dobby.supabase

import com.example.dobby.dto.roast.RoastLogDb
import com.example.dobby.dto.roast.RoastLogDbResponse
import com.example.dobby.dto.roast.RoastResult
import com.example.dobby.dto.roast.RoastTargetDb
import com.example.dobby.exception.DobbyException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import org.springframework.stereotype.Component


private const val ROAST_LOGS_TABLE = "roasts"
private const val ROAST_TARGETS_TABLE = "roast_targets"

@Component
class SupabaseRoast(
    private val supabaseClient: SupabaseClient
) {

    suspend fun saveRoastResult(guildId: String, channelId: String, result: RoastResult) {
        return safeDbCall("save roast log and targets for guild $guildId") {

            val logDto = RoastLogDb(
                guildId = guildId,
                channelId = channelId,
                roastText = result.text,
                personaUsed = result.persona,
                primaryTargetId = result.primaryTargetId,
                clappedTheMostId = result.clappedTheMostId,
                burnAccuracy = result.burnAccuracy,
                severityScore = result.severityScore
            )

            val insertedLog = supabaseClient.from(ROAST_LOGS_TABLE)
                .insert(logDto) {
                    select()
                }
                .decodeSingle<RoastLogDbResponse>()

            val generatedRoastId = insertedLog.id
                ?: throw DobbyException.DatabaseException("Failed to retrieve generated ID from inserted roast log.")

            // Map target with BOTH roastId and guildId to fulfill the foreign key constraint
            val targets = result.targets.map { target ->
                RoastTargetDb(
                    roastId = generatedRoastId,
                    discordUserId = target.userId,
                    guildId = guildId,
                    reason = target.reason
                )
            }

            if (targets.isNotEmpty()) {
                supabaseClient.from(ROAST_TARGETS_TABLE)
                    .insert(targets)
            }
        }
    }

    suspend fun getAllRoastsByGuildId(guildId: String): List<RoastLogDbResponse> {
        return safeDbCall("fetch all roasts and nested targets for guild $guildId") {
            supabaseClient.from(ROAST_LOGS_TABLE)
                .select(columns = Columns.raw("*, roast_targets(*, user_profiles(*))")) {
                    filter {
                        eq("guild_id", guildId)
                    }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList<RoastLogDbResponse>()
        }
    }
}