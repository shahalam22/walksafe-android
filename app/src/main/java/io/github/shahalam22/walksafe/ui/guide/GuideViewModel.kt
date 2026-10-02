package io.github.shahalam22.walksafe.ui.guide

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.shahalam22.walksafe.data.prefs.UserPrefs
import io.github.shahalam22.walksafe.data.prefs.UserPrefsRepository
import io.github.shahalam22.walksafe.data.server.ApiException
import io.github.shahalam22.walksafe.data.server.ServerConfigRepository
import io.github.shahalam22.walksafe.data.server.WalkSafeApi
import io.github.shahalam22.walksafe.guidance.GuidanceController
import io.github.shahalam22.walksafe.guidance.GuidanceState
import io.github.shahalam22.walksafe.guidance.GuidanceStateHolder
import io.github.shahalam22.walksafe.guidance.GuidanceStatus
import io.github.shahalam22.walksafe.guidance.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HelperUiState(
    val server: String = "Not checked yet.",
    val camera: String = "Allow camera",
)

@HiltViewModel
class GuideViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val holder: GuidanceStateHolder,
    private val controller: GuidanceController,
    private val prefsRepo: UserPrefsRepository,
    private val serverConfig: ServerConfigRepository,
    private val api: WalkSafeApi,
    private val speaker: Speaker,
) : ViewModel() {

    val guidance: StateFlow<GuidanceState> = holder.state
    val prefs: StateFlow<UserPrefs> =
        prefsRepo.prefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPrefs())

    private val _helper = MutableStateFlow(HelperUiState())
    val helper: StateFlow<HelperUiState> = _helper.asStateFlow()

    private var lastToggle = 0L

    init {
        viewModelScope.launch { prefsRepo.prefs.collect { speaker.rate = it.speechRate } }
    }

    /** The whole screen is one button: tap to start, tap again to stop. */
    fun onTap() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToggle < TAP_GUARD_MS) return           // a double tap counts once
        lastToggle = now
        when (holder.state.value.status) {
            GuidanceStatus.IDLE -> start()
            GuidanceStatus.STARTING, GuidanceStatus.RUNNING -> controller.stop()
            GuidanceStatus.STOPPING -> Unit
        }
    }

    fun stop() = controller.stop()

    private fun start() {
        if (!controller.hasCamera()) {
            val why = "The camera is not available. Ask your helper to allow the camera."
            holder.update { it.copy(statusText = why) }
            speaker.say("$why WalkSafe is off.", interrupt = true)
            return
        }
        controller.start()
    }

    fun hasCamera(): Boolean = controller.hasCamera()

    fun onPermissionsResult(cameraAllowed: Boolean) {
        _helper.update {
            it.copy(camera = if (cameraAllowed) "Camera allowed ✓" else "Camera blocked: allow it in the app's settings")
        }
        if (cameraAllowed) speaker.say("Camera allowed.")
    }

    fun checkServer() {
        _helper.update { it.copy(server = "Checking…") }
        viewModelScope.launch {
            val address = serverConfig.load()
            val text = try {
                val h = api.health(address.url)
                (if (h.ready) "Server is running. " else "Server is still loading. ") + address.url +
                    (if (address.fromCache) " (saved address; Supabase not reachable)" else "")
            } catch (e: ApiException) {
                "${e.userMessage} ${address.url}"
            }
            _helper.update { it.copy(server = text) }
        }
    }

    fun testVoice() {
        speaker.alarm()
        speaker.say("This is how WalkSafe sounds. Walk forward. Step left. Stop.", interrupt = true)
    }

    fun batteryAllowed(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun setShowView(show: Boolean) {
        viewModelScope.launch { prefsRepo.setShowView(show) }
    }

    fun setRepeat(seconds: Int) {
        viewModelScope.launch { prefsRepo.setRepeatSeconds(seconds) }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch { prefsRepo.setSpeechRate(rate) }
    }

    private companion object {
        const val TAP_GUARD_MS = 1_500L
    }
}
