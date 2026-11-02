package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.dsp.DecodedAudio
import com.example.dsp.DemoAudioSynthesizer
import com.example.dsp.EngineTelemetry
import com.example.dsp.LockFreeRingBuffer
import com.example.dsp.RealTimeKaraokeEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "VoxEngineAudioService"
private const val NOTIFICATION_CHANNEL_ID = "vox_engine_service_channel"
private const val NOTIFICATION_ID = 4040

enum class AudioSourceMode {
    DEMO_SYNTH_TRACK,
    CUSTOM_AUDIO_FILE
}

class VoxEngineAudioService : Service() {

    private val binder = LocalBinder()
    val engine = RealTimeKaraokeEngine(48000f)
    private val demoSynthesizer = DemoAudioSynthesizer(48000f)

    private val isRunning = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val micCaptureEnabled = AtomicBoolean(true)

    private val _isPlayingState = MutableStateFlow(false)
    val isPlayingState: StateFlow<Boolean> = _isPlayingState.asStateFlow()

    private val _activeTrackName = MutableStateFlow("Synth Multi-Stem (Center Vocal)")
    val activeTrackName: StateFlow<String> = _activeTrackName.asStateFlow()

    private val _telemetryState = MutableStateFlow(EngineTelemetry())
    val telemetryState: StateFlow<EngineTelemetry> = _telemetryState.asStateFlow()

    private var sourceMode = AudioSourceMode.DEMO_SYNTH_TRACK
    private var customAudio: DecodedAudio? = null
    private var customAudioIndex = 0

    // Ring buffers (SPSC lock-free)
    // 48000 samples/sec -> buffer of 16384 samples provides ample headroom
    private val musicRingBufferL = LockFreeRingBuffer(16384)
    private val musicRingBufferR = LockFreeRingBuffer(16384)
    private val micRingBuffer = LockFreeRingBuffer(16384)
    private val outputRingBuffer = LockFreeRingBuffer(32768) // Stereo interleaved

