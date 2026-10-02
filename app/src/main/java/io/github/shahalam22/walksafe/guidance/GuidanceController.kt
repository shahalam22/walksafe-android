package io.github.shahalam22.walksafe.guidance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Starts and stops the guidance service. */
@Singleton
class GuidanceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val holder: GuidanceStateHolder,
) {
    fun hasCamera(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    /** Call only while the app is on screen and the camera is allowed. */
    fun start() {
        ContextCompat.startForegroundService(context, GuidanceService.intent(context, GuidanceService.ACTION_START))
    }

    fun stop() {
        if (holder.state.value.status == GuidanceStatus.IDLE) return
        context.startService(GuidanceService.intent(context, GuidanceService.ACTION_STOP))
    }
}
