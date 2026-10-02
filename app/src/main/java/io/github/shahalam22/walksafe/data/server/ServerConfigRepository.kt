package io.github.shahalam22.walksafe.data.server

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.shahalam22.walksafe.data.auth.AuthRepository
import io.github.shahalam22.walksafe.data.model.AppConfigRow
import io.github.shahalam22.walksafe.data.prefs.UserPrefsRepository
import kotlinx.coroutines.CancellationException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class ServerAddress(val url: String, val updatedAt: String?, val fromCache: Boolean)

/**
 * The WalkSafe server's address. The Colab notebook gets a new one every run;
 * the admin saves it in Supabase and every phone reads it from there.
 */
@Singleton
class ServerConfigRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val auth: AuthRepository,
    private val prefs: UserPrefsRepository,
) {

    /** Reads it fresh, falling back to the last address seen on this phone. */
    suspend fun load(): ServerAddress = try {
        val row = supabase.from(TABLE).select {
            filter { eq("key", KEY) }
        }.decodeSingleOrNull<AppConfigRow>()
        val url = cleanServerUrl(row?.value.orEmpty())
        prefs.setCachedServerUrl(url)
        ServerAddress(url, row?.updatedAt, fromCache = false)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ServerAddress(prefs.cachedServerUrl(), null, fromCache = true)
    }

    suspend fun save(url: String): String {
        val value = cleanServerUrl(url)
        supabase.from(TABLE).upsert(
            AppConfigRow(KEY, value, updatedAt = Instant.now().toString(), updatedBy = auth.currentUser?.id),
        )
        prefs.setCachedServerUrl(value)
        return value
    }

    private companion object {
        const val TABLE = "app_config"
        const val KEY = "server_url"
    }
}
