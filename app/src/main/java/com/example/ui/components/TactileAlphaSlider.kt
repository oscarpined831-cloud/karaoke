package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple

@Composable
fun TactileAlphaSlider(
    currentAlpha: Float,
    onAlphaChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tactile_alpha_slider_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Value Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VOCAL SUPPRESSION FACTOR (α)",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "|Y(m,k)| = |X(m,k)| · [1.0 - α · M_v(m,k)]",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Numeric readout
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "α = ${String.format("%.2f", currentAlpha)} (${(currentAlpha * 100).toInt()}%)",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Continuous Slider
            Slider(
                value = currentAlpha,
                onValueChange = onAlphaChanged,
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alpha_continuous_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = VocalPurple,
                    inactiveTrackColor = Color(0xFF0F172A)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tactile Preset Chips (0.0, 0.25, 0.50, 0.75, 1.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PresetChip(
                    label = "0.0: Original",
                    targetAlpha = 0.0f,
                    isSelected = currentAlpha < 0.05f,
                    onClick = { onAlphaChanged(0.0f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = "0.25: Guide",
                    targetAlpha = 0.25f,
                    isSelected = currentAlpha in 0.20f..0.30f,
                    onClick = { onAlphaChanged(0.25f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = "0.50: Half",
                    targetAlpha = 0.50f,
                    isSelected = currentAlpha in 0.45f..0.55f,
                    onClick = { onAlphaChanged(0.50f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = "0.75: Cut",
                    targetAlpha = 0.75f,
                    isSelected = currentAlpha in 0.70f..0.80f,
                    onClick = { onAlphaChanged(0.75f) },
                    modifier = Modifier.weight(1f)
                )
                PresetChip(
                    label = "1.0: Full",
                    targetAlpha = 1.0f,
                    isSelected = currentAlpha > 0.95f,
                    onClick = { onAlphaChanged(1.0f) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    targetAlpha: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .background(
                if (isSelected) VocalPurple.copy(alpha = 0.25f) else Color(0xFF0F172A),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) NeonCyan else Color(0xFF334155),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}
