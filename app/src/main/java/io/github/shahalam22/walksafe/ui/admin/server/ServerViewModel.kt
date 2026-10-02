package io.github.shahalam22.walksafe.ui.admin.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.server.ApiException
import io.github.shahalam22.walksafe.data.server.ServerConfigRepository
import io.github.shahalam22.walksafe.data.server.WalkSafeApi
import io.github.shahalam22.walksafe.data.server.cleanServerUrl
import io.github.shahalam22.walksafe.ui.components.Message
import io.github.shahalam22.walksafe.ui.util.formatDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class ServerUiState(
    val url: String = "",
    val saved: String = "",
    val busy: Boolean = false,
    val message: Message? = null,
)

/** The address the Colab notebook printed; every phone reads it from Supabase. */
@HiltViewModel
class ServerViewModel @Inject constructor(
    private val config: ServerConfigRepository,
    private val api: WalkSafeApi,
) : ViewModel() {

    private val _state = MutableStateFlow(ServerUiState())
    val state: StateFlow<ServerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val a = config.load()
            _state.update {
                it.copy(
                    url = a.url,
                    saved = if (a.url.isNotEmpty()) "Saved address, updated ${formatDateTime(a.updatedAt)}." else "No address saved yet.",
                )
            }
        }
    }

    fun onUrl(value: String) = _state.update { it.copy(url = value, message = null) }

    fun test() {
        _state.update { it.copy(busy = true, message = Message("Testing…", ok = true)) }
        viewModelScope.launch {
            val msg = try {
                val h = api.health(cleanServerUrl(_state.value.url))
                Message(if (h.ready) "Server is running and ready." else "Server answers but is still loading.", ok = true)
            } catch (e: ApiException) {
                Message(e.userMessage, ok = false)
            }
            _state.update { it.copy(busy = false, message = msg) }
        }
    }

    fun save() {
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val url = config.save(_state.value.url)
                _state.update {
                    it.copy(
                        busy = false,
                        url = url,
                        message = Message("Saved. Phones use it from their next start.", ok = true),
                        saved = "Saved address, updated ${formatDateTime(Instant.now().toString())}.",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, message = Message("Could not save: ${e.message}", ok = false)) }
            }
        }
    }
}
