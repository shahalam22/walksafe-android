package io.github.shahalam22.walksafe.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Settings kept on this phone. */
data class UserPrefs(
    val speechRate: Float = 1f,
    val repeatSeconds: Int = 0,          // 0: speak an instruction only when it changes
    val showView: Boolean = false,       // camera and analysis on screen, for demos
    val adminOnGuide: Boolean = false,   // an admin chose the guidance screen
)

@Singleton
class UserPrefsRepository @Inject constructor(private val store: DataStore<Preferences>) {

    val prefs: Flow<UserPrefs> = store.data.map { p ->
        UserPrefs(
            speechRate = p[RATE] ?: 1f,
            repeatSeconds = p[REPEAT] ?: 0,
            showView = p[VIEW] ?: false,
            adminOnGuide = p[ADMIN_ON_GUIDE] ?: false,
        )
    }

    suspend fun current(): UserPrefs = prefs.first()

    suspend fun setSpeechRate(rate: Float) = store.edit { it[RATE] = rate }
    suspend fun setRepeatSeconds(seconds: Int) = store.edit { it[REPEAT] = seconds }
    suspend fun setShowView(show: Boolean) = store.edit { it[VIEW] = show }
    suspend fun setAdminOnGuide(onGuide: Boolean) = store.edit { it[ADMIN_ON_GUIDE] = onGuide }

    suspend fun cachedServerUrl(): String = store.data.first()[SERVER_URL].orEmpty()
    suspend fun setCachedServerUrl(url: String) = store.edit { it[SERVER_URL] = url }

    private companion object {
        val RATE = floatPreferencesKey("speech_rate")
        val REPEAT = intPreferencesKey("repeat_seconds")
        val VIEW = booleanPreferencesKey("show_view")
        val ADMIN_ON_GUIDE = booleanPreferencesKey("admin_on_guide")
        val SERVER_URL = stringPreferencesKey("server_url")
    }
}
