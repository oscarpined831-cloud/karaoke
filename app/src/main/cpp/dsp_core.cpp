#include "dsp_core.h"
#include <cmath>
#include <algorithm>

void BiquadFilter::setupLowPass(float sampleRate, float cutoffFreq, float q) {
    float omega = 2.0f * DSP_PI * cutoffFreq / sampleRate;
    float alpha = std::sin(omega) / (2.0f * q);
    float cosOmega = std::cos(omega);

    float b0 = (1.0f - cosOmega) / 2.0f;
    float b1 = 1.0f - cosOmega;
    float b2 = (1.0f - cosOmega) / 2.0f;
    float a0 = 1.0f + alpha;
    float a1 = -2.0f * cosOmega;
    float a2 = 1.0f - alpha;

    b0_ = b0 / a0; b1_ = b1 / a0; b2_ = b2 / a0;
    a1_ = a1 / a0; a2_ = a2 / a0;
    z1_ = z2_ = 0.0f;
}

void BiquadFilter::setupHighPass(float sampleRate, float cutoffFreq, float q) {
    float omega = 2.0f * DSP_PI * cutoffFreq / sampleRate;
    float alpha = std::sin(omega) / (2.0f * q);
    float cosOmega = std::cos(omega);

    float b0 = (1.0f + cosOmega) / 2.0f;
    float b1 = -(1.0f + cosOmega);
    float b2 = (1.0f + cosOmega) / 2.0f;
    float a0 = 1.0f + alpha;
    float a1 = -2.0f * cosOmega;
    float a2 = 1.0f - alpha;

    b0_ = b0 / a0; b1_ = b1 / a0; b2_ = b2 / a0;
    a1_ = a1 / a0; a2_ = a2 / a0;
    z1_ = z2_ = 0.0f;
}

void BiquadFilter::reset() {
    z1_ = z2_ = 0.0f;
}

void CombFilter::init(size_t bufferSize, float feedback, float damping) {
    buffer_.assign(bufferSize, 0.0f);
    index_ = 0;
    feedback_ = feedback;
    damping_ = damping;
    filterStore_ = 0.0f;
}

float CombFilter::process(float input) {
    if (buffer_.empty()) return input;
    float output = buffer_[index_];
    filterStore_ = (output * (1.0f - damping_)) + (filterStore_ * damping_);
    buffer_[index_] = input + (filterStore_ * feedback_);
    if (++index_ >= buffer_.size()) index_ = 0;
    return output;
}

void AllPassFilter::init(size_t bufferSize, float feedback) {
    buffer_.assign(bufferSize, 0.0f);
    index_ = 0;
    feedback_ = feedback;
}

float AllPassFilter::process(float input) {
    if (buffer_.empty()) return input;
    float bufOut = buffer_[index_];
    float output = -input + bufOut;
    buffer_[index_] = input + (bufOut * feedback_);
    if (++index_ >= buffer_.size()) index_ = 0;
    return output;
}

void FreeverbReverb::setup(float sampleRate) {
    float scale = sampleRate / 44100.0f;
    size_t combLengths[4] = {
        static_cast<size_t>(1116 * scale),
        static_cast<size_t>(1188 * scale),
        static_cast<size_t>(1277 * scale),
        static_cast<size_t>(1356 * scale)
    };
    for (int i = 0; i < 4; ++i) {
        combs_[i].init(combLengths[i], 0.84f, 0.2f);
    }
    size_t allPassLengths[2] = {
        static_cast<size_t>(556 * scale),
        static_cast<size_t>(441 * scale)
    };
    allpasses_[0].init(allPassLengths[0], 0.5f);
    allpasses_[1].init(allPassLengths[1], 0.5f);
}

float FreeverbReverb::process(float input, float wetLevel) {
    if (wetLevel <= 0.001f) return input;
    float outCombs = 0.0f;
    for (int i = 0; i < 4; ++i) {
        outCombs += combs_[i].process(input);
    }
    outCombs *= 0.25f;
    float outAllPass = allpasses_[1].process(allpasses_[0].process(outCombs));
    return input * (1.0f - wetLevel) + outAllPass * wetLevel;
}

RealTimeKaraokeEngine::RealTimeKaraokeEngine(float sampleRate)
    : sampleRate_(sampleRate), alphaFactor_(0.0f), micGain_(1.2f) {
    lpFilterL_.setupLowPass(sampleRate, 250.0f);
    lpFilterR_.setupLowPass(sampleRate, 250.0f);
    hpFilterL_.setupHighPass(sampleRate, 4000.0f);
    hpFilterR_.setupHighPass(sampleRate, 4000.0f);
    micHpFilter_.setupHighPass(sampleRate, 85.0f);
    micReverb_.setup(sampleRate);
}

