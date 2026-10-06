package io.github.shahalam22.walksafe.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.data.auth.ResetStage

@Composable
fun ResetPasswordScreen(viewModel: ResetPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val stage by viewModel.stage.collectAsStateWithLifecycle()
    val ready = stage == ResetStage.READY

    BackHandler(onBack = viewModel::cancel)

    val subtitle = when {
        !ready -> "Checking the reset link…"
        viewModel.email != null -> "Choose a new password for ${viewModel.email}."
        else -> "Choose a new password for your account."
    }

    AuthLayout("New password", subtitle) {
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = { Text("New Password") },
            singleLine = true,
            enabled = ready,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.confirm,
            onValueChange = viewModel::onConfirm,
            label = { Text("Confirm New Password") },
            singleLine = true,
            enabled = ready,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let {
            Text(
                it,
                Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = viewModel::save,
            enabled = ready && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.busy) "Saving…" else "Reset password")
        }
        TextButton(onClick = viewModel::cancel, Modifier.align(Alignment.CenterHorizontally)) {
            Text("Cancel")
        }
    }
}
