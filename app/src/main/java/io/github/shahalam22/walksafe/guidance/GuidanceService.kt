package io.github.shahalam22.walksafe.guidance

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.shahalam22.walksafe.MainActivity
import io.github.shahalam22.walksafe.R
import io.github.shahalam22.walksafe.WalkSafeApp
import io.github.shahalam22.walksafe.data.auth.AuthRepository
import io.github.shahalam22.walksafe.data.prefs.UserPrefs
import io.github.shahalam22.walksafe.data.prefs.UserPrefsRepository
import io.github.shahalam22.walksafe.data.server.ApiException
import io.github.shahalam22.walksafe.data.server.ServerConfigRepository
import io.github.shahalam22.walksafe.data.server.WalkSafeApi
import io.github.shahalam22.walksafe.di.ApplicationScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Guides the blind user: camera frame → WalkSafe server → spoken instruction,
 * one frame at a time. A foreground service with a partial wake lock, so it
 * keeps going with the screen off. Stops on a tap in the app, a shake, or the
 * notification's Stop button.
 *
 * Everything here runs on the main thread; the network calls suspend.
 */
@AndroidEntryPoint
class GuidanceService : LifecycleService() {

    @Inject lateinit var api: WalkSafeApi
    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var serverConfig: ServerConfigRepository
    @Inject lateinit var prefsRepo: UserPrefsRepository
    @Inject lateinit var holder: GuidanceStateHolder
    @Inject lateinit var speaker: Speaker
    @Inject @field:ApplicationScope lateinit var appScope: CoroutineScope

    private lateinit var camera: FrameCapture
    private lateinit var shake: ShakeDetector
    private var wakeLock: PowerManager.WakeLock? = null
    private var job: Job? = null
    private var prefs = UserPrefs()
    private var base = ""
    private var sessionId: String? = null

    private val status get() = holder.state.value.status

    override fun onCreate() {
        super.onCreate()
        camera = FrameCapture(this)
        shake = ShakeDetector(this) { stop() }
        lifecycleScope.launch {
            prefsRepo.prefs.collect {
                prefs = it
                speaker.rate = it.speechRate
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> begin()
            ACTION_STOP -> if (status == GuidanceStatus.IDLE) stopSelf() else stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (status != GuidanceStatus.IDLE) {          // killed while guiding
            job?.cancel()
            endServerSession()
            release()
            holder.update { GuidanceState() }
        }
        super.onDestroy()
    }

    // ── Start / stop ─────────────────────────────────────────────────────────

    private fun begin() {
        // Every start request must put the service in the foreground, even a repeated one.
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification("Starting…"), foregroundType())
        } catch (e: Exception) {
            finish("The camera is not available. Ask your helper to allow the camera. WalkSafe is off.")
            return
        }
        if (status != GuidanceStatus.IDLE) return
        holder.update { GuidanceState(status = GuidanceStatus.STARTING, statusText = "Starting…") }
        speaker.say("Starting WalkSafe.", interrupt = true)
        vibrate(longArrayOf(0, 80))
        job = lifecycleScope.launch { run() }
    }

    private suspend fun run() {
        var cameraOn = false
        try {
            base = serverConfig.load().url
            if (!api.health(base).ready) throw ApiException(503, "models_loading")
            camera.start(this)
            cameraOn = true
            sessionId = api.startSession(base)
            acquireWakeLock()
            shake.start()
            holder.update { it.copy(status = GuidanceStatus.RUNNING, statusText = "Guiding") }
            updateNotification("Guiding")
            speaker.say("WalkSafe is on. Shake the phone or tap the screen to stop.")
            loop(sessionId!!)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            finish("${e.userMessage} WalkSafe is off.")
        } catch (e: Exception) {
            finish(
                if (cameraOn) "Something went wrong. WalkSafe is off."
                else "The camera is not available. Ask your helper to allow the camera. WalkSafe is off.",
            )
        }
    }

