package io.github.shahalam22.walksafe.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.auth.AuthRepository
import io.github.shahalam22.walksafe.data.auth.ResetStage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResetPasswordUiState(
    val password: String = "",
    val confirm: String = "",
    val busy: Boolean = false,
    val error: String? = null,
)

/** The new-password screen, opened by the reset link in the email. */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ResetPasswordUiState())
    val state: StateFlow<ResetPasswordUiState> = _state.asStateFlow()

    val stage: StateFlow<ResetStage?> = auth.reset

    /** The account the link is for, once it has been checked. */
    val email: String? get() = auth.currentUser?.email?.takeIf { it.isNotBlank() }

    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun onConfirm(value: String) = _state.update { it.copy(confirm = value, error = null) }

    fun save() {
        val s = _state.value
        if (s.busy || stage.value != ResetStage.READY) return
        val problem = when {
            s.password.length < 6 -> "The password needs at least 6 characters."
            s.password != s.confirm -> "The two passwords do not match."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            // On success this screen closes and the sign-in screen says so.
            val error = auth.setNewPassword(s.password)
            _state.update { it.copy(busy = false, error = error) }
        }
    }

    fun cancel() {
        viewModelScope.launch { auth.cancelReset() }
    }
}
