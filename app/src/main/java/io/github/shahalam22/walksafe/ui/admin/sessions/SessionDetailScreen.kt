package io.github.shahalam22.walksafe.ui.admin.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.ui.components.MessageText
import io.github.shahalam22.walksafe.ui.components.SectionCard
import io.github.shahalam22.walksafe.ui.components.SpeedChart
import io.github.shahalam22.walksafe.ui.util.formatDateTime
import io.github.shahalam22.walksafe.ui.util.formatDuration
import io.github.shahalam22.walksafe.ui.util.formatNumber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionDetailScreen(onBack: () -> Unit, viewModel: SessionDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Text("All sessions")
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (session != null) {
            item {
                Text("${state.userName} · ${formatDateTime(session.startedAt)}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Session ${session.sessionId.take(8)} · " +
                        if (session.endedAt != null) formatDuration(session.startedAt, session.endedAt) else "still running",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SectionCard("Export") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("frames" to "Frames CSV", "agents" to "Agents CSV", "gaps" to "Gaps CSV").forEach { (table, label) ->
                            OutlinedButton(onClick = { viewModel.export(table) }, enabled = state.exporting == null) {
                                Text(label)
                            }
                        }
                    }
                    state.exporting?.let { Text("Exporting $it…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    MessageText(state.message)
                }
            }
        }
        state.stats?.let { s ->
            item {
                StatTiles(
                    listOf(
                        "Frames" to formatNumber(s.totalFrames),
                        "Average speed" to (s.avgSpeedMs?.let { "$it m/s" } ?: "—"),
                        "Agent encounters" to formatNumber(s.totalAgents),
                        "Critical events" to formatNumber(s.criticalCount),
                    ),
                )
            }
            item { SectionCard("Speed") { SpeedChart(state.speed) } }
            item { StatsCharts(s) }
        }
    }
}
