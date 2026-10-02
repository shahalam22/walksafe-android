package io.github.shahalam22.walksafe.ui.admin.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.shahalam22.walksafe.data.admin.AdminRepository
import io.github.shahalam22.walksafe.data.admin.CsvExporter
import io.github.shahalam22.walksafe.data.admin.toCsv
import io.github.shahalam22.walksafe.data.model.SessionRow
import io.github.shahalam22.walksafe.data.model.SessionStats
import io.github.shahalam22.walksafe.data.model.SpeedPoint
import io.github.shahalam22.walksafe.ui.admin.SessionDetailRoute
import io.github.shahalam22.walksafe.ui.components.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val session: SessionRow? = null,
    val userName: String = "",
    val stats: SessionStats? = null,
    val speed: List<SpeedPoint> = emptyList(),
    val exporting: String? = null,          // the table being exported, with progress
    val message: Message? = null,
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: AdminRepository,
    private val exporter: CsvExporter,
) : ViewModel() {

    private val sessionId = savedState.toRoute<SessionDetailRoute>().sessionId

    private val _state = MutableStateFlow(SessionDetailUiState())
    val state: StateFlow<SessionDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val session = async { repo.session(sessionId) }
                val users = async { repo.users() }
                val stats = async { repo.stats(sessionId = sessionId) }
                val speed = async { repo.speed(sessionId) }
                val row = session.await()
                _state.update {
                    it.copy(
                        loading = false,
                        session = row,
                        userName = users.await().firstOrNull { u -> u.id == row?.userId }?.name ?: "Deleted user",
                        stats = stats.await(),
                        speed = speed.await(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Could not load the session.") }
            }
        }
    }

    /** Saves one table of this session as CSV in Downloads. */
    fun export(table: String) {
        if (_state.value.exporting != null) return
        _state.update { it.copy(exporting = table, message = null) }
        viewModelScope.launch {
            try {
                val rows = repo.exportRows(table, sessionId) { n -> _state.update { it.copy(exporting = "$table ($n)") } }
                val name = "walksafe-${sessionId.take(8)}-$table.csv"
                exporter.saveToDownloads(name, toCsv(rows))
                _state.update { it.copy(message = Message("Saved $name in Downloads (${rows.size} rows).", ok = true)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(message = Message("Export failed: ${e.message}", ok = false)) }
            } finally {
                _state.update { it.copy(exporting = null) }
            }
        }
    }
}
