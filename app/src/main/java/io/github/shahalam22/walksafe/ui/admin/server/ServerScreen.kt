package io.github.shahalam22.walksafe.ui.admin.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.ui.components.MessageText
import io.github.shahalam22.walksafe.ui.components.SectionCard

@Composable
fun ServerScreen(viewModel: ServerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionCard("Server address") {
            Text(
                "Each time you start the Colab notebook, it prints a new address " +
                    "(https://….trycloudflare.com). Paste it here and save. Every phone reads it " +
                    "the next time WalkSafe starts.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrl,
                label = { Text("Address") },
                placeholder = { Text("https://example.trycloudflare.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::test, enabled = !state.busy) { Text("Test") }
                Button(onClick = viewModel::save, enabled = !state.busy) { Text("Save") }
            }
            MessageText(state.message)
            Text(state.saved, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
