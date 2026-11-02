package com.example.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple

@Composable
fun ArchitectureSpecScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("architecture_spec_screen")
    ) {
        // Architecture Identifier Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VOX-ENGINE-ZERO-HW",
                color = NeonCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .background(MeterGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "C++20 / AAudio / SIMD",
                    color = MeterGreen,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 1: Mathematical Foundation (STFT & Soft Mask)
        SpecSectionCard(
            title = "1. STFT & PARAMETRIC VOCAL ATTENUATION",
            icon = Icons.Default.Functions,
            color = NeonCyan
        ) {
            Text(
                text = "Mathematical attenuation formula:",
                color = TextSecondary,
                fontSize = 11.sp
            )
            FormulaBox("|Y(m, k)| = |X(m, k)| · [ 1.0 - α · M_v(m, k) ]")

            Text(
                text = "• α = 0.0 ⟹ Full original vocal pass-through\n" +
                        "• α = 0.5 ⟹ 50% Vocal attenuation (Vocal rehearsal guide)\n" +
                        "• α = 1.0 ⟹ Total phantom center vocal cancellation",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Psychoacoustic noise floor preservation:",
                color = TextSecondary,
                fontSize = 11.sp
            )
            FormulaBox("|Y_final(m,k)| = max( |Y(m,k)|, Φ(k) · |X(m,k)| )\nΦ(k) = β0 + β1 · (k · fs / N)^(-1)")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 2: Multiband Mid/Side Crossover
        SpecSectionCard(
            title = "2. 3-BAND MID/SIDE SPECTRAL CROSSOVER",
            icon = Icons.Default.Memory,
            color = VocalPurple
        ) {
            Text(
                text = "Deterministic, zero-latency 3-band division:",
                color = TextSecondary,
                fontSize = 11.sp
            )
            FormulaBox(
                "• Bass (<250 Hz): (L + R)/2 [Mono punch preservation]\n" +
                "• Vocal Mid (250-4000 Hz):\n" +
                "    Mid = (L + R)/2 , Side = (L - R)/2\n" +
                "    Mid_out = (1.0 - α) · Mid\n" +
                "    L_mid = Mid_out + Side, R_mid = Mid_out - Side\n" +
                "• Highs (>4000 Hz): L, R [Stereo transient & room preservation]"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 3: Live Mic Conditioning & True-Peak Limiter
        SpecSectionCard(
            title = "3. LOW-LATENCY MIC DSP & TRUE-PEAK LIMITER",
            icon = Icons.Default.Code,
            color = PeakAmber
        ) {
            Text(
                text = "Signal conditioning chain:",
                color = TextSecondary,
                fontSize = 11.sp
            )
            FormulaBox(
                "1. HPF Butterworth 2nd Order: fc = 85 Hz (anti-plosive)\n" +
                "2. Soft Noise Gate: Threshold = -45 dBFS\n" +
                "3. Freeverb Schroeder Reverb: 4 comb filters + 2 all-pass (T60 ≈ 0.8s)\n" +
                "4. True-Peak Cubic Soft-Clipping Limiter:\n" +
                "    S_lim[n] = sgn(x) · (3 - (2 - 3|x|)^2) / 3   when |x| > 2/3"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Card 4: Thread Topology
        SpecSectionCard(
            title = "4. LOCK-FREE SPSC THREAD TOPOLOGY",
            icon = Icons.Default.Memory,
            color = MeterGreen
        ) {
            FormulaBox(
                "Thread 1 (Ingest):     Spotify / File ──► RingBuffer A (Lock-free)\n" +
                "Thread 2 (Mic):        AudioRecord    ──► RingBuffer B (Lock-free)\n" +
                "Thread 3 (DSP Core):   Reads A & B    ──► DSP M/S + Mix ──► RingBuffer C\n" +
                "Thread 4 (Dispatcher): AudioTrack A2DP consumes C (0.00 ms relative drift)\n" +
                "Thread 5 (UI Overlay): atomic<float> alpha injected into Thread 3"
            )
        }
    }
}

@Composable
private fun SpecSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun FormulaBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFF070B12), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = text,
            color = NeonCyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 15.sp
        )
    }
}
