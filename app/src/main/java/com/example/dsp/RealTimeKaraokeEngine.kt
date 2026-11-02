package com.example.dsp

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

data class EngineTelemetry(
    val peakMusic: Float = 0f,
    val peakMic: Float = 0f,
    val peakOutput: Float = 0f,
    val rmsMusic: Float = 0f,
    val rmsMic: Float = 0f,
    val bassEnergy: Float = 0f,
    val vocalMidEnergy: Float = 0f,
    val highEnergy: Float = 0f,
    val vocalSuppressionDb: Float = 0f,
    val currentAlpha: Float = 0f,
    val waveformSamples: FloatArray = FloatArray(64)
)

/**
 * RealTimeKaraokeEngine in Kotlin:
 * Executes the complete VOX-ENGINE-ZERO-HW signal processing pipeline:
 * - 3-band Mid/Side vocal isolation with alpha factor [0.0 .. 1.0]
 * - Live microphone conditioning: 85 Hz Butterworth HPF, soft noise gate (-45 dBFS),
 *   Schroeder/Freeverb algorithmic reverb, and adjustable gain.
 * - Monitored mix sum with true-peak cubic soft-clipping limiter.
 */
class RealTimeKaraokeEngine(private val sampleRate: Float = 48000f) {

    // Atomic runtime parameters
    private val alphaFactor = AtomicReference(0.0f)
    private val micGain = AtomicReference(1.2f)
    private val reverbWet = AtomicReference(0.30f)
    private val noiseGateEnabled = AtomicBoolean(true)
    private val hpfEnabled = AtomicBoolean(true)

    // Filters
    private val lpFilterL = BiquadFilter().apply { setupLowPass(sampleRate, 250f) }
    private val lpFilterR = BiquadFilter().apply { setupLowPass(sampleRate, 250f) }
    private val hpFilterL = BiquadFilter().apply { setupHighPass(sampleRate, 4000f) }
    private val hpFilterR = BiquadFilter().apply { setupHighPass(sampleRate, 4000f) }
    private val micHpFilter = BiquadFilter().apply { setupHighPass(sampleRate, 85f) }
    private val micReverb = FreeverbReverb(sampleRate)

    // Noise gate state
    private var micRmsEnv = 0.0f

    // Visualizer telemetry buffer
    private val telemetry = AtomicReference(EngineTelemetry())
    private val waveformBuffer = FloatArray(64)
    private var waveformIndex = 0

    fun setVocalSuppressionAlpha(alpha: Float) {
        val clamped = alpha.coerceIn(0.0f, 1.0f)
        alphaFactor.set(clamped)
    }

    fun getVocalSuppressionAlpha(): Float = alphaFactor.get()

    fun setMicrophoneGain(gain: Float) {
        micGain.set(max(0.0f, gain))
    }

    fun getMicrophoneGain(): Float = micGain.get()

    fun setReverbLevel(wetLevel: Float) {
        reverbWet.set(wetLevel.coerceIn(0.0f, 1.0f))
    }

    fun getReverbLevel(): Float = reverbWet.get()

    fun setNoiseGateEnabled(enabled: Boolean) {
        noiseGateEnabled.set(enabled)
    }

    fun isNoiseGateEnabled(): Boolean = noiseGateEnabled.get()

    fun setHpfEnabled(enabled: Boolean) {
        hpfEnabled.set(enabled)
    }

    fun isHpfEnabled(): Boolean = hpfEnabled.get()

    fun getTelemetry(): EngineTelemetry = telemetry.get()

