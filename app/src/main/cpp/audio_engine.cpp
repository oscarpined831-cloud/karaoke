#include <jni.h>
#include <memory>
#include "dsp_core.h"

static std::unique_ptr<RealTimeKaraokeEngine> sKaraokeEngine;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_service_NativeAudioBridge_initEngine(JNIEnv* /*env*/, jobject /*thiz*/, jfloat sampleRate) {
    sKaraokeEngine = std::make_unique<RealTimeKaraokeEngine>(sampleRate);
}

JNIEXPORT void JNICALL
Java_com_example_service_NativeAudioBridge_setAlpha(JNIEnv* /*env*/, jobject /*thiz*/, jfloat alpha) {
    if (sKaraokeEngine) {
        sKaraokeEngine->setVocalSuppressionAlpha(alpha);
    }
}

JNIEXPORT void JNICALL
Java_com_example_service_NativeAudioBridge_setMicGain(JNIEnv* /*env*/, jobject /*thiz*/, jfloat gain) {
    if (sKaraokeEngine) {
        sKaraokeEngine->setMicrophoneGain(gain);
    }
}

JNIEXPORT void JNICALL
Java_com_example_service_NativeAudioBridge_setReverb(JNIEnv* /*env*/, jobject /*thiz*/, jfloat reverb) {
    if (sKaraokeEngine) {
        sKaraokeEngine->setReverbLevel(reverb);
    }
}

JNIEXPORT void JNICALL
Java_com_example_service_NativeAudioBridge_setNoiseGate(JNIEnv* /*env*/, jobject /*thiz*/, jboolean enabled) {
    if (sKaraokeEngine) {
        sKaraokeEngine->setNoiseGateEnabled(enabled);
    }
}

JNIEXPORT void JNICALL
Java_com_example_service_NativeAudioBridge_processBlock(
    JNIEnv* env, jobject /*thiz*/,
    jfloatArray musicL, jfloatArray musicR,
    jfloatArray mic, jfloatArray outputStereo,
    jint frameCount) {

    if (!sKaraokeEngine) return;

    jfloat* pMusicL = musicL ? env->GetFloatArrayElements(musicL, nullptr) : nullptr;
    jfloat* pMusicR = musicR ? env->GetFloatArrayElements(musicR, nullptr) : nullptr;
    jfloat* pMic    = mic    ? env->GetFloatArrayElements(mic, nullptr)    : nullptr;
    jfloat* pOut    = env->GetFloatArrayElements(outputStereo, nullptr);

    sKaraokeEngine->processAudioBlock(pMusicL, pMusicR, pMic, pOut, static_cast<size_t>(frameCount));

    if (pMusicL) env->ReleaseFloatArrayElements(musicL, pMusicL, JNI_ABORT);
    if (pMusicR) env->ReleaseFloatArrayElements(musicR, pMusicR, JNI_ABORT);
    if (pMic)    env->ReleaseFloatArrayElements(mic, pMic, JNI_ABORT);
    env->ReleaseFloatArrayElements(outputStereo, pOut, 0);
}

}
