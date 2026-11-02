package com.example

import com.example.dsp.BiquadFilter
import com.example.dsp.FreeverbReverb
import com.example.dsp.LockFreeRingBuffer
import com.example.dsp.RealTimeKaraokeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class ExampleUnitTest {

    @Test
    fun testLockFreeRingBuffer() {
        val buffer = LockFreeRingBuffer(16)
        val input = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)
        val success = buffer.push(input, 0, 4)
        assertTrue("Push should succeed", success)
        assertEquals(4, buffer.availableRead())

        val output = FloatArray(4)
        val popSuccess = buffer.pop(output, 0, 4)
        assertTrue("Pop should succeed", popSuccess)
        assertEquals(0.1f, output[0], 0.0001f)
        assertEquals(0.4f, output[3], 0.0001f)
        assertEquals(0, buffer.availableRead())
    }

    @Test
    fun testBiquadFilterLowPass() {
        val filter = BiquadFilter()
        filter.setupLowPass(48000f, 250f)
        // DC response: passing constant 1.0 should converge close to 1.0
        var out = 0f
        for (i in 0 until 500) {
            out = filter.process(1.0f)
        }
        assertTrue("Low-pass DC gain should be close to 1.0", abs(out - 1.0f) < 0.05f)
    }

    @Test
    fun testRealTimeKaraokeEngineAlphaVocalSuppression() {
        val engine = RealTimeKaraokeEngine(48000f)
        val frameCount = 256
        val musicL = FloatArray(frameCount)
        val musicR = FloatArray(frameCount)
        val output = FloatArray(frameCount * 2)

        var sampleIndex = 0L

        // Block 1: Alpha = 0.0 (Original pass-through)
        for (i in 0 until frameCount) {
            val s = sin(2.0 * Math.PI * 1000.0 * (sampleIndex++ / 48000.0)).toFloat() * 0.8f
            musicL[i] = s
            musicR[i] = s
        }
        engine.setVocalSuppressionAlpha(0.0f)
        engine.processAudioBlock(musicL, musicR, null, output, frameCount)
        val peakAlpha0 = engine.getTelemetry().peakOutput

        // Block 2: Alpha = 1.0 (Full vocal suppression with continuous phase)
        for (i in 0 until frameCount) {
            val s = sin(2.0 * Math.PI * 1000.0 * (sampleIndex++ / 48000.0)).toFloat() * 0.8f
            musicL[i] = s
            musicR[i] = s
        }
        engine.setVocalSuppressionAlpha(1.0f)
        engine.processAudioBlock(musicL, musicR, null, output, frameCount)
        val peakAlpha1 = engine.getTelemetry().peakOutput

        assertTrue(
            "Vocal energy at alpha=1.0 ($peakAlpha1) should be drastically lower than at alpha=0.0 ($peakAlpha0)",
            peakAlpha1 < peakAlpha0 * 0.25f
        )
    }

    @Test
    fun testRmsComputation() {
        val engine = RealTimeKaraokeEngine(48000f)
        val frameCount = 256
        val musicL = FloatArray(frameCount) { 0.5f } // constant DC 0.5
        val musicR = FloatArray(frameCount) { 0.5f }
        val mic = FloatArray(frameCount) { 0.25f }
        val output = FloatArray(frameCount * 2)

        engine.processAudioBlock(musicL, musicR, mic, output, frameCount)
        val telemetry = engine.getTelemetry()

        // RMS of constant DC 0.5 is exactly 0.5
        assertEquals(0.5f, telemetry.rmsMusic, 0.01f)
        // RMS of constant DC 0.25 is exactly 0.25
        assertEquals(0.25f, telemetry.rmsMic, 0.01f)
    }
}
