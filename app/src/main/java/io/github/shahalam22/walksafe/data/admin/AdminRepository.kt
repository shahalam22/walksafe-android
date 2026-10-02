package io.github.shahalam22.walksafe.data.admin

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.shahalam22.walksafe.data.model.SessionRow
import io.github.shahalam22.walksafe.data.model.SessionStats
import io.github.shahalam22.walksafe.data.model.SpeedPoint
import io.github.shahalam22.walksafe.data.model.UserRow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session data and the user list, read straight from Supabase with the admin's
 * login (so it works while the server is off). Row level security limits this
 * to admins.
 */
@Singleton
class AdminRepository @Inject constructor(private val supabase: SupabaseClient) {

    suspend fun users(): List<UserRow> =
        supabase.from("profiles")
            .select(Columns.raw("id, email, display_name, role, is_active, created_at, sessions(count)")) {
                order("created_at", Order.ASCENDING)
            }
            .decodeList()

    suspend fun sessions(userId: String?): List<SessionRow> =
        supabase.from("sessions")
            .select(Columns.raw("session_id, user_id, started_at, ended_at, total_frames, end_reason")) {
                if (userId != null) filter { eq("user_id", userId) }
                order("started_at", Order.DESCENDING)
                limit(200)
            }
            .decodeList()

    suspend fun session(sessionId: String): SessionRow? =
        supabase.from("sessions")
            .select(Columns.raw("session_id, user_id, started_at, ended_at, total_frames, end_reason")) {
                filter { eq("session_id", sessionId) }
            }
            .decodeSingleOrNull()

    suspend fun stats(sessionId: String? = null, userId: String? = null): SessionStats =
        supabase.postgrest.rpc("session_stats", buildJsonObject {
            if (sessionId != null) put("p_session", sessionId) else put("p_session", JsonNull)
            if (userId != null) put("p_user", userId) else put("p_user", JsonNull)
        }).decodeAs()

    suspend fun speed(sessionId: String, points: Int = 400): List<SpeedPoint> =
        supabase.postgrest.rpc("session_speed", buildJsonObject {
            put("p_session", sessionId)
            put("p_points", points)
        }).decodeList()

    /** Every row of one table for one session, in pages of 1000. */
    suspend fun exportRows(table: String, sessionId: String, onProgress: (Int) -> Unit = {}): List<JsonObject> {
        val rows = mutableListOf<JsonObject>()
        var from = 0L
        while (true) {
            val page = supabase.from(table).select {
                filter { eq("session_id", sessionId) }
                order(if (table == "frames") "frame_idx" else "id", Order.ASCENDING)
                range(from, from + PAGE - 1)
            }.decodeList<JsonObject>()
            rows += page
            onProgress(rows.size)
            if (page.size < PAGE) return rows
            from += PAGE
        }
    }

    private companion object {
        const val PAGE = 1000L
    }
}
