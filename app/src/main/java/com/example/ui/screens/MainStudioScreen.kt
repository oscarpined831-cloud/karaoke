package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiAcousticCoach
import com.example.dsp.AudioFileDecoder
import com.example.service.OverlayController
import com.example.service.VoxEngineAudioService
import com.example.ui.components.AudioSourceCard
import com.example.ui.components.BluetoothSyncStatusCard
import com.example.ui.components.MicDspRack
import com.example.ui.components.OscilloscopeVisualizer
import com.example.ui.components.TactileAlphaSlider
import com.example.ui.theme.MeterGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PeakAmber
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VocalPurple
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudioScreen(
    service: VoxEngineAudioService?,
    overlayController: OverlayController?,
    coach: GeminiAcousticCoach,
    onStartService: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isOverlayActive by remember { mutableStateOf(overlayController?.isShowing() == true) }

    // Service State flows
    val isPlaying = service?.isPlayingState?.collectAsState()?.value ?: false
    val activeTrackName = service?.activeTrackName?.collectAsState()?.value ?: "Synth Multi-Stem (Center Vocal)"
    val telemetry = service?.telemetryState?.collectAsState()?.value ?: com.example.dsp.EngineTelemetry()

    // Engine DSP Parameters state
    var currentAlpha by remember(telemetry.currentAlpha) { mutableStateOf(service?.engine?.getVocalSuppressionAlpha() ?: 0.0f) }
    var micGain by remember { mutableStateOf(service?.engine?.getMicrophoneGain() ?: 1.2f) }
    var reverbWet by remember { mutableStateOf(service?.engine?.getReverbLevel() ?: 0.30f) }
    var hpfEnabled by remember { mutableStateOf(service?.engine?.isHpfEnabled() ?: true) }
    var noiseGateEnabled by remember { mutableStateOf(service?.engine?.isNoiseGateEnabled() ?: true) }
    var micCaptureEnabled by remember { mutableStateOf(service?.isMicCaptureEnabled() ?: true) }

    // Permissions launcher for RECORD_AUDIO & POST_NOTIFICATIONS
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordGranted) {
            Toast.makeText(context, "Microphone access enabled for low-latency DSP", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && service != null) {
            coroutineScope.launch {
                Toast.makeText(context, "Decoding audio file into 32-bit PCM...", Toast.LENGTH_SHORT).show()
                val decoded = AudioFileDecoder.decodeUri(context, uri)
                if (decoded != null) {
                    val displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Custom Track"
                    service.loadCustomAudio(decoded, displayName)
                    Toast.makeText(context, "Loaded $displayName (${String.format("%.1f", decoded.durationSec)}s)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to decode audio. Please check file format.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "VOXENGINE",
                            color = NeonCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(VocalPurple.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                                .border(1.dp, VocalPurple.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ZERO-HW",
                                color = VocalPurple,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                actions = {
                    // Floating overlay shortcut button
                    IconButton(
                        onClick = {
                            if (overlayController != null) {
                                if (!overlayController.canDrawOverlays()) {
                                    overlayController.requestOverlayPermission(context)
                                } else {
                                    if (overlayController.isShowing()) {
                                        overlayController.hideOverlay()
                                        isOverlayActive = false
                                    } else {
                                        overlayController.showOverlay()
                                        isOverlayActive = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("action_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Floating Overlay",
                            tint = if (isOverlayActive) VocalPurple else TextSecondary
                        )
                    }

                    // Main Engine Power Toggle
                    IconButton(
                        onClick = {
                            if (service == null || !isPlaying) {
                                onStartService()
                                service?.startAudioEngine()
                            } else {
                                service.stopAudioEngine()
                            }
                        },
                        modifier = Modifier.testTag("action_engine_power_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Engine Power",
                            tint = if (isPlaying) NeonCyan else Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioObsidian
                )
            )
        },
        containerColor = StudioObsidian
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: DSP Studio, AI Acoustic Lab, Architecture Spec
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = StudioObsidian,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DSP Console", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Acoustic Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Specs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Tab 0: Primary DSP Console
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. Real-time Oscilloscope & Spectral Band Analyzer
                        OscilloscopeVisualizer(
                            telemetry = telemetry,
                            isPlaying = isPlaying
                        )

                        // 2. Hero Tactile Alpha Slider
                        TactileAlphaSlider(
                            currentAlpha = currentAlpha,
                            onAlphaChanged = {
                                currentAlpha = it
                                service?.engine?.setVocalSuppressionAlpha(it)
                            }
                        )

                        // 3. Live Microphone DSP Conditioning Rack
                        MicDspRack(
                            micGain = micGain,
                            onMicGainChanged = {
                                micGain = it
                                service?.engine?.setMicrophoneGain(it)
                            },
                            reverbWet = reverbWet,
                            onReverbChanged = {
                                reverbWet = it
                                service?.engine?.setReverbLevel(it)
                            },
                            hpfEnabled = hpfEnabled,
                            onHpfToggled = {
                                hpfEnabled = it
                                service?.engine?.setHpfEnabled(it)
                            },
                            noiseGateEnabled = noiseGateEnabled,
                            onNoiseGateToggled = {
                                noiseGateEnabled = it
                                service?.engine?.setNoiseGateEnabled(it)
                            },
                            micCaptureEnabled = micCaptureEnabled,
                            onMicCaptureToggled = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                                } else {
                                    micCaptureEnabled = it
                                    service?.setMicCaptureEnabled(it)
                                }
                            },
                            micPeak = telemetry.peakMic
                        )

                        // 4. Audio Ingest Source Card
                        AudioSourceCard(
                            trackName = activeTrackName,
                            isPlaying = isPlaying,
                            onTogglePlay = {
                                if (service == null) {
                                    onStartService()
                                } else {
                                    if (isPlaying) {
                                        service.togglePlayPause()
                                    } else {
                                        service.startAudioEngine()
                                    }
                                }
                            },
                            onSelectDemoTrack = {
                                service?.loadDemoSynthTrack()
                                Toast.makeText(context, "Loaded Built-in Multi-Stem Synth Song", Toast.LENGTH_SHORT).show()
                            },
                            onLoadCustomFile = {
                                filePickerLauncher.launch("audio/*")
                            }
                        )

                        // 5. Bluetooth Unified Sync & Latency Status Card
                        BluetoothSyncStatusCard(
                            isOverlayActive = isOverlayActive,
                            onToggleOverlay = {
                                if (overlayController != null) {
                                    if (!overlayController.canDrawOverlays()) {
                                        overlayController.requestOverlayPermission(context)
                                    } else {
                                        if (overlayController.isShowing()) {
                                            overlayController.hideOverlay()
                                            isOverlayActive = false
                                        } else {
                                            overlayController.showOverlay()
                                            isOverlayActive = true
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
                1 -> {
                    // Tab 1: AI Master Lab
                    AiAcousticLabScreen(
                        telemetry = telemetry,
                        coach = coach
                    )
                }
                2 -> {
                    // Tab 2: Architecture Specifications
                    ArchitectureSpecScreen()
                }
            }
        }
    }
}
