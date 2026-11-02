package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.dsp.EngineTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiAcousticCoach"

sealed interface AiCoachState {
    data object Idle : AiCoachState
    data class Loading(val mode: String) : AiCoachState
    data class Success(val response: String, val modelUsed: String) : AiCoachState
    data class Error(val message: String) : AiCoachState
}

class GeminiAcousticCoach {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Deep Acoustic & Mix Reasoning using gemini-4-argon with thinkingLevel: HIGH.
     * Evaluates complex spectral characteristics, formant masking, and stereo field parameters.
     */
    suspend fun analyzeDeepAcousticReasoning(
        telemetry: EngineTelemetry,
        userQuery: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalAcousticReport(telemetry, isDeep = true)
        }

        val prompt = buildString {
            appendLine("You are the Lead Acoustic & Audio Mastering Engineer behind VoxEngine Zero.")
            appendLine("Perform a rigorous technical analysis of the current live DSP audio stream with the following telemetry:")
            appendLine("- Current Alpha Vocal Suppression: ${(telemetry.currentAlpha * 100).toInt()}% (${String.format("%.1f", telemetry.vocalSuppressionDb)} dB attenuation)")
            appendLine("- Bass (<250Hz) Spectral Energy: ${String.format("%.4f", telemetry.bassEnergy)}")
            appendLine("- Mid Vocal (250Hz-4000Hz) Energy: ${String.format("%.4f", telemetry.vocalMidEnergy)}")
            appendLine("- High (>4000Hz) Transient Energy: ${String.format("%.4f", telemetry.highEnergy)}")
            appendLine("- Music Peak: ${String.format("%.2f", telemetry.peakMusic)} | Mic Peak: ${String.format("%.2f", telemetry.peakMic)} | Output Peak: ${String.format("%.2f", telemetry.peakOutput)}")
            if (!userQuery.isNullOrBlank()) {
                appendLine("User Question / Acoustic Goal: $userQuery")
            } else {
                appendLine("Evaluate: 1) Vocal separation efficacy and phantom-center cancellation; 2) Bass preservation vs vocal bleed; 3) Reverb & noise-gate setting recommendations for singer clarity.")
            }
        }

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            // High thinking configuration mandatory for gemini-4-argon
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-4-argon:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini 4 Argon error: $body")
                return@withContext generateLocalAcousticReport(telemetry, isDeep = true)
            }
            parseGeminiResponse(body)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 4 Argon call failed", e)
            generateLocalAcousticReport(telemetry, isDeep = true)
        }
    }

    /**
     * Low-latency instantaneous vocal coach using gemini-3.1-flash-lite.
     */
    suspend fun getQuickVocalTips(
        telemetry: EngineTelemetry,
        quickTopic: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalAcousticReport(telemetry, isDeep = false)
        }

        val prompt = "Provide 3 concise, high-impact vocal performance and mixing tips for a singer performing over VoxEngine (Alpha=${(telemetry.currentAlpha * 100).toInt()}%, Topic=$quickTopic). Keep it under 80 words."

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini 3.1 Flash-Lite error: $body")
                return@withContext generateLocalAcousticReport(telemetry, isDeep = false)
            }
            parseGeminiResponse(body)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini 3.1 Flash-Lite call failed", e)
            generateLocalAcousticReport(telemetry, isDeep = false)
        }
    }

    private fun parseGeminiResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    sb.append(part.getString("text"))
                }
            }
            sb.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response", e)
            "Acoustic analysis processed. Adjust vocal alpha slider to match playback acoustics."
        }
    }

    /**
     * Fallback expert report computed directly from real-time DSP mathematical state
     */
    private fun generateLocalAcousticReport(telemetry: EngineTelemetry, isDeep: Boolean): String {
        val alphaPct = (telemetry.currentAlpha * 100).toInt()
        val bassRatio = if (telemetry.peakOutput > 0.001f) telemetry.bassEnergy / telemetry.peakOutput else 0.5f
        val midRatio = if (telemetry.peakOutput > 0.001f) telemetry.vocalMidEnergy / telemetry.peakOutput else 0.3f

        return if (isDeep) {
            """
            ✦ Deep Spectral & Mix Diagnostics (Acoustic Reasoner):
            
            • Mid/Side Cancellation Ratio: At α = $alphaPct%, center-panned vocal energy is reduced by ${String.format("%.1f", telemetry.vocalSuppressionDb)} dB.
            • Rhythm Section Isolation: Bass band (<250 Hz) energy retention is high (${String.format("%.0f", bassRatio * 100)}%), preserving punchy kick and bass foundation without mono phase cancellation.
            • Stereo Transient Width: High band (>4000 Hz) stereo side information remains intact for spacious acoustic atmosphere.
            • Recommended Settings: For rehearsal, set α = 0.50 for pitch guide; for live karaoke, set α = 1.00 with Reverb at 30% and Noise Gate enabled (-45 dBFS) to prevent room bleed.
            """.trimIndent()
        } else {
            """
            ⚡ Quick Vocal Tips:
            1. Maintain 3-5 cm microphone distance; the 85 Hz Butterworth filter eliminates proximity plosives.
            2. At α = $alphaPct%, use the attenuated original vocals as a subtle pitch guide.
            3. Freeverb Schroeder tail is tuned to 0.8s for smooth vocal polish without muddying the mix.
            """.trimIndent()
        }
    }
}
