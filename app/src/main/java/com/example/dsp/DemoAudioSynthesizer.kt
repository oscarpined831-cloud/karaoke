package com.example.dsp

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * DemoAudioSynthesizer: Generates realistic multi-track audio blocks
 * featuring a distinct Center-Panned Vocal Melody alongside wide Stereo Guitars/Pads,
 * punchy Kick & Bass (<250Hz), and crisp percussion (>4000Hz).
 *
 * Perfect for verifying vocal isolation (alpha = 0.0 vs 0.5 vs 1.0) instantly!
 */
class DemoAudioSynthesizer(private val sampleRate: Float = 48000f) {

    private var sampleIndex = 0L

    // Musical note frequencies (A minor / C major progression)
    private val vocalMelody = floatArrayOf(
        440.0f, 440.0f, 523.25f, 587.33f, // A4, A4, C5, D5
        659.25f, 587.33f, 523.25f, 440.0f, // E5, D5, C5, A4
        392.0f, 440.0f, 523.25f, 440.0f,  // G4, A4, C5, A4
        329.63f, 392.0f, 440.0f, 440.0f   // E4, G4, A4, A4
    )

    private val bassNotes = floatArrayOf(
        110.0f, 110.0f, 130.81f, 146.83f, // A2, A2, C3, D3
        164.81f, 146.83f, 130.81f, 110.0f, // E3, D3, C3, A2
        98.00f, 110.0f, 130.81f, 110.0f,  // G2, A2, C3, A2
        82.41f, 98.00f, 110.0f, 110.0f    // E2, G2, A2, A2
    )

    fun generateBlock(outL: FloatArray, outR: FloatArray, frameCount: Int) {
        val beatDurationSamples = (sampleRate * 0.45f).toInt() // ~133 BPM

        for (i in 0 until frameCount) {
            val totalSample = sampleIndex + i
            val beatIndex = (totalSample / beatDurationSamples).toInt()
            val sampleInBeat = (totalSample % beatDurationSamples).toInt()
            val noteIdx = beatIndex % vocalMelody.size

            val timeSec = totalSample / sampleRate

            // --- 1. Bass (< 250 Hz) ---
            val bassFreq = bassNotes[noteIdx]
            val bassWave = sin(2.0 * PI * bassFreq * timeSec).toFloat() * 0.28f +
                    sin(4.0 * PI * bassFreq * timeSec).toFloat() * 0.12f

            // --- 2. Drums: Kick & Hi-hat ---
            val kickDecay = exp(-sampleInBeat / (sampleRate * 0.08f))
            val kickFreq = 120.0f * kickDecay + 45.0f
            val kick = (sin(2.0 * PI * kickFreq * (sampleInBeat / sampleRate)) * kickDecay).toFloat() * 0.35f

            val hatSubBeat = (totalSample % (beatDurationSamples / 2)).toInt()
            val hatDecay = exp(-hatSubBeat / (sampleRate * 0.015f))
            // Pseudo-random white noise for crisp hi-hat (> 4000 Hz)
            val noise = ((totalSample * 1103515245L + 12345L) and 0x7FFFFFFF).toFloat() / 0x7FFFFFFF - 0.5f
            val hiHat = noise * hatDecay * 0.14f

            // --- 3. Stereo Accompaniment (Chords & Arpeggio panned Left/Right) ---
            val chordFreqL = bassFreq * 2.0f // left chord
            val chordFreqR = bassFreq * 2.5f // right chord (major/minor third)
            val padL = (sin(2.0 * PI * chordFreqL * timeSec) * 0.18f + sin(4.0 * PI * chordFreqL * timeSec) * 0.06f).toFloat()
            val padR = (sin(2.0 * PI * chordFreqR * timeSec) * 0.18f + sin(4.0 * PI * chordFreqR * timeSec) * 0.06f).toFloat()

            // --- 4. Center Vocal (Phantom Center: equal on L and R with vocal formants) ---
            val vocalFreq = vocalMelody[noteIdx]
            val vocalVibrato = sin(2.0 * PI * 5.5 * timeSec).toFloat() * 6.0f // 5.5 Hz vibrato
            val currentVocalPitch = vocalFreq + vocalVibrato

            // Vocal envelope per note with attack & decay
            val noteFraction = sampleInBeat.toFloat() / beatDurationSamples
            val vocalEnv = if (noteFraction < 0.1f) {
                noteFraction / 0.1f
            } else {
                (1.0f - (noteFraction - 0.1f) / 0.9f).coerceIn(0.2f, 1.0f)
            }

            // Rich vocal timbre (fundamental + 2nd, 3rd, 4th harmonics mimicking vocal tract formant)
            val vocalWave = (
                sin(2.0 * PI * currentVocalPitch * timeSec) * 0.32f +
                sin(4.0 * PI * currentVocalPitch * timeSec) * 0.20f +
                sin(6.0 * PI * currentVocalPitch * timeSec) * 0.14f +
                sin(8.0 * PI * currentVocalPitch * timeSec) * 0.08f
            ).toFloat() * vocalEnv

            // Mix: Bass (Center) + Drums + Stereo Pads (Panned) + Vocal (Dead Center)
            outL[i] = bassWave + kick + padL + hiHat * 0.6f + vocalWave
            outR[i] = bassWave + kick + padR + hiHat * 0.4f + vocalWave
        }

        sampleIndex += frameCount
    }

    fun reset() {
        sampleIndex = 0L
    }
}
