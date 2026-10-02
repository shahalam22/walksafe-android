package io.github.shahalam22.walksafe.guidance

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

enum class GuidanceStatus { IDLE, STARTING, RUNNING, STOPPING }

data class GuidanceState(
    val status: GuidanceStatus = GuidanceStatus.IDLE,
    val statusText: String = "",
    val phrase: String = "",
    val detail: String = "",
    val camera: Bitmap? = null,      // the last frame sent (demo view only)
    val panel: Bitmap? = null,       // the server's analysis panel (demo view only)
) {
    val active: Boolean get() = status == GuidanceStatus.STARTING || status == GuidanceStatus.RUNNING
}

/** What the guidance service is doing, for the screen to show. */
@Singleton
class GuidanceStateHolder @Inject constructor() {
    private val _state = MutableStateFlow(GuidanceState())
    val state: StateFlow<GuidanceState> = _state.asStateFlow()

    fun update(change: (GuidanceState) -> GuidanceState) = _state.update(change)
}
