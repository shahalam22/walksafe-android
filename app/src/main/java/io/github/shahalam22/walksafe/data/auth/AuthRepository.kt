package io.github.shahalam22.walksafe.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.parseSessionFromFragment
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.shahalam22.walksafe.data.model.SignedInUser
import io.github.shahalam22.walksafe.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: SignedInUser) : AuthState
}

/** Where a password reset from the email link is. */
enum class ResetStage {
    CHECKING,   // the link opened the app; its login is being checked
    READY,      // signed in with the link: the new password can be saved
}

/** A line for the sign-in screen, e.g. after a password reset. */
data class AuthNotice(val text: String, val ok: Boolean)

/**
 * Sign-in through Supabase Auth. The session is saved on the phone and refreshed
 * automatically, so a phone that was set up once stays signed in.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationScope private val scope: CoroutineScope,
) {

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

    private val _reset = MutableStateFlow<ResetStage?>(null)

    /** Not null while the new-password screen is open. */
    val reset: StateFlow<ResetStage?> = _reset.asStateFlow()

    private val _notice = MutableStateFlow<AuthNotice?>(null)
    val notice: StateFlow<AuthNotice?> = _notice.asStateFlow()

    val currentUser: SignedInUser?
        get() = supabase.auth.currentSessionOrNull()?.toUser()

    /** Signs in; returns an error message for the user, or null on success. */
    suspend fun signIn(email: String, password: String): String? = try {
        _notice.value = null
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        null
    } catch (e: RestException) {
        if (e.message.orEmpty().contains("invalid", ignoreCase = true)) WRONG_CREDENTIALS
        else e.error
    } catch (e: HttpRequestException) {
        "No internet connection."
    }

    suspend fun signOut() {
        runCatching { supabase.auth.signOut() }
    }

    // ── Password reset ────────────────────────────────────────────────────────

    /** Emails a reset link that opens this app; returns an error message or null. */
    suspend fun sendPasswordReset(email: String): String? = try {
        supabase.auth.resetPasswordForEmail(email.trim(), redirectUrl = RESET_LINK)
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        e.userText()
    } catch (e: HttpRequestException) {
        "No internet connection."
    }

    /**
     * The reset link from the email opened the app:
     * walksafe://reset-password#access_token=…&refresh_token=…&type=recovery,
     * or #error=…&error_description=… when the link has expired.
     */
    fun openResetLink(link: String) {
        if (!link.startsWith(RESET_LINK)) return
        val fragment = link.substringAfter('#', "")
        val parts = fragment.split('&').mapNotNull {
            it.split('=', limit = 2).takeIf { pair -> pair.size == 2 }?.let { (k, v) -> k to v }
        }.toMap()
        if (parts["type"] != "recovery" || parts["access_token"] == null) {
            _notice.value = AuthNotice(LINK_EXPIRED, ok = false)
            return
        }
        _notice.value = null
        _reset.value = ResetStage.CHECKING
        scope.launch {
            try {
                val session = supabase.auth.parseSessionFromFragment(fragment)
                val user = supabase.auth.retrieveUser(session.accessToken)
                supabase.auth.importSession(session.copy(user = user))
                _reset.value = ResetStage.READY
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _reset.value = null
                _notice.value = AuthNotice(LINK_EXPIRED, ok = false)
            }
        }
    }

    /** Saves the new password, then signs out so the user signs in with it. */
    suspend fun setNewPassword(password: String): String? = try {
        supabase.auth.updateUser { this.password = password }
        finishReset(AuthNotice("Password changed. Sign in with your new password.", ok = true))
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        if (e is AuthSessionMissingException || supabase.auth.currentSessionOrNull() == null) {
            finishReset(AuthNotice(LINK_EXPIRED, ok = false))
            null
        } else {
            e.userText()
        }
    } catch (e: HttpRequestException) {
        "No internet connection."
    }

    suspend fun cancelReset() = finishReset(null)

    // Sign out first, so the reset login never shows the guidance or admin screen.
    private suspend fun finishReset(notice: AuthNotice?) {
        signOut()
        _notice.value = notice
        _reset.value = null
    }

    // ─────────────────────────────────────────────────────────────────────────

    fun accessToken(): String? = supabase.auth.currentAccessTokenOrNull()

    /** Refreshes the token; if the session is missing from memory, reloads it from the phone. */
    suspend fun refresh() {
        runCatching {
            if (supabase.auth.currentSessionOrNull() == null) supabase.auth.loadFromStorage()
            else supabase.auth.refreshCurrentSession()
        }
    }

    private fun UserSession.toUser(): SignedInUser {
        val info = user
        val role = info?.appMetadata?.get("role")?.jsonPrimitive?.contentOrNull
        return SignedInUser(id = info?.id.orEmpty(), email = info?.email.orEmpty(), isAdmin = role == "admin")
    }

    /** Auth errors carry a readable message; others only a short code. */
    private fun RestException.userText(): String = when {
        statusCode == 429 -> "Too many tries. Wait a few minutes and try again."
        this is AuthRestException -> message ?: error
        else -> error
    }

    companion object {
        const val WRONG_CREDENTIALS = "Wrong credentials."
        const val LINK_EXPIRED = "This reset link has expired or was already used. Ask for a new one."

        /** Where the reset email's link sends the user. Must be in Supabase's allowed redirect URLs. */
        const val RESET_LINK = "walksafe://reset-password"
    }
}
