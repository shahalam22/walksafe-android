package io.github.shahalam22.walksafe.ui.admin.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.shahalam22.walksafe.ui.components.SectionCard

private const val APK_URL = "https://github.com/shahalam22/walksafe-android/releases/latest/download/walksafe.apk"

private val SETUP = listOf(
    "Start the server on Colab and save its address in Server.",
    "Add the person in Users with an email and password.",
    "On their Android phone, download and install the app: $APK_URL " +
        "(allow installing from this source; tap Install anyway if Play Protect warns).",
    "Open WalkSafe and sign in with that email and password.",
    "Open Helper menu (top of the screen): tap Allow camera and allow the camera and notifications, " +
        "then Battery: allow background and choose Allow, then Test voice.",
    "Tap Check server, then close the menu and tap the screen once to try a walk.",
)

private val TEACH = listOf(
    "Open: tap the WalkSafe icon, or say “Hey Google, open WalkSafe”.",
    "Start: tap anywhere on the screen (with TalkBack: double-tap anywhere).",
    "Stop: shake the phone firmly, or tap anywhere again.",
    "Hold the phone upright at chest height with the back camera facing forward.",
    "The screen can be turned off with the power button: guidance keeps going. " +
        "Pressing power again shows WalkSafe, so a tap stops it.",
)

@Composable
fun SetupScreen() {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        SectionCard("Setting up a blind person's phone") {
            SETUP.forEachIndexed { i, step -> Step("${i + 1}.", step) }
            Text("Teach the user", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
            TEACH.forEach { Step("•", it) }
        }
    }
}

@Composable
private fun Step(marker: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$marker $text", style = MaterialTheme.typography.bodyMedium)
    }
}
