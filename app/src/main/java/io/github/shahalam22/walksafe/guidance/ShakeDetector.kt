package io.github.shahalam22.walksafe.guidance

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Counts strong shakes: [peaks] readings over [threshold] m/s² within [windowMs],
 * at least [gapMs] apart (one swing is one peak).
 */
class ShakeCounter(
    private val threshold: Float = 25f,
    private val peaks: Int = 3,
    private val windowMs: Long = 1_200,
    private val gapMs: Long = 150,
) {
    private val times = ArrayDeque<Long>()

    /** Returns true when this reading completes a shake. */
    fun onReading(x: Float, y: Float, z: Float, nowMs: Long): Boolean {
        if (sqrt(x * x + y * y + z * z) < threshold) return false
        if (times.isNotEmpty() && nowMs - times.last() < gapMs) return false
        while (times.isNotEmpty() && nowMs - times.first() >= windowMs) times.removeFirst()
        times.addLast(nowMs)
        if (times.size >= peaks) {
            times.clear()
            return true
        }
        return false
    }

    fun reset() = times.clear()
}

/** Shaking the phone firmly stops guidance; works with the screen off. */
class ShakeDetector(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensors = context.getSystemService(SensorManager::class.java)
    private val counter = ShakeCounter()

    fun start() {
        counter.reset()
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() = sensors.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent) {
        val (x, y, z) = event.values
        if (counter.onReading(x, y, z, event.timestamp / 1_000_000)) onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
