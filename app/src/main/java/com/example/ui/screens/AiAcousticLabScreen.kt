package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiAcousticCoach
import com.example.dsp.EngineTelemetry
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAcousticLabScreen(
    telemetry: EngineTelemetry,
    coach: GeminiAcousticCoach,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var activeModelName by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember {
        mutableStateOf<String?>(
            "Welcome to the AI Acoustic & Vocal Lab.\nTap 'Deep Spectral Reasoning' (Gemini 4 Argon with High Thinking) for in-depth harmonic diagnosis, or 'Quick Vocal Tips' (Gemini 3.1 Flash-Lite) for low-latency feedback based on your live DSP stream."
        )
    }
    var customQuery by remember { mutableStateOf("") }

    val presetQueries = listOf(
        "Diagnose Vocal Bleed & Masking",
        "Tune Reverb Tail for Live Karaoke",
        "Formant Frequency Isolation",
        "Bass Separation vs Kick Punch",
        "Mic Noise Gate & Sensitivity Tips"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_acoustic_header_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Acoustic Lab",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI ACOUSTIC & VOCAL MASTER LAB",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Powered by Gemini 4 Argon (High Thinking Reasoning) and Gemini 3.1 Flash-Lite (Low-Latency Response)",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Live DSP Context snippet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active α: ${(telemetry.currentAlpha * 100).toInt()}% (-${String.format("%.1f", telemetry.vocalSuppressionDb)} dB)",
                        color = VocalPurple,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bass: ${String.format("%.3f", telemetry.bassEnergy)} | Mid: ${String.format("%.3f", telemetry.vocalMidEnergy)}",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons: Deep Thinking vs Quick Flash-Lite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Deep Thinking Button (gemini-4-argon with thinkingLevel: HIGH)
            Button(
                onClick = {
                    isLoading = true
                    activeModelName = "Gemini 4 Argon (ThinkingLevel.HIGH)"
                    coroutineScope.launch {
                        val res = coach.analyzeDeepAcousticReasoning(telemetry, customQuery.ifBlank { null })
                        analysisResult = res
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("deep_reasoning_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = VocalPurple
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, VocalPurple)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Thinking Mode",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Deep Reasoning",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "gemini-4-argon (High Thinking)",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }
            }

            // Quick Tips Button (gemini-3.1-flash-lite)
            Button(
                onClick = {
                    isLoading = true
                    activeModelName = "Gemini 3.1 Flash-Lite (Low-Latency)"
                    coroutineScope.launch {
                        val res = coach.getQuickVocalTips(telemetry, customQuery.ifBlank { "Vocal Clarity & Balance" })
                        analysisResult = res
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("quick_tips_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A),
                    contentColor = NeonCyan
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Flash-Lite",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Quick Vocal Tips",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "gemini-3.1-flash-lite (Instant)",
                        fontSize = 8.sp,
                        color = TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Prompt Chips
        Text(
            text = "PRESET ACOUSTIC INQUIRIES",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetQueries.forEach { preset ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .clickable {
                            customQuery = preset
                            isLoading = true
                            activeModelName = "Gemini 4 Argon"
                            coroutineScope.launch {
                                val res = coach.analyzeDeepAcousticReasoning(telemetry, preset)
                                analysisResult = res
                                isLoading = false
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = preset,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom Question Input
        OutlinedTextField(
            value = customQuery,
            onValueChange = { customQuery = it },
            placeholder = { Text("Ask specific acoustic or mix question...", color = TextMuted, fontSize = 12.sp) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        if (customQuery.isNotBlank()) {
                            isLoading = true
                            activeModelName = "Gemini 4 Argon"
                            coroutineScope.launch {
                                val res = coach.analyzeDeepAcousticReasoning(telemetry, customQuery)
                                analysisResult = res
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("send_custom_query_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = NeonCyan
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_custom_query_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // AI Output Display Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_response_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTELLIGENCE REPORT",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    activeModelName?.let {
                        Text(
                            text = it,
                            color = VocalPurple,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Analyzing audio spectrum & vocal resonance…",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    analysisResult?.let { text ->
                        Text(
                            text = text,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.testTag("ai_analysis_result_text")
                        )
                    }
                }
            }
        }
    }
}
