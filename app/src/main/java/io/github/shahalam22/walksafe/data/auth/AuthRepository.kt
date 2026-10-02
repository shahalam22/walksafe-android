package io.github.shahalam22.walksafe.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.shahalam22.walksafe.data.model.SignedInUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: SignedInUser) : AuthState
}

/**
 * Sign-in through Supabase Auth. The session is saved on the phone and refreshed
 * automatically, so a phone that was set up once stays signed in.
 */
@Singleton
class AuthRepository @Inject constructor(private val supabase: SupabaseClient) {

    val state: Flow<AuthState> = supabase.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthState.SignedIn(status.session.toUser())
                is SessionStatus.NotAuthenticated -> AuthState.SignedOut
                // Initializing, or a refresh failed (e.g. offline): keep any saved session.
                else -> supabase.auth.currentSessionOrNull()?.let { AuthState.SignedIn(it.toUser()) }
                    ?: AuthState.Loading
            }
        }
        .distinctUntilChanged()

    val currentUser: SignedInUser?
        get() = supabase.auth.currentSessionOrNull()?.toUser()

    /** Signs in; returns an error message for the user, or null on success. */
    suspend fun signIn(email: String, password: String): String? = try {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        null
    } catch (e: RestException) {
        if (e.message.orEmpty().contains("invalid", ignoreCase = true)) "Wrong email or password."
        else e.error
    } catch (e: HttpRequestException) {
        "No internet connection."
    }

    suspend fun signOut() {
        runCatching { supabase.auth.signOut() }
    }

    fun accessToken(): String? = supabase.auth.currentAccessTokenOrNull()

    suspend fun refresh() {
        runCatching { supabase.auth.refreshCurrentSession() }
    }

    private fun UserSession.toUser(): SignedInUser {
        val info = user
        val role = info?.appMetadata?.get("role")?.jsonPrimitive?.contentOrNull
        return SignedInUser(id = info?.id.orEmpty(), email = info?.email.orEmpty(), isAdmin = role == "admin")
    }
}
