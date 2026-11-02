package com.example.dsp

import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance 2nd-order IIR Biquad Filter in Kotlin,
 * mathematically equivalent to the C++ implementation.
 */
class BiquadFilter {
    private var b0 = 1.0f
    private var b1 = 0.0f
    private var b2 = 0.0f
    private var a1 = 0.0f
    private var a2 = 0.0f
    private var z1 = 0.0f
    private var z2 = 0.0f

    fun setupLowPass(sampleRate: Float, cutoffFreq: Float, q: Float = 0.7071f) {
        val omega = (2.0 * Math.PI * cutoffFreq / sampleRate).toFloat()
        val alpha = sin(omega) / (2.0f * q)
        val cosOmega = cos(omega)

        val b0Unnorm = (1.0f - cosOmega) / 2.0f
        val b1Unnorm = 1.0f - cosOmega
        val b2Unnorm = (1.0f - cosOmega) / 2.0f
        val a0 = 1.0f + alpha
        val a1Unnorm = -2.0f * cosOmega
        val a2Unnorm = 1.0f - alpha

        b0 = b0Unnorm / a0
        b1 = b1Unnorm / a0
        b2 = b2Unnorm / a0
        a1 = a1Unnorm / a0
        a2 = a2Unnorm / a0
        z1 = 0.0f
        z2 = 0.0f
    }

    fun setupHighPass(sampleRate: Float, cutoffFreq: Float, q: Float = 0.7071f) {
        val omega = (2.0 * Math.PI * cutoffFreq / sampleRate).toFloat()
        val alpha = sin(omega) / (2.0f * q)
        val cosOmega = cos(omega)

        val b0Unnorm = (1.0f + cosOmega) / 2.0f
        val b1Unnorm = -(1.0f + cosOmega)
        val b2Unnorm = (1.0f + cosOmega) / 2.0f
        val a0 = 1.0f + alpha
        val a1Unnorm = -2.0f * cosOmega
        val a2Unnorm = 1.0f - alpha

        b0 = b0Unnorm / a0
        b1 = b1Unnorm / a0
        b2 = b2Unnorm / a0
        a1 = a1Unnorm / a0
        a2 = a2Unnorm / a0
        z1 = 0.0f
        z2 = 0.0f
    }

    fun process(input: Float): Float {
        val output = input * b0 + z1
        z1 = input * b1 - a1 * output + z2
        z2 = input * b2 - a2 * output
        return output
    }

    fun reset() {
        z1 = 0.0f
        z2 = 0.0f
    }
}
