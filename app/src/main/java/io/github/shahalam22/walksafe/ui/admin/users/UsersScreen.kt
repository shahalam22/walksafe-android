package io.github.shahalam22.walksafe.ui.admin.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.data.model.UserRow
import io.github.shahalam22.walksafe.ui.components.MessageText
import io.github.shahalam22.walksafe.ui.components.SectionCard
import io.github.shahalam22.walksafe.ui.util.formatDateTime
import io.github.shahalam22.walksafe.ui.util.formatNumber

@Composable
fun UsersScreen(viewModel: UsersViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var passwordFor by remember { mutableStateOf<UserRow?>(null) }
    var deleteFor by remember { mutableStateOf<UserRow?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SectionCard("Add a blind user") {
                OutlinedTextField(state.name, viewModel::onName, Modifier.fillMaxWidth(),
                    label = { Text("Name") }, placeholder = { Text("e.g. Rahim") }, singleLine = true)
                OutlinedTextField(state.email, viewModel::onEmail, Modifier.fillMaxWidth(),
                    label = { Text("Email") }, placeholder = { Text("used to sign in") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                OutlinedTextField(state.password, viewModel::onPassword, Modifier.fillMaxWidth(),
                    label = { Text("Password") }, placeholder = { Text("at least 6 characters") }, singleLine = true)
                Button(onClick = viewModel::addUser, enabled = !state.adding) { Text("Add user") }
                MessageText(state.formMessage)
            }
        }
        item {
            Text("Users", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Adding, changing or removing users needs the server to be running.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            MessageText(state.actionMessage)
        }
        items(state.users, key = { it.id }) { user ->
            UserItem(
                user,
                onPassword = { passwordFor = user },
                onToggle = { viewModel.toggleActive(user) },
                onDelete = { deleteFor = user },
            )
        }
    }

    passwordFor?.let { user ->
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { passwordFor = null },
            title = { Text("New password for ${user.name}") },
            text = {
                OutlinedTextField(password, { password = it }, label = { Text("At least 6 characters") }, singleLine = true)
            },
            confirmButton = {
                TextButton(enabled = password.length >= 6, onClick = {
                    viewModel.setPassword(user, password)
                    passwordFor = null
                }) { Text("Change") }
            },
            dismissButton = { TextButton(onClick = { passwordFor = null }) { Text("Cancel") } },
        )
    }

    deleteFor?.let { user ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("Delete ${user.name}?") },
            text = { Text("They can no longer sign in. Their sessions are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(user)
                    deleteFor = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteFor = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserItem(user: UserRow, onPassword: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    SectionCard(title = null) {
        Text(user.displayName?.takeIf { it.isNotBlank() } ?: "—", fontWeight = FontWeight.SemiBold)
        Text(user.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            listOf(
                if (user.isAdmin) "Admin" else "Blind user",
                if (user.isActive) "Active" else "Turned off",
                "${formatNumber(user.sessionCount)} sessions",
                "added ${formatDateTime(user.createdAt)}",
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!user.isAdmin) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPassword) { Text("New password") }
                OutlinedButton(onClick = onToggle) { Text(if (user.isActive) "Turn off" else "Turn on") }
                OutlinedButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
