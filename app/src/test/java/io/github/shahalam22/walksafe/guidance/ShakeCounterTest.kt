package io.github.shahalam22.walksafe.guidance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeCounterTest {

    private val strong = 30f

    @Test
    fun threeStrongPeaksInAWindowAreAShake() {
        val c = ShakeCounter()
        assertFalse(c.onReading(strong, 0f, 0f, 0))
        assertFalse(c.onReading(strong, 0f, 0f, 300))
        assertTrue(c.onReading(strong, 0f, 0f, 600))
    }

    @Test
    fun gentleMovementIsIgnored() {
        val c = ShakeCounter()
        repeat(10) { assertFalse(c.onReading(0f, 9.8f, 3f, it * 200L)) }
    }

    @Test
    fun readingsOfOneSwingCountOnce() {
        val c = ShakeCounter()
        assertFalse(c.onReading(strong, 0f, 0f, 0))
        assertFalse(c.onReading(strong, 0f, 0f, 50))
        assertFalse(c.onReading(strong, 0f, 0f, 100))
    }

    @Test
    fun peaksTooFarApartAreNotAShake() {
        val c = ShakeCounter()
        assertFalse(c.onReading(strong, 0f, 0f, 0))
        assertFalse(c.onReading(strong, 0f, 0f, 1_000))
        assertFalse(c.onReading(strong, 0f, 0f, 2_100))
    }
}
