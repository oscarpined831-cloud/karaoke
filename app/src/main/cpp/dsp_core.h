#pragma once

#include <atomic>
#include <vector>
#include <cmath>
#include <algorithm>
#include <cstddef>

constexpr float DSP_PI = 3.14159265358979323846f;

// Second-order Biquad IIR filter for real-time crossover & conditioning
class BiquadFilter {
public:
    void setupLowPass(float sampleRate, float cutoffFreq, float q = 0.7071f);
    void setupHighPass(float sampleRate, float cutoffFreq, float q = 0.7071f);
    inline float process(float in) {
        float out = in * b0_ + z1_;
        z1_ = in * b1_ - a1_ * out + z2_;
        z2_ = in * b2_ - a2_ * out;
        return out;
    }
    void reset();

private:
    float b0_{1.0f}, b1_{0.0f}, b2_{0.0f}, a1_{0.0f}, a2_{0.0f};
    float z1_{0.0f}, z2_{0.0f};
};

// Algorithmic Comb Filter for Schroeder Reverb
class CombFilter {
public:
    void init(size_t bufferSize, float feedback, float damping);
    float process(float input);
    void setFeedback(float feedback) { feedback_ = feedback; }
    void setDamping(float damping) { damping_ = damping; }

private:
    std::vector<float> buffer_;
    size_t index_{0};
    float feedback_{0.8f};
    float damping_{0.2f};
    float filterStore_{0.0f};
};

// Algorithmic All-pass Filter for Schroeder Reverb
class AllPassFilter {
public:
    void init(size_t bufferSize, float feedback);
    float process(float input);

private:
    std::vector<float> buffer_;
    size_t index_{0};
    float feedback_{0.5f};
};

// Algorithmic Reverb (Freeverb core)
class FreeverbReverb {
public:
    void setup(float sampleRate);
    float process(float input, float wetLevel);

private:
    CombFilter combs_[4];
    AllPassFilter allpasses_[2];
};

// Central Real-Time Parametric Vocal Isolation & Low-Latency Mixer Engine
class RealTimeKaraokeEngine {
public:
    RealTimeKaraokeEngine(float sampleRate);

    void setVocalSuppressionAlpha(float alpha);
    void setMicrophoneGain(float gain);
    void setReverbLevel(float wetLevel);
    void setNoiseGateEnabled(bool enabled);

    void processAudioBlock(const float* musicInputL, const float* musicInputR,
                           const float* micInput, float* outputStereoInterleaved,
                           size_t frameCount);

    float getCurrentAlpha() const { return alphaFactor_.load(std::memory_order_relaxed); }
    float getCurrentMicGain() const { return micGain_.load(std::memory_order_relaxed); }
    float getPeakMusic() const { return peakMusic_.load(std::memory_order_relaxed); }
    float getPeakMic() const { return peakMic_.load(std::memory_order_relaxed); }

private:
    inline float softClip(float in) {
        if (in <= -1.0f) return -1.0f;
        if (in >= 1.0f)  return 1.0f;
        return in - (std::pow(in, 3.0f) / 3.0f);
    }

    float sampleRate_;
    std::atomic<float> alphaFactor_{0.0f};
    std::atomic<float> micGain_{1.2f};
    std::atomic<float> reverbWet_{0.35f};
    std::atomic<bool> noiseGateEnabled_{true};

    std::atomic<float> peakMusic_{0.0f};
    std::atomic<float> peakMic_{0.0f};

    BiquadFilter lpFilterL_, lpFilterR_;
    BiquadFilter hpFilterL_, hpFilterR_;
    BiquadFilter micHpFilter_;
    FreeverbReverb micReverb_;

    float micRmsEnv_{0.0f};
};
