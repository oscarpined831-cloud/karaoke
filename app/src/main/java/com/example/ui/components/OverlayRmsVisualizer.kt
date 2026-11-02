package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MeterGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.PeakRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple
import kotlin.math.log10
import kotlin.math.max

/**
 * OverlayRmsVisualizer: Real-time dual RMS audio level meter designed specifically
 * for the floating overlay window. Displays smooth, ballistic RMS energy
 * for both Music playback and Microphone input.
 */
@Composable
fun OverlayRmsVisualizer(
    rmsMusic: Float,
    rmsMic: Float,
    isMicActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Normalization: raw RMS [0.0 .. 1.0] mapped with logarithmic perceived response
    // Dynamic range 0 dBFS down to -50 dBFS
    val musicLevelNorm = remember(rmsMusic) {
        if (rmsMusic > 0.0001f) {
            val db = 20.0f * log10(rmsMusic.coerceIn(0.0001f, 1.0f))
            ((db + 50.0f) / 50.0f).coerceIn(0.0f, 1.0f)
        } else 0.0f
    }

    val micLevelNorm = remember(rmsMic, isMicActive) {
        if (isMicActive && rmsMic > 0.0001f) {
            val db = 20.0f * log10(rmsMic.coerceIn(0.0001f, 1.0f))
            ((db + 50.0f) / 50.0f).coerceIn(0.0f, 1.0f)
        } else 0.0f
    }

    // Smooth ballistic animation (fast attack ~60ms, smooth decay)
    val animatedMusicLevel by animateFloatAsState(
        targetValue = musicLevelNorm,
        animationSpec = tween(
            durationMillis = if (musicLevelNorm > 0.1f) 65 else 180,
            easing = LinearOutSlowInEasing
        ),
        label = "animated_music_rms"
    )

    val animatedMicLevel by animateFloatAsState(
        targetValue = micLevelNorm,
        animationSpec = tween(
            durationMillis = if (micLevelNorm > 0.1f) 65 else 180,
            easing = LinearOutSlowInEasing
        ),
        label = "animated_mic_rms"
    )

    // Peak hold tracking with smooth decay
    var peakHoldMusic by remember { mutableFloatStateOf(0.0f) }
    var peakHoldMic by remember { mutableFloatStateOf(0.0f) }

    LaunchedEffect(musicLevelNorm) {
        if (musicLevelNorm > peakHoldMusic) {
            peakHoldMusic = musicLevelNorm
        } else {
            peakHoldMusic = max(musicLevelNorm, peakHoldMusic * 0.94f)
        }
    }

    LaunchedEffect(micLevelNorm) {
        if (micLevelNorm > peakHoldMic) {
            peakHoldMic = micLevelNorm
        } else {
            peakHoldMic = max(micLevelNorm, peakHoldMic * 0.94f)
        }
    }

    val animatedPeakMusic by animateFloatAsState(
        targetValue = peakHoldMusic,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "peak_music_anim"
    )

    val animatedPeakMic by animateFloatAsState(
        targetValue = peakHoldMic,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "peak_mic_anim"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF070B14), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag("overlay_rms_visualizer")
    ) {
        // Meter Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "REAL-TIME RMS MONITORS",
                color = TextSecondary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "-48dB  ·  -12dB  ·  0dB",
                color = TextMuted,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        // 1. Music Input RMS Bar
        RmsChannelRow(
            channelName = "MUSIC",
            icon = Icons.Default.MusicNote,
            level = animatedMusicLevel,
            peakHold = animatedPeakMusic,
            rawRms = rmsMusic,
            gradientColors = listOf(NeonCyan, Color(0xFF0284C7)),
            accentColor = NeonCyan,
            testTag = "overlay_rms_music_meter"
        )

        Spacer(modifier = Modifier.height(5.dp))

        // 2. Microphone Input RMS Bar
        RmsChannelRow(
            channelName = "VOX",
            icon = if (isMicActive) Icons.Default.Mic else Icons.Default.MicOff,
            level = animatedMicLevel,
            peakHold = animatedPeakMic,
            rawRms = if (isMicActive) rmsMic else 0.0f,
            gradientColors = listOf(VocalPurple, Color(0xFFEC4899)),
            accentColor = VocalPurple,
            testTag = "overlay_rms_mic_meter"
        )
    }
}

