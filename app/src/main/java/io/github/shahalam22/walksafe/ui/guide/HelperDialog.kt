package io.github.shahalam22.walksafe.ui.guide

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.shahalam22.walksafe.data.model.SignedInUser
import io.github.shahalam22.walksafe.data.prefs.UserPrefs

private val REPEAT_OPTIONS = listOf(0 to "Only when it changes", 5 to "Every 5 seconds", 10 to "Every 10 seconds")

/** For the sighted helper: set up the phone and change settings. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelperDialog(
    user: SignedInUser,
    prefs: UserPrefs,
    helper: HelperUiState,
    viewModel: GuideViewModel,
    onAllowCamera: () -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var batteryOk by remember { mutableStateOf(viewModel.batteryAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { batteryOk = viewModel.batteryAllowed() }
    var rate by remember(prefs.speechRate) { mutableFloatStateOf(prefs.speechRate) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.94f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Helper menu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Signed in as ${user.email}", color = MaterialTheme.colorScheme.onSurfaceVariant)

                Heading("Server")
                Text(helper.server, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = viewModel::checkServer, Modifier.fillMaxWidth()) { Text("Check server") }

                Heading("This phone")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAllowCamera) { Text(helper.camera) }
                    OutlinedButton(onClick = viewModel::testVoice) { Text("Test voice") }
                    if (batteryOk) {
                        OutlinedButton(onClick = {}, enabled = false) { Text("Battery: background allowed ✓") }
                    } else {
                        OutlinedButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:${context.packageName}")),
                                )
                            }.onFailure {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        }) { Text("Battery: allow background") }
                    }
                }
                Text(
                    "Allow the camera and notifications, and allow background use, so guidance keeps going " +
                        "with the screen off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Heading("Settings")
                Row(
                    Modifier.fillMaxWidth().toggleable(prefs.showView, role = Role.Switch, onValueChange = viewModel::setShowView),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Show camera and analysis on screen (for demos)", Modifier.weight(1f))
                    Switch(checked = prefs.showView, onCheckedChange = null)
                }
                Text("Repeat the current instruction", color = MaterialTheme.colorScheme.onSurfaceVariant)
                REPEAT_OPTIONS.forEach { (seconds, label) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(prefs.repeatSeconds == seconds, role = Role.RadioButton) { viewModel.setRepeat(seconds) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = prefs.repeatSeconds == seconds, onClick = null)
                        Text(label, Modifier.padding(start = 8.dp))
                    }
                }
                Text("Speech speed ${"%.1f".format(rate)}×", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = rate,
                    onValueChange = { rate = it },
                    onValueChangeFinished = { viewModel.setSpeechRate(rate) },
                    valueRange = 0.6f..1.6f,
                    steps = 9,
                )

                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (user.isAdmin) OutlinedButton(onClick = onOpenAdmin) { Text("Admin dashboard") }
                    TextButton(onClick = onSignOut) { Text("Sign out", color = MaterialTheme.colorScheme.error) }
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
