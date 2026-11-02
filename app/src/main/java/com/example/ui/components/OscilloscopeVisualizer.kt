package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.EngineTelemetry
import com.example.ui.theme.MeterGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.PeakRed
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple

@Composable
fun OscilloscopeVisualizer(
    telemetry: EngineTelemetry,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("oscilloscope_visualizer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Live Oscilloscope title and status badges
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
                                if (isPlaying) NeonCyan else PeakAmber,
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REAL-TIME SPECTRAL OSCILLOSCOPE",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Vocal Attenuation badge
                Box(
                    modifier = Modifier
                        .background(VocalPurple.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, VocalPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Vocal Cut: -${String.format("%.1f", telemetry.vocalSuppressionDb)} dB",
                        color = VocalPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Canvas: Oscilloscope waveform & 3-Band Spectral bars
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(StudioObsidian, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerY = h / 2f

                    // 1. Grid background lines
                    val gridColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                    drawLine(gridColor, Offset(0f, centerY), Offset(w, centerY), strokeWidth = 1f)
                    drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 0.5f)
                    drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 0.5f)

                    for (xStep in 0..8) {
                        val x = w * (xStep / 8f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 0.5f)
                    }

                    // 2. Multiband Energy Columns (Low <250Hz, Mid 250-4000Hz, High >4000Hz)
                    val barWidth = 18.dp.toPx()
                    val bassHeight = (telemetry.bassEnergy * h * 2.5f).coerceIn(4f, h - 8f)
                    val midHeight = (telemetry.vocalMidEnergy * h * 2.5f).coerceIn(4f, h - 8f)
                    val highHeight = (telemetry.highEnergy * h * 2.5f).coerceIn(4f, h - 8f)

                    // Bass bar (< 250Hz) - Cyan
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(NeonCyan, Color(0xFF0369A1))),
                        topLeft = Offset(16.dp.toPx(), h - bassHeight),
                        size = Size(barWidth, bassHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Vocal Mid bar (250Hz - 4000Hz) - Purple
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(VocalPurple, Color(0xFF6B21A8))),
                        topLeft = Offset(42.dp.toPx(), h - midHeight),
                        size = Size(barWidth, midHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // High bar (> 4000Hz) - Amber
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(PeakAmber, Color(0xFFB45309))),
                        topLeft = Offset(68.dp.toPx(), h - highHeight),
                        size = Size(barWidth, highHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // 3. Real-time Output Oscilloscope Trace
                    val samples = telemetry.waveformSamples
                    if (samples.isNotEmpty()) {
                        val path = Path()
                        val startX = 100.dp.toPx()
                        val availableWaveWidth = w - startX

                        val stepX = availableWaveWidth / (samples.size - 1)
                        path.moveTo(startX, centerY + samples[0] * centerY * 0.9f)

                        for (i in 1 until samples.size) {
                            val x = startX + i * stepX
                            val y = (centerY + samples[i] * centerY * 0.85f).coerceIn(2f, h - 2f)
                            path.lineTo(x, y)
                        }

                        // Glow layer
                        drawPath(
                            path = path,
                            color = NeonCyan.copy(alpha = 0.35f),
                            style = Stroke(width = 5.dp.toPx())
                        )
                        // Sharp trace line
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // VU Peak Meters & Band Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Legends for the spectral bands
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BandLegend(color = NeonCyan, label = "Bass (<250Hz)")
                    Spacer(modifier = Modifier.width(10.dp))
                    BandLegend(color = VocalPurple, label = "Vocal Mid (250-4k)")
                    Spacer(modifier = Modifier.width(10.dp))
                    BandLegend(color = PeakAmber, label = "Highs (>4k)")
                }

                // VU Meter peak indicator
                VuPeakPill(label = "OUT", peak = telemetry.peakOutput)
            }
        }
    }
}

@Composable
private fun BandLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun VuPeakPill(label: String, peak: Float) {
    val meterColor = when {
        peak > 0.95f -> PeakRed
        peak > 0.70f -> PeakAmber
        else -> MeterGreen
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
            .border(1.dp, meterColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label: ",
            color = TextMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${(peak * 100).toInt().coerceIn(0, 100)}%",
            color = meterColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
