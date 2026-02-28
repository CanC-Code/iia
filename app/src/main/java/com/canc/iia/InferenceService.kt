package com.canc.iia

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class InferenceService : LifecycleService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    
    // Tracks model loading state globally in the service
    private var isModelLoaded = mutableStateOf(false)
    private var isGenerating = mutableStateOf(false)

    companion object {
        const val CHANNEL_ID = "AI_Inference_Channel"
        const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        when (intent?.action) {
            "SHOW_OVERLAY" -> showFloatingOverlay()
            "LOAD_MODEL" -> {
                val mPath = intent.getStringExtra("MODEL_PATH") ?: ""
                val vPath = intent.getStringExtra("VAE_PATH") ?: ""
                val threads = intent.getIntExtra("THREADS", 4)
                
                serviceScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        NativeLib.loadModel(mPath, vPath, threads)
                    }
                    isModelLoaded.value = success
                }
            }
        }
        
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IIA Engine Active")
            .setContentText("Stable Diffusion is ready in the background")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showFloatingOverlay() {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 200
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@InferenceService)
            setViewTreeSavedStateRegistryOwner(this@InferenceService)
            setViewTreeViewModelStoreOwner(this@InferenceService)

            setContent {
                var isExpanded by remember { mutableStateOf(false) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }
                var promptText by remember { mutableStateOf("") }
                
                val loaded by isModelLoaded
                val generating by isGenerating

                Box(modifier = Modifier.padding(8.dp)) {
                    if (!isExpanded) {
                        // The Floating Bubble
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(if (loaded) MaterialTheme.colorScheme.primary else Color.Gray)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                        params.x = offsetX.roundToInt()
                                        params.y = offsetY.roundToInt()
                                        windowManager?.updateViewLayout(this@apply, params)
                                    }
                                }
                        ) {
                            IconButton(onClick = { isExpanded = true }) {
                                if (generating) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Open", tint = Color.White)
                                }
                            }
                        }
                    } else {
                        // The Expanded Interface
                        Card(
                            modifier = Modifier.width(300.dp).wrapContentHeight(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("IIA Controls", style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { isExpanded = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Minimize")
                                    }
                                }

                                OutlinedTextField(
                                    value = promptText,
                                    onValueChange = { promptText = it },
                                    label = { Text("Prompt") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !generating
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { 
                                        isGenerating.value = true
                                        serviceScope.launch {
                                            // Call the txt2img JNI function
                                            val result = withContext(Dispatchers.IO) {
                                                NativeLib.txt2img(promptText, "", 7.0f, 320, 320, 15, -1L)
                                            }
                                            // Handle result (e.g., save to gallery or show in overlay)
                                            isGenerating.value = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = loaded && !generating
                                ) {
                                    if (generating) {
                                        Text("Generating...")
                                    } else {
                                        Text("Generate Image")
                                    }
                                }
                                
                                if (!loaded) {
                                    Text("Model not loaded", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        windowManager?.addView(overlayView, params)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "AI Generation Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        NativeLib.freeModel()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
