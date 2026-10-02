package io.github.shahalam22.walksafe.data.server

import io.github.shahalam22.walksafe.data.auth.AuthRepository
import io.github.shahalam22.walksafe.data.model.FrameResult
import io.github.shahalam22.walksafe.data.model.ServerHealth
import io.github.shahalam22.walksafe.data.model.SessionStarted
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Calls to the WalkSafe server on Colab. Every call except health() sends the login token. */
@Singleton
class WalkSafeApi @Inject constructor(
    private val http: HttpClient,
    private val json: Json,
    private val auth: AuthRepository,
) {

    suspend fun health(base: String): ServerHealth {
        val h = json.decodeFromString<ServerHealth>(call(base, HttpMethod.Get, "/api/health", auth = false, timeoutMs = 8_000))
        if (h.app != "walksafe") throw ApiException(0, "not_walksafe")
        return h
    }

    suspend fun startSession(base: String): String =
        json.decodeFromString<SessionStarted>(call(base, HttpMethod.Post, "/api/session/start", body = "{}")).sessionId

    suspend fun sendFrame(base: String, sessionId: String, jpeg: ByteArray, view: Boolean, timeoutMs: Long): FrameResult =
        json.decodeFromString(
            call(base, HttpMethod.Post, "/api/frame?session_id=$sessionId&view=${if (view) 1 else 0}",
                bytes = jpeg, timeoutMs = timeoutMs),
        )

    suspend fun stopSession(base: String, sessionId: String) {
        call(base, HttpMethod.Post, "/api/session/stop",
            body = buildJsonObject { put("session_id", sessionId) }.toString(), timeoutMs = 5_000)
    }

    // ── Admin: blind-user accounts (the server holds the Supabase secret key) ─

    suspend fun createUser(base: String, email: String, password: String, displayName: String) {
        call(base, HttpMethod.Post, "/api/admin/users", body = buildJsonObject {
            put("email", email)
            put("password", password)
            put("display_name", displayName)
        }.toString())
    }

    suspend fun changeUser(base: String, userId: String, change: JsonObject) {
        call(base, HttpMethod.Patch, "/api/admin/users/$userId", body = change.toString())
    }

    suspend fun deleteUser(base: String, userId: String) {
        call(base, HttpMethod.Delete, "/api/admin/users/$userId")
    }

    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun call(
        base: String,
        method: HttpMethod,
        path: String,
        auth: Boolean = true,
        body: String? = null,
        bytes: ByteArray? = null,
        timeoutMs: Long = 20_000,
    ): String {
        if (base.isBlank()) throw ApiException(0, ApiException.NO_ADDRESS)
        val token = if (auth) this.auth.accessToken() ?: throw ApiException(401, "login_required") else null
        val response = try {
            http.request("$base$path") {
                this.method = method
                timeout {
                    requestTimeoutMillis = timeoutMs
                    connectTimeoutMillis = 10_000
                }
                token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                setPayload(body, bytes)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            throw ApiException(0, ApiException.TIMEOUT)
        } catch (e: Exception) {
            val timedOut = e.javaClass.simpleName.contains("Timeout")
            throw ApiException(0, if (timedOut) ApiException.TIMEOUT else ApiException.NETWORK)
        }
        val text = response.bodyAsText()
        if (response.status.value !in 200..299) {
            throw ApiException(response.status.value, errorDetail(text) ?: response.status.description)
        }
        return text
    }

    private fun HttpRequestBuilder.setPayload(body: String?, bytes: ByteArray?) {
        when {
            bytes != null -> setBody(ByteArrayContent(bytes, ContentType.Image.JPEG))
            body != null -> setBody(TextContent(body, ContentType.Application.Json))
        }
    }

    /** FastAPI errors look like {"detail": "reason"}. */
    private fun errorDetail(text: String): String? = runCatching {
        (json.parseToJsonElement(text).jsonObject["detail"] as? JsonPrimitive)?.content
    }.getOrNull()
}
