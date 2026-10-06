package io.github.shahalam22.walksafe.ui.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.shahalam22.walksafe.R
import io.github.shahalam22.walksafe.ui.components.Message
import io.github.shahalam22.walksafe.ui.components.MessageText

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()

    if (state.forgotOpen) {
        BackHandler(onBack = viewModel::closeForgot)
        ForgotPassword(state, viewModel)
        return
    }

    AuthLayout("WalkSafe", "Sign in with the account your helper set up.") {
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmail,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.signIn() }),
            modifier = Modifier.fillMaxWidth(),
        )
        notice?.let { MessageText(Message(it.text, it.ok)) }
        state.error?.let {
            Text(
                it,
                Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = viewModel::signIn,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.busy) "Signing in…" else "Sign in")
        }
        if (state.showForgot || notice?.ok == false) {
            TextButton(onClick = viewModel::openForgot, Modifier.align(Alignment.CenterHorizontally)) {
                Text("Forgot password?")
            }
        }
    }
}

@Composable
private fun ForgotPassword(state: LoginUiState, viewModel: LoginViewModel) {
    AuthLayout("Reset password", "Enter the account's email. We will send a link to choose a new password.") {
        OutlinedTextField(
            value = state.resetEmail,
            onValueChange = viewModel::onResetEmail,
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { viewModel.sendResetLink() }),
            modifier = Modifier.fillMaxWidth(),
        )
        MessageText(state.resetMessage)
        Button(
            onClick = viewModel::sendResetLink,
            enabled = !state.sending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.sending) "Sending…" else "Send Reset Link")
        }
        TextButton(onClick = viewModel::closeForgot, Modifier.align(Alignment.CenterHorizontally)) {
            Text("Back to sign in")
        }
    }
}

/** The logo, a title and a short line above a narrow form: sign-in and password screens. */
@Composable
internal fun AuthLayout(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 380.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    title,
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    subtitle,
                    Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                content()
            }
        }
    }
}
