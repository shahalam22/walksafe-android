package io.github.shahalam22.walksafe.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.auth.AuthNotice
import io.github.shahalam22.walksafe.data.auth.AuthRepository
import io.github.shahalam22.walksafe.ui.components.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val showForgot: Boolean = false,         // after a wrong email or password
    // Forgot password: email a reset link.
    val forgotOpen: Boolean = false,
    val resetEmail: String = "",
    val sending: Boolean = false,
    val resetMessage: Message? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** e.g. "Password changed" after a reset, or an expired reset link. */
    val notice: StateFlow<AuthNotice?> = auth.notice

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val s = _state.value
        if (s.busy || s.email.isBlank() || s.password.isEmpty()) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val error = auth.signIn(s.email, s.password)
            _state.update {
                it.copy(
                    busy = false,
                    error = error,
                    password = if (error == null) "" else it.password,
                    showForgot = it.showForgot || error == AuthRepository.WRONG_CREDENTIALS,
                )
            }
        }
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    fun openForgot() = _state.update {
        it.copy(forgotOpen = true, resetEmail = it.email.trim(), resetMessage = null)
    }

    fun closeForgot() = _state.update { it.copy(forgotOpen = false) }

    fun onResetEmail(value: String) = _state.update { it.copy(resetEmail = value, resetMessage = null) }

    fun sendResetLink() {
        val s = _state.value
        val email = s.resetEmail.trim()
        if (s.sending) return
        if (!email.contains('@')) {
            _state.update { it.copy(resetMessage = Message("Enter the account's email.", ok = false)) }
            return
        }
        _state.update { it.copy(sending = true, resetMessage = null) }
        viewModelScope.launch {
            val error = auth.sendPasswordReset(email)
            _state.update {
                it.copy(
                    sending = false,
                    resetMessage = if (error == null) {
                        Message("Link has been sent.", ok = true)
                    } else {
                        Message(error, ok = false)
                    },
                )
            }
        }
    }
}