    // Dedicated threads
    private var ingestThread: Thread? = null
    private var micThread: Thread? = null
    private var dspThread: Thread? = null
    private var dispatchThread: Thread? = null
    private var telemetryThread: Thread? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    inner class LocalBinder : Binder() {
        fun getService(): VoxEngineAudioService = this@VoxEngineAudioService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAudioEngine()
            ACTION_STOP -> stopAudioEngine()
            ACTION_TOGGLE_PLAY -> {
                if (isRunning.get()) {
                    togglePlayPause()
                } else {
                    startAudioEngine()
                }
            }
            ACTION_SET_ALPHA -> {
                val alpha = intent.getFloatExtra(EXTRA_ALPHA, 0.0f)
                engine.setVocalSuppressionAlpha(alpha)
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alphaVal = engine.getVocalSuppressionAlpha()
        val alphaPercent = (alphaVal * 100).toInt()

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("Vocal Cut: $alphaPercent% | Low-Latency A2DP Mixer Active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun startAudioEngine() {
        if (isRunning.getAndSet(true)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        isPaused.set(false)
        _isPlayingState.value = true

        clearBuffers()
        initHardwareAudio()
        launchThreads()
    }

    fun stopAudioEngine() {
        if (!isRunning.getAndSet(false)) return

        isPaused.set(true)
        _isPlayingState.value = false

        teardownThreads()
        teardownHardwareAudio()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun togglePlayPause() {
        val current = isPaused.get()
        isPaused.set(!current)
        _isPlayingState.value = current
    }

    fun setMicCaptureEnabled(enabled: Boolean) {
        micCaptureEnabled.set(enabled)
    }

    fun isMicCaptureEnabled(): Boolean = micCaptureEnabled.get()

    fun loadCustomAudio(audio: DecodedAudio, fileName: String) {
        customAudio = audio
        customAudioIndex = 0
        sourceMode = AudioSourceMode.CUSTOM_AUDIO_FILE
        _activeTrackName.value = fileName
        clearBuffers()
    }

    fun loadDemoSynthTrack() {
        customAudio = null
        sourceMode = AudioSourceMode.DEMO_SYNTH_TRACK
        demoSynthesizer.reset()
        _activeTrackName.value = "Synth Multi-Stem (Center Vocal)"
        clearBuffers()
    }

    private fun clearBuffers() {
        musicRingBufferL.clear()
        musicRingBufferR.clear()
        micRingBuffer.clear()
        outputRingBuffer.clear()
    }

    private fun initHardwareAudio() {
        val sampleRate = 48000
        val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minTrackBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val trackBufferSize = maxOf(minTrackBufferSize, 2048)

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .setEncoding(audioFormat)
            .build()

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(trackBufferSize)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
        }

        // Initialize AudioRecord for live microphone
        val minRecBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val recBufferSize = maxOf(minRecBufferSize, 1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufferSize
            )
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Record audio permission not granted yet", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioRecord", e)
        }
    }

    private fun teardownHardwareAudio() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "AudioTrack stop error", e)
        }
        audioTrack = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "AudioRecord stop error", e)
        }
        audioRecord = null
    }

    private fun launchThreads() {
        val blockSize = 256

        // Thread 1: Ingest Thread (Music source -> musicRingBuffers)
        ingestThread = Thread({
            val tempL = FloatArray(blockSize)
            val tempR = FloatArray(blockSize)

            while (isRunning.get()) {
                if (isPaused.get()) {
                    Thread.sleep(10)
                    continue
                }

                if (musicRingBufferL.availableWrite() >= blockSize) {
                    when (sourceMode) {
                        AudioSourceMode.DEMO_SYNTH_TRACK -> {
                            demoSynthesizer.generateBlock(tempL, tempR, blockSize)
                        }
                        AudioSourceMode.CUSTOM_AUDIO_FILE -> {
                            val audio = customAudio
                            if (audio != null && audio.leftChannel.isNotEmpty()) {
                                for (i in 0 until blockSize) {
                                    val idx = (customAudioIndex + i) % audio.leftChannel.size
                                    tempL[i] = audio.leftChannel[idx]
                                    tempR[i] = audio.rightChannel[idx]
                                }
                                customAudioIndex = (customAudioIndex + blockSize) % audio.leftChannel.size
                            } else {
                                tempL.fill(0f)
                                tempR.fill(0f)
                            }
                        }
                    }
                    musicRingBufferL.push(tempL, 0, blockSize)
                    musicRingBufferR.push(tempR, 0, blockSize)
                } else {
                    Thread.sleep(2)
                }
            }
        }, "VoxEngine_IngestThread").apply {
            priority = Thread.NORM_PRIORITY + 2
            start()
        }

        // Thread 2: Mic Capture Thread (Hardware mic -> micRingBuffer)
        micThread = Thread({
            val micShortBuffer = ShortArray(blockSize)
            val micFloatBuffer = FloatArray(blockSize)

            while (isRunning.get()) {
                val record = audioRecord
                if (record != null && record.recordingState == AudioRecord.RECORDSTATE_RECORDING && micCaptureEnabled.get()) {
                    val read = record.read(micShortBuffer, 0, blockSize)
                    if (read > 0) {
                        for (i in 0 until read) {
                            micFloatBuffer[i] = micShortBuffer[i] / 32768.0f
                        }
                        micRingBuffer.push(micFloatBuffer, 0, read)
                    } else {
                        Thread.sleep(2)
                    }
                } else {
                    // Feed silence when mic is disabled or uninitialized
                    micFloatBuffer.fill(0f)
                    if (micRingBuffer.availableWrite() >= blockSize) {
                        micRingBuffer.push(micFloatBuffer, 0, blockSize)
                    }
                    Thread.sleep(5)
                }
            }
        }, "VoxEngine_MicThread").apply {
            priority = Thread.MAX_PRIORITY - 1
            start()
        }

        // Thread 3: DSP Core Thread (Parametric Vocal Isolation + Mic FX + Mixer Sum)
        dspThread = Thread({
            val musicBlockL = FloatArray(blockSize)
            val musicBlockR = FloatArray(blockSize)
            val micBlock = FloatArray(blockSize)
            val outputBlockStereo = FloatArray(blockSize * 2)

            while (isRunning.get()) {
                val canReadMusic = musicRingBufferL.availableRead() >= blockSize
                val canReadMic = micRingBuffer.availableRead() >= blockSize
                val canWriteOut = outputRingBuffer.availableWrite() >= (blockSize * 2)

                if (canReadMusic && canWriteOut) {
                    musicRingBufferL.pop(musicBlockL, 0, blockSize)
                    musicRingBufferR.pop(musicBlockR, 0, blockSize)

                    if (canReadMic) {
                        micRingBuffer.pop(micBlock, 0, blockSize)
                    } else {
                        micBlock.fill(0f)
                    }

                    // Process complete DSP block
                    engine.processAudioBlock(musicBlockL, musicBlockR, micBlock, outputBlockStereo, blockSize)

                    // Push to output dispatcher
                    outputRingBuffer.push(outputBlockStereo, 0, blockSize * 2)
                } else {
                    Thread.sleep(1)
                }
            }
        }, "VoxEngine_DspThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        // Thread 4: Bluetooth Dispatcher Thread (Consumes from outputRingBuffer -> AudioTrack A2DP)
        dispatchThread = Thread({
            val floatStereoBuffer = FloatArray(blockSize * 2)
            val shortOutputBuffer = ShortArray(blockSize * 2)

            while (isRunning.get()) {
                val track = audioTrack
                if (track != null && outputRingBuffer.availableRead() >= (blockSize * 2)) {
                    outputRingBuffer.pop(floatStereoBuffer, 0, blockSize * 2)

                    // Convert float [-1.0 .. 1.0] to 16-bit PCM short
                    for (i in 0 until blockSize * 2) {
                        val sample = (floatStereoBuffer[i] * 32767.0f).coerceIn(-32768.0f, 32767.0f)
                        shortOutputBuffer[i] = sample.toInt().toShort()
                    }

                    track.write(shortOutputBuffer, 0, blockSize * 2)
                } else {
                    Thread.sleep(1)
                }
            }
        }, "VoxEngine_DispatchThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }

        // Telemetry updater thread for UI (60 FPS)
        telemetryThread = Thread({
            while (isRunning.get()) {
                _telemetryState.value = engine.getTelemetry()
                Thread.sleep(33) // ~30 updates per second
            }
        }, "VoxEngine_TelemetryThread").apply {
            priority = Thread.MIN_PRIORITY + 1
            start()
        }
    }

    private fun teardownThreads() {
        ingestThread?.interrupt()
        micThread?.interrupt()
        dspThread?.interrupt()
        dispatchThread?.interrupt()
        telemetryThread?.interrupt()

        ingestThread = null
        micThread = null
        dspThread = null
        dispatchThread = null
        telemetryThread = null
    }

    override fun onDestroy() {
        stopAudioEngine()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TOGGLE_PLAY = "com.example.service.ACTION_TOGGLE_PLAY"
        const val ACTION_SET_ALPHA = "com.example.service.ACTION_SET_ALPHA"
        const val EXTRA_ALPHA = "com.example.service.EXTRA_ALPHA"
    }
}
