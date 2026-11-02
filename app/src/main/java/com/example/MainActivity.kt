package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ai.GeminiAcousticCoach
import com.example.service.OverlayController
import com.example.service.VoxEngineAudioService
import com.example.ui.screens.MainStudioScreen
import com.example.ui.theme.VoxEngineTheme

class MainActivity : ComponentActivity() {

    private var audioService by mutableStateOf<VoxEngineAudioService?>(null)
    private var overlayController: OverlayController? = null
    private val coach = GeminiAcousticCoach()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? VoxEngineAudioService.LocalBinder
            audioService = binder?.getService()
            audioService?.let {
                overlayController = OverlayController(this@MainActivity, it)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            overlayController = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestRequiredPermissions()
        bindAudioService()

        setContent {
            VoxEngineTheme {
                MainStudioScreen(
                    service = audioService,
                    overlayController = overlayController,
                    coach = coach,
                    onStartService = { startAudioService() }
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 101)
        }
    }

    private fun startAudioService() {
        val intent = Intent(this, VoxEngineAudioService::class.java).apply {
            action = VoxEngineAudioService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun bindAudioService() {
        val intent = Intent(this, VoxEngineAudioService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(serviceConnection)
        } catch (e: Exception) {
            // Already unbound
        }
    }
}
