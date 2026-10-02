package io.github.shahalam22.walksafe.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SignedInUser(val id: String, val email: String, val isAdmin: Boolean)

// ── WalkSafe server ─────────────────────────────────────────────────────────

@Serializable
data class ServerHealth(
    val app: String = "",
    val ready: Boolean = false,
    val busy: Boolean = false,
)

@Serializable
data class FrameResult(
    val command: String = "",
    val phrase: String = "",
    val detail: String = "",
    val view: String? = null,        // base64 JPEG of the analysis panel, when asked for
)

@Serializable
internal data class SessionStarted(@SerialName("session_id") val sessionId: String)

// ── Supabase tables and functions ───────────────────────────────────────────

@Serializable
data class AppConfigRow(
    val key: String,
    val value: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
)

@Serializable
data class CountRow(val count: Long = 0)

@Serializable
data class UserRow(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    val role: String = "blind",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    val sessions: List<CountRow> = emptyList(),
) {
    val name: String get() = displayName?.takeIf { it.isNotBlank() } ?: email.orEmpty()
    val isAdmin: Boolean get() = role == "admin"
    val sessionCount: Long get() = sessions.firstOrNull()?.count ?: 0
}

@Serializable
data class SessionRow(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("total_frames") val totalFrames: Long = 0,
    @SerialName("end_reason") val endReason: String? = null,
)

@Serializable
data class LabelCount(val label: String? = null, val count: Long = 0)

@Serializable
data class SessionStats(
    val sessions: Long = 0,
    @SerialName("total_frames") val totalFrames: Long = 0,
    @SerialName("avg_speed_ms") val avgSpeedMs: Double? = null,
    @SerialName("total_agents") val totalAgents: Long = 0,
    @SerialName("critical_count") val criticalCount: Long = 0,
    val commands: List<LabelCount> = emptyList(),
    val actions: List<LabelCount> = emptyList(),
    val risks: List<LabelCount> = emptyList(),
)

@Serializable
data class SpeedPoint(
    @SerialName("frame_idx") val frameIdx: Int,
    @SerialName("ego_speed_ms") val egoSpeedMs: Double? = null,
    val action: String? = null,
)