void RealTimeKaraokeEngine::setVocalSuppressionAlpha(float alpha) {
    alphaFactor_.store(std::clamp(alpha, 0.0f, 1.0f), std::memory_order_relaxed);
}

void RealTimeKaraokeEngine::setMicrophoneGain(float gain) {
    micGain_.store(std::max(0.0f, gain), std::memory_order_relaxed);
}

void RealTimeKaraokeEngine::setReverbLevel(float wetLevel) {
    reverbWet_.store(std::clamp(wetLevel, 0.0f, 1.0f), std::memory_order_relaxed);
}

void RealTimeKaraokeEngine::setNoiseGateEnabled(bool enabled) {
    noiseGateEnabled_.store(enabled, std::memory_order_relaxed);
}

void RealTimeKaraokeEngine::processAudioBlock(const float* musicInputL, const float* musicInputR,
                                             const float* micInput, float* outputStereoInterleaved,
                                             size_t frameCount) {
    float currentAlpha = alphaFactor_.load(std::memory_order_relaxed);
    float currentMicGain = micGain_.load(std::memory_order_relaxed);
    float currentReverb = reverbWet_.load(std::memory_order_relaxed);
    bool useNoiseGate = noiseGateEnabled_.load(std::memory_order_relaxed);

    float blockPeakMusic = 0.0f;
    float blockPeakMic = 0.0f;

    for (size_t i = 0; i < frameCount; ++i) {
        float inL = musicInputL ? musicInputL[i] : 0.0f;
        float inR = musicInputR ? musicInputR[i] : 0.0f;

        blockPeakMusic = std::max(blockPeakMusic, std::max(std::abs(inL), std::abs(inR)));

        // 1. Bass crossover (< 250 Hz)
        float bassL = lpFilterL_.process(inL);
        float bassR = lpFilterR_.process(inR);

        // 2. Highs crossover (> 4000 Hz)
        float highsL = hpFilterL_.process(inL);
        float highsR = hpFilterR_.process(inR);

        // 3. Vocal midband extraction
        float midsL = inL - bassL - highsL;
        float midsR = inR - bassR - highsR;

        // 4. Mid/Side decomposition
        float mid = 0.5f * (midsL + midsR);
        float side = 0.5f * (midsL - midsR);

        // 5. Parametric vocal attenuation (Phantom Center suppression)
        float processedMid = mid * (1.0f - currentAlpha);

        // 6. Resynthesis of instrumental/attenuated audio
        float outL = bassL + highsL + (processedMid + side);
        float outR = bassR + highsR + (processedMid - side);

        // 7. Live Microphone processing chain
        float rawMic = micInput ? micInput[i] : 0.0f;
        blockPeakMic = std::max(blockPeakMic, std::abs(rawMic));

        // 7a. Butterworth HPF at 85 Hz (eliminate rumble/plosives)
        float micHpf = micHpFilter_.process(rawMic);

        // 7b. Soft Noise Gate (-45 dBFS threshold)
        float micProcessed = micHpf;
        if (useNoiseGate) {
            float instantPower = micHpf * micHpf;
            micRmsEnv_ = 0.995f * micRmsEnv_ + 0.005f * instantPower;
            float rmsDb = 10.0f * std::log10(std::max(micRmsEnv_, 1e-9f));
            if (rmsDb < -45.0f) {
                float gateRatio = std::clamp((rmsDb - (-60.0f)) / 15.0f, 0.0f, 1.0f);
                micProcessed *= gateRatio;
            }
        }

        // 7c. Algorithmic Reverb
        float micWithReverb = micReverb_.process(micProcessed, currentReverb);

        // 7d. Mic gain amplification
        float finalMic = micWithReverb * currentMicGain;

        // 8. Monitored Sum & True-Peak Soft Clipping Limiter
        outL += finalMic;
        outR += finalMic;

        outputStereoInterleaved[i * 2]     = softClip(outL);
        outputStereoInterleaved[i * 2 + 1] = softClip(outR);
    }

    peakMusic_.store(blockPeakMusic, std::memory_order_relaxed);
    peakMic_.store(blockPeakMic, std::memory_order_relaxed);
}