    /**
     * Critical realtime DSP block process.
     * Processes stereo music and mono mic into interleaved stereo output.
     */
    fun processAudioBlock(
        musicInputL: FloatArray?,
        musicInputR: FloatArray?,
        micInput: FloatArray?,
        outputStereoInterleaved: FloatArray,
        frameCount: Int
    ) {
        val currentAlpha = alphaFactor.get()
        val currentMicGain = micGain.get()
        val currentReverb = reverbWet.get()
        val useNoiseGate = noiseGateEnabled.get()
        val useHpf = hpfEnabled.get()

        var blockPeakMusic = 0.0f
        var blockPeakMic = 0.0f
        var blockPeakOut = 0.0f
        var sumSqMusic = 0.0f
        var sumSqMic = 0.0f
        var sumBass = 0.0f
        var sumVocalMid = 0.0f
        var sumHighs = 0.0f

        val step = max(1, frameCount / 64)

        for (i in 0 until frameCount) {
            val inL = musicInputL?.getOrNull(i) ?: 0.0f
            val inR = musicInputR?.getOrNull(i) ?: 0.0f
            sumSqMusic += 0.5f * (inL * inL + inR * inR)

            val absMusic = max(abs(inL), abs(inR))
            if (absMusic > blockPeakMusic) blockPeakMusic = absMusic

            // 1. Crossover: Low-pass bass (< 250 Hz)
            val bassL = lpFilterL.process(inL)
            val bassR = lpFilterR.process(inR)
            sumBass += (abs(bassL) + abs(bassR)) * 0.5f

            // 2. Crossover: High-pass transients (> 4000 Hz)
            val highsL = hpFilterL.process(inL)
            val highsR = hpFilterR.process(inR)
            sumHighs += (abs(highsL) + abs(highsR)) * 0.5f

            // 3. Vocal midband extraction (250 Hz .. 4000 Hz)
            val midsL = inL - bassL - highsL
            val midsR = inR - bassR - highsR

            // 4. Mid/Side decomposition of midband
            val mid = 0.5f * (midsL + midsR)
            val side = 0.5f * (midsL - midsR)
            sumVocalMid += abs(mid)

            // 5. Parametric vocal attenuation (Phantom Center suppression)
            // |Y(m,k)| = |X(m,k)| * [1.0 - alpha * Mv(m,k)]
            val processedMid = mid * (1.0f - currentAlpha)

            // 6. Resynthesis of instrumental output
            var outL = bassL + highsL + (processedMid + side)
            var outR = bassR + highsR + (processedMid - side)

            // 7. Live microphone conditioning
            val rawMic = micInput?.getOrNull(i) ?: 0.0f
            sumSqMic += rawMic * rawMic
            val absMic = abs(rawMic)
            if (absMic > blockPeakMic) blockPeakMic = absMic

            // 7a. Butterworth HPF at 85 Hz
            val micHpf = if (useHpf) micHpFilter.process(rawMic) else rawMic

            // 7b. Soft noise gate with -45 dBFS threshold
            var micGated = micHpf
            if (useNoiseGate) {
                val power = micHpf * micHpf
                micRmsEnv = 0.995f * micRmsEnv + 0.005f * power
                val rmsDb = 10.0f * log10(max(micRmsEnv, 1e-9f))
                if (rmsDb < -45.0f) {
                    val gateRatio = ((rmsDb - (-60.0f)) / 15.0f).coerceIn(0.0f, 1.0f)
                    micGated *= gateRatio
                }
            }

            // 7c. Algorithmic Reverb
            val micReverbed = micReverb.process(micGated, currentReverb)

            // 7d. Mic gain amplification
            val finalMic = micReverbed * currentMicGain

            // 8. Mixer Sum: music + mic
            outL += finalMic
            outR += finalMic

            // 9. True-Peak Cubic Soft-Clipping Limiter
            val clippedL = softClip(outL)
            val clippedR = softClip(outR)

            outputStereoInterleaved[i * 2] = clippedL
            outputStereoInterleaved[i * 2 + 1] = clippedR

            val absOut = max(abs(clippedL), abs(clippedR))
            if (absOut > blockPeakOut) blockPeakOut = absOut

            // Capture waveform subsamples for visualizer
            if (i % step == 0 && waveformIndex < 64) {
                waveformBuffer[waveformIndex++] = (clippedL + clippedR) * 0.5f
            }
        }

        waveformIndex = 0

        val blockRmsMusic = sqrt(sumSqMusic / max(1, frameCount))
        val blockRmsMic = sqrt(sumSqMic / max(1, frameCount))

        // Compute vocal attenuation in dB
        val suppressionDb = if (currentAlpha > 0.001f) {
            -20.0f * log10(max(1.0f - currentAlpha, 0.01f))
        } else {
            0.0f
        }

        telemetry.set(
            EngineTelemetry(
                peakMusic = blockPeakMusic,
                peakMic = blockPeakMic,
                peakOutput = blockPeakOut,
                rmsMusic = blockRmsMusic,
                rmsMic = blockRmsMic,
                bassEnergy = sumBass / frameCount,
                vocalMidEnergy = sumVocalMid / frameCount,
                highEnergy = sumHighs / frameCount,
                vocalSuppressionDb = suppressionDb,
                currentAlpha = currentAlpha,
                waveformSamples = waveformBuffer.copyOf()
            )
        )
    }

    /**
     * Soft-clipping cubic polynomial transfer function:
     * Prevents harsh digital clipping while maintaining warm analog saturation.
     */
    private fun softClip(input: Float): Float {
        val absVal = abs(input)
        return when {
            absVal <= (2.0f / 3.0f) -> input
            absVal <= 1.0f -> {
                val sgn = sign(input)
                val diff = 2.0f - 3.0f * absVal
                sgn * (3.0f - diff * diff) / 3.0f
            }
            else -> sign(input)
        }
    }

    fun reset() {
        lpFilterL.reset()
        lpFilterR.reset()
        hpFilterL.reset()
        hpFilterR.reset()
        micHpFilter.reset()
        micRmsEnv = 0f
    }
}
