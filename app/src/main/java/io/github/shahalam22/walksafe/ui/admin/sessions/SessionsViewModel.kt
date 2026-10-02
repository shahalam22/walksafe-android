package io.github.shahalam22.walksafe.ui.admin.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.admin.AdminRepository
import io.github.shahalam22.walksafe.data.model.SessionRow
import io.github.shahalam22.walksafe.data.model.SessionStats
import io.github.shahalam22.walksafe.data.model.UserRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val users: List<UserRow> = emptyList(),
    val userId: String? = null,           // null: all users
    val stats: SessionStats? = null,
    val sessions: List<SessionRow> = emptyList(),
) {
    fun userName(id: String?): String = users.firstOrNull { it.id == id }?.name ?: "Deleted user"
}

@HiltViewModel
class SessionsViewModel @Inject constructor(private val repo: AdminRepository) : ViewModel() {

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun selectUser(userId: String?) {
        _state.update { it.copy(userId = userId) }
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val userId = _state.value.userId
                val users = async { repo.users() }
                val stats = async { repo.stats(userId = userId) }
                val sessions = async { repo.sessions(userId) }
                _state.update {
                    it.copy(loading = false, users = users.await(), stats = stats.await(), sessions = sessions.await())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not load the sessions.") }
            }
        }
    }
}