    /** Called from anywhere: a tap, a shake, the notification, or the loop itself. */
    private fun stop(message: String = "WalkSafe stopped.") {
        if (status == GuidanceStatus.IDLE || status == GuidanceStatus.STOPPING) return
        holder.update { it.copy(status = GuidanceStatus.STOPPING) }
        job?.cancel()
        endServerSession()
        finish(message)
    }

    private fun finish(message: String?) {
        release()
        holder.update {
            GuidanceState(statusText = message?.removeSuffix(" WalkSafe is off.") ?: "Stopped")
        }
        if (message != null) {
            speaker.say(message, interrupt = true)
            vibrate(longArrayOf(0, 60, 60, 60))
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun endServerSession() {
        val sid = sessionId ?: return
        val server = base
        sessionId = null
        appScope.launch { runCatching { api.stopSession(server, sid) } }
    }

    private fun release() {
        sessionId = null
        camera.stop()
        shake.stop()
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    // ── The frame loop ───────────────────────────────────────────────────────

    private suspend fun loop(sid: String) {
        var failures = 0
        var first = true
        var lastPhrase = ""
        var lastSpokenAt = 0L

        while (currentCoroutineContext().isActive) {
            val began = SystemClock.elapsedRealtime()
            val jpeg = camera.next() ?: continue
            try {
                val res = api.sendFrame(base, sid, jpeg, prefs.showView,
                    if (first) FIRST_FRAME_TIMEOUT_MS else FRAME_TIMEOUT_MS)
                if (failures > 0) speaker.say("Connected again.")
                failures = 0
                first = false

                val now = SystemClock.elapsedRealtime()
                val repeatMs = prefs.repeatSeconds * 1000L
                if (res.phrase != lastPhrase || (repeatMs > 0 && now - lastSpokenAt >= repeatMs)) {
                    val urgent = res.command == "stop"
                    if (urgent && res.phrase != lastPhrase) speaker.alarm()
                    speaker.say(res.phrase, interrupt = urgent)
                    lastPhrase = res.phrase
                    lastSpokenAt = now
                }
                val show = prefs.showView
                holder.update {
                    it.copy(
                        phrase = res.phrase,
                        detail = res.detail,
                        camera = if (show) decode(jpeg) else null,
                        panel = if (show) res.view?.let { v -> decode(Base64.decode(v, Base64.DEFAULT)) } else null,
                    )
                }
                delay(MIN_FRAME_MS - (SystemClock.elapsedRealtime() - began))
            } catch (e: ApiException) {
                when (e.status) {
                    409 -> return stop("The session ended on the server. WalkSafe stopped.")
                    401 -> {
                        auth.refresh()
                        if (++failures < 2) continue
                        return stop("The sign-in has expired. Ask your helper. WalkSafe stopped.")
                    }
                    else -> {
                        failures++
                        if (failures == 1) speaker.say("Connection lost. Trying again.", interrupt = true)
                        if (failures >= MAX_FAILURES) return stop("Cannot reach the WalkSafe server. WalkSafe stopped.")
                        delay(1000L * failures)
                    }
                }
            }
        }
    }

    // ── Android plumbing ─────────────────────────────────────────────────────

    private fun foregroundType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(this, 1, intent(this, ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, WalkSafeApp.GUIDANCE_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, getString(R.string.notification_stop), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String) {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text))
    }

    private fun acquireWakeLock() {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WalkSafe:guidance")
            .apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_MAX_MS)
            }
    }

    private fun vibrate(pattern: LongArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    private fun decode(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    companion object {
        const val ACTION_START = "io.github.shahalam22.walksafe.START"
        const val ACTION_STOP = "io.github.shahalam22.walksafe.STOP"

        private const val NOTIFICATION_ID = 1
        private const val FIRST_FRAME_TIMEOUT_MS = 120_000L   // the first frame waits for a depth map
        private const val FRAME_TIMEOUT_MS = 20_000L
        private const val MIN_FRAME_MS = 150L
        private const val MAX_FAILURES = 4                    // in a row, before giving up
        private const val WAKE_LOCK_MAX_MS = 4 * 60 * 60 * 1000L

        fun intent(context: Context, action: String): Intent =
            Intent(context, GuidanceService::class.java).setAction(action)
    }
}
