package io.github.shahalam22.walksafe.ui.admin.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.admin.AdminRepository
import io.github.shahalam22.walksafe.data.model.UserRow
import io.github.shahalam22.walksafe.data.server.ApiException
import io.github.shahalam22.walksafe.data.server.ServerConfigRepository
import io.github.shahalam22.walksafe.data.server.WalkSafeApi
import io.github.shahalam22.walksafe.ui.components.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

data class UsersUiState(
    val users: List<UserRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val adding: Boolean = false,
    val formMessage: Message? = null,
    val actionMessage: Message? = null,
)

/**
 * Blind-user accounts. The list comes from Supabase; adding, changing and
 * deleting go through the WalkSafe server, which holds the secret key.
 */
@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repo: AdminRepository,
    private val api: WalkSafeApi,
    private val serverConfig: ServerConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(UsersUiState())
    val state: StateFlow<UsersUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun onName(v: String) = _state.update { it.copy(name = v, formMessage = null) }
    fun onEmail(v: String) = _state.update { it.copy(email = v, formMessage = null) }
    fun onPassword(v: String) = _state.update { it.copy(password = v, formMessage = null) }

    fun reload() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(users = repo.users(), loading = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not load the users.") }
            }
        }
    }

    fun addUser() {
        val s = _state.value
        if (s.adding) return
        if (s.email.isBlank() || s.password.length < 6) {
            _state.update { it.copy(formMessage = Message("Enter an email and a password of at least 6 characters.", ok = false)) }
            return
        }
        _state.update { it.copy(adding = true, formMessage = Message("Adding…", ok = true)) }
        viewModelScope.launch {
            val result = runServer { api.createUser(it, s.email.trim(), s.password, s.name.trim()) }
            _state.update {
                if (result == null) {
                    it.copy(adding = false, name = "", email = "", password = "",
                        formMessage = Message("Added ${s.email.trim()}. Sign in with it on their phone.", ok = true))
                } else {
                    it.copy(adding = false, formMessage = Message(result, ok = false))
                }
            }
            if (result == null) reload()
        }
    }

    fun setPassword(user: UserRow, password: String) = change(user, "Password changed for ${user.name}.") {
        api.changeUser(it, user.id, buildJsonObject { put("password", password) })
    }

    fun toggleActive(user: UserRow) = change(user, if (user.isActive) "${user.name} turned off." else "${user.name} turned on.") {
        api.changeUser(it, user.id, buildJsonObject { put("active", !user.isActive) })
    }

    fun delete(user: UserRow) = change(user, "${user.name} deleted. Their sessions are kept.") {
        api.deleteUser(it, user.id)
    }

    private fun change(user: UserRow, done: String, call: suspend (String) -> Unit) {
        _state.update { it.copy(actionMessage = null) }
        viewModelScope.launch {
            val error = runServer(call)
            _state.update { it.copy(actionMessage = Message(error ?: done, ok = error == null)) }
            if (error == null) reload()
        }
    }

    /** Runs a server call with the saved address; returns an error message or null. */
    private suspend fun runServer(call: suspend (String) -> Unit): String? = try {
        call(serverConfig.load().url)
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        e.userMessage
    }
}
