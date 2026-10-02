package io.github.shahalam22.walksafe.ui.admin.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.data.model.SessionRow
import io.github.shahalam22.walksafe.data.model.SessionStats
import io.github.shahalam22.walksafe.ui.components.BarChart
import io.github.shahalam22.walksafe.ui.components.EmptyText
import io.github.shahalam22.walksafe.ui.components.SectionCard
import io.github.shahalam22.walksafe.ui.components.StatTile
import io.github.shahalam22.walksafe.ui.util.formatDateTime
import io.github.shahalam22.walksafe.ui.util.formatDuration
import io.github.shahalam22.walksafe.ui.util.formatNumber
import io.github.shahalam22.walksafe.ui.util.pretty

@Composable
fun SessionsScreen(onOpenSession: (String) -> Unit, viewModel: SessionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserPicker(state, viewModel::selectUser, Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::refresh) { Text("Refresh") }
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        state.stats?.let { s ->
            item {
                StatTiles(
                    listOf(
                        "Sessions" to formatNumber(s.sessions),
                        "Frames" to formatNumber(s.totalFrames),
                        "Average speed" to (s.avgSpeedMs?.let { "$it m/s" } ?: "—"),
                        "Agent encounters" to formatNumber(s.totalAgents),
                        "Critical events" to formatNumber(s.criticalCount),
                    ),
                )
            }
            item { StatsCharts(s) }
        }
        item {
            Text("Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (state.sessions.isEmpty() && !state.loading) item { EmptyText("No sessions yet") }
        items(state.sessions, key = { it.sessionId }) { row ->
            SessionItem(row, state.userName(row.userId)) { onOpenSession(row.sessionId) }
        }
    }
}

@Composable
private fun UserPicker(state: SessionsUiState, onPick: (String?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val current = state.users.firstOrNull { it.id == state.userId }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, Modifier.fillMaxWidth()) {
            Text("User: " + (current?.let { "${it.name} (${it.role})" } ?: "All users"))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("All users") }, onClick = { open = false; onPick(null) })
            state.users.forEach { u ->
                DropdownMenuItem(text = { Text("${u.name} (${u.role})") }, onClick = { open = false; onPick(u.id) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatTiles(tiles: List<Pair<String, String>>) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 2,
    ) {
        tiles.forEach { (label, value) -> StatTile(label, value, Modifier.weight(1f).widthIn(min = 140.dp)) }
    }
}

@Composable
fun StatsCharts(s: SessionStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard("Navigation commands") { BarChart(s.commands) }
        SectionCard("Risk levels") { BarChart(s.risks, risk = true) }
        SectionCard("User actions") { BarChart(s.actions) }
    }
}

@Composable
private fun SessionItem(row: SessionRow, userName: String, onClick: () -> Unit) {
    SectionCard(title = null, modifier = Modifier.clickable(onClickLabel = "View session", onClick = onClick)) {
        Text(formatDateTime(row.startedAt), fontWeight = FontWeight.SemiBold)
        Text(userName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Fact("Length", if (row.endedAt != null) formatDuration(row.startedAt, row.endedAt) else "running")
            Fact("Frames", formatNumber(row.totalFrames))
            Fact("Ended by", pretty(row.endReason))
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
