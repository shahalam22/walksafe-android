package io.github.shahalam22.walksafe.guidance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The back camera, without a preview, so it keeps working with the screen off.
 * Frames are taken only when asked for, turned upright, scaled to the width the
 * server works at, and JPEG-encoded.
 */
class FrameCapture(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val wanted = AtomicBoolean(false)
    private val frames = Channel<ByteArray>(Channel.CONFLATED)
    private var provider: ProcessCameraProvider? = null

    suspend fun start(owner: LifecycleOwner) {
        val p = cameraProvider()
        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(Size(1280, 720), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                    )
                    .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setTargetRotation(Surface.ROTATION_0)       // the phone held upright
            .build()
        analysis.setAnalyzer(executor) { image ->
            image.use {
                if (wanted.getAndSet(false)) frames.trySend(encode(it))
            }
        }
        p.unbindAll()
        p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, analysis)
        provider = p
    }

    /** The next camera frame as JPEG, or null if none arrives in time. */
    suspend fun next(timeoutMs: Long = 3_000): ByteArray? {
        frames.tryReceive()                 // drop a frame left over from a timed-out request
        wanted.set(true)
        return withTimeoutOrNull(timeoutMs) { frames.receive() }
    }

    fun stop() {
        wanted.set(false)
        provider?.unbindAll()
        provider = null
    }

    private fun encode(image: ImageProxy): ByteArray {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val width = if (rotation % 180 == 0) bitmap.width else bitmap.height
        val scale = UPLOAD_WIDTH.toFloat() / width
        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
            postScale(scale, scale)
        }
        val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return ByteArrayOutputStream().use { out ->
            upright.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    }

    private suspend fun cameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching { future.get() }
                .onSuccess { cont.resume(it) }
                .onFailure { cont.resumeWithException(it) }
        }, ContextCompat.getMainExecutor(context))
    }

    private companion object {
        const val UPLOAD_WIDTH = 518      // the server works at this width
        const val JPEG_QUALITY = 70
    }
}
