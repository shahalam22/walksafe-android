package io.github.shahalam22.walksafe.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.auth.AuthRepository
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
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    fun signIn() {
        val s = _state.value
        if (s.busy || s.email.isBlank() || s.password.isEmpty()) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val error = auth.signIn(s.email, s.password)
            _state.update { it.copy(busy = false, error = error, password = if (error == null) "" else it.password) }
        }
    }
}
