package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeterGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple

@Composable
fun MicDspRack(
    micGain: Float,
    onMicGainChanged: (Float) -> Unit,
    reverbWet: Float,
    onReverbChanged: (Float) -> Unit,
    hpfEnabled: Boolean,
    onHpfToggled: (Boolean) -> Unit,
    noiseGateEnabled: Boolean,
    onNoiseGateToggled: (Boolean) -> Unit,
    micCaptureEnabled: Boolean,
    onMicCaptureToggled: (Boolean) -> Unit,
    micPeak: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mic_dsp_rack_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with mic mute toggle and live meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (micCaptureEnabled) MeterGreen else Color(0xFFEF4444),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIVE VOCAL CONDITIONING RACK",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Mic Peak Meter readout
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .border(1.dp, MeterGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "MIC: ${(micPeak * 100).toInt()}%",
                            color = MeterGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { onMicCaptureToggled(!micCaptureEnabled) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (micCaptureEnabled) NeonCyan.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (micCaptureEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mute/Unmute Mic",
                            tint = if (micCaptureEnabled) NeonCyan else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Microphone Gain Slider (gamma)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mic Gain (γ)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${String.format("%.1f", micGain)}x",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = micGain,
                onValueChange = onMicGainChanged,
                valueRange = 0f..2.5f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.height(30.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Algorithmic Reverb Wet Mix Slider (Freeverb / Schroeder)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Freeverb Algorithmic Reverb (T60 ≈ 0.8s)",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(reverbWet * 100).toInt()}%",
                    color = VocalPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Slider(
                value = reverbWet,
                onValueChange = onReverbChanged,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = VocalPurple,
                    activeTrackColor = VocalPurple,
                    inactiveTrackColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.height(30.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Toggles: Butterworth HPF (85Hz) and Soft Noise Gate (-45 dBFS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Butterworth HPF Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .border(1.dp, if (hpfEnabled) NeonCyan.copy(alpha = 0.5f) else Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HPF 85 Hz",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Butterworth 2nd",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = hpfEnabled,
                            onCheckedChange = onHpfToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                // Soft Noise Gate Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .border(1.dp, if (noiseGateEnabled) VocalPurple.copy(alpha = 0.5f) else Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Noise Gate",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-45 dBFS Threshold",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Switch(
                            checked = noiseGateEnabled,
                            onCheckedChange = onNoiseGateToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VocalPurple,
                                checkedTrackColor = VocalPurple.copy(alpha = 0.3f),
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }
        }
    }
}
