package io.github.shahalam22.walksafe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.shahalam22.walksafe.ui.theme.SuccessGreen

@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            content()
        }
    }
}

/** A result line: green when it worked, red when it did not. Read out by screen readers. */
data class Message(val text: String, val ok: Boolean)

@Composable
fun MessageText(message: Message?, modifier: Modifier = Modifier) {
    message ?: return
    Text(
        message.text,
        modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = if (message.ok) SuccessGreen else MaterialTheme.colorScheme.error,
    )
}
