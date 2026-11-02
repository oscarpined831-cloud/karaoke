package com.example.dsp

/**
 * Comb filter for Schroeder/Freeverb reverberator.
 */
class CombFilter(
    private val bufferSize: Int,
    var feedback: Float = 0.84f,
    var damping: Float = 0.2f
) {
    private val buffer = FloatArray(bufferSize)
    private var index = 0
    private var filterStore = 0.0f

    fun process(input: Float): Float {
        val output = buffer[index]
        filterStore = (output * (1.0f - damping)) + (filterStore * damping)
        buffer[index] = input + (filterStore * feedback)
        index = (index + 1) % bufferSize
        return output
    }
}

/**
 * All-pass filter for Schroeder/Freeverb reverberator.
 */
class AllPassFilter(
    private val bufferSize: Int,
    var feedback: Float = 0.5f
) {
    private val buffer = FloatArray(bufferSize)
    private var index = 0

    fun process(input: Float): Float {
        val bufOut = buffer[index]
        val output = -input + bufOut
        buffer[index] = input + (bufOut * feedback)
        index = (index + 1) % bufferSize
        return output
    }
}

/**
 * Algorithmic Freeverb implementation for vocal spatialization.
 */
class FreeverbReverb(sampleRate: Float = 48000f) {
    private val combs: Array<CombFilter>
    private val allpasses: Array<AllPassFilter>

    init {
        val scale = sampleRate / 44100.0f
        combs = arrayOf(
            CombFilter((1116 * scale).toInt(), 0.84f, 0.2f),
            CombFilter((1188 * scale).toInt(), 0.84f, 0.2f),
            CombFilter((1277 * scale).toInt(), 0.84f, 0.2f),
            CombFilter((1356 * scale).toInt(), 0.84f, 0.2f)
        )
        allpasses = arrayOf(
            AllPassFilter((556 * scale).toInt(), 0.5f),
            AllPassFilter((441 * scale).toInt(), 0.5f)
        )
    }

    fun process(input: Float, wetLevel: Float): Float {
        if (wetLevel <= 0.001f) return input
        var outCombs = 0.0f
        for (comb in combs) {
            outCombs += comb.process(input)
        }
        outCombs *= 0.25f
        val outAllPass = allpasses[1].process(allpasses[0].process(outCombs))
        return input * (1.0f - wetLevel) + outAllPass * wetLevel
    }
}