@Composable
private fun RmsChannelRow(
    channelName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    level: Float,
    peakHold: Float,
    rawRms: Float,
    gradientColors: List<Color>,
    accentColor: Color,
    testTag: String
) {
    val dbText = remember(rawRms) {
        if (rawRms > 0.0001f) {
            val db = 20.0f * log10(rawRms.coerceIn(0.0001f, 1.0f))
            "${String.format("%.0f", db)} dB"
        } else {
            "-∞ dB"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Label & Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = channelName,
                tint = accentColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = channelName,
                color = TextPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Animated RMS Level Bar with Peak Hold line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(4.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val activeWidth = (level.coerceIn(0f, 1f) * w)

                // Fill gradient level
                if (activeWidth > 0f) {
                    val brush = Brush.horizontalGradient(
                        colors = listOf(
                            gradientColors.first(),
                            gradientColors.last(),
                            if (level > 0.85f) PeakRed else if (level > 0.70f) PeakAmber else gradientColors.last()
                        ),
                        startX = 0f,
                        endX = w
                    )
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset.Zero,
                        size = Size(activeWidth, h),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }

                // Tick marks at -24dB (~50%) and -6dB (~88%)
                val tick24 = w * 0.50f
                val tick6 = w * 0.88f
                val tickColor = Color(0x55FFFFFF)
                drawLine(tickColor, Offset(tick24, 0f), Offset(tick24, h), strokeWidth = 1f)
                drawLine(tickColor, Offset(tick6, 0f), Offset(tick6, h), strokeWidth = 1f)

                // Peak hold indicator line
                val peakX = (peakHold.coerceIn(0f, 1f) * (w - 2.dp.toPx()))
                if (peakX > 2.dp.toPx()) {
                    val peakColor = if (peakHold > 0.85f) PeakRed else if (peakHold > 0.70f) PeakAmber else Color.White
                    drawRoundRect(
                        color = peakColor,
                        topLeft = Offset(peakX, 0f),
                        size = Size(2.dp.toPx(), h),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Live dBFS readout
        Text(
            text = dbText,
            color = if (level > 0.85f) PeakRed else if (level > 0.70f) PeakAmber else TextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
    }
}

/**
 * Compact mini dual-meter for the collapsed header bar of the overlay
 */
@Composable
fun CompactOverlayRmsPill(
    rmsMusic: Float,
    rmsMic: Float,
    modifier: Modifier = Modifier
) {
    val musicLevelNorm = remember(rmsMusic) {
        if (rmsMusic > 0.0001f) {
            val db = 20.0f * log10(rmsMusic.coerceIn(0.0001f, 1.0f))
            ((db + 50.0f) / 50.0f).coerceIn(0.0f, 1.0f)
        } else 0.0f
    }

    val micLevelNorm = remember(rmsMic) {
        if (rmsMic > 0.0001f) {
            val db = 20.0f * log10(rmsMic.coerceIn(0.0001f, 1.0f))
            ((db + 50.0f) / 50.0f).coerceIn(0.0f, 1.0f)
        } else 0.0f
    }

    val animatedMusic by animateFloatAsState(
        targetValue = musicLevelNorm,
        animationSpec = tween(durationMillis = 65, easing = LinearOutSlowInEasing),
        label = "compact_music_rms"
    )

    val animatedMic by animateFloatAsState(
        targetValue = micLevelNorm,
        animationSpec = tween(durationMillis = 65, easing = LinearOutSlowInEasing),
        label = "compact_mic_rms"
    )

    Row(
        modifier = modifier
            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Music mini vertical bar
        MiniVerticalBar(level = animatedMusic, color = NeonCyan)
        // Mic mini vertical bar
        MiniVerticalBar(level = animatedMic, color = VocalPurple)
    }
}

@Composable
private fun MiniVerticalBar(level: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(3.dp)
            .height(14.dp)
            .background(Color(0xFF1E293B), RoundedCornerShape(1.5.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(level.coerceIn(0.05f, 1.0f))
                .background(
                    if (level > 0.85f) PeakRed else if (level > 0.70f) PeakAmber else color,
                    RoundedCornerShape(1.5.dp)
                )
        )
    }
}
