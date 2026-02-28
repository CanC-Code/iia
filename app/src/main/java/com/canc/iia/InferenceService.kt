package com.canc.iia

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
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
        if (intent?.action == "SHOW_OVERLAY") {
            showFloatingOverlay()
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
            // Vital for Compose to work in a Service window
            setViewTreeLifecycleOwner(this@InferenceService)
            setViewTreeSavedStateRegistryOwner(this@InferenceService)
            setViewTreeViewModelStoreOwner(this@InferenceService)
            
            setContent {
                var isExpanded by remember { mutableStateOf(false) }
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                Box(modifier = Modifier.padding(8.dp)) {
                    if (!isExpanded) {
                        // The Floating Bubble
                        IconButton(
                            onClick = { isExpanded = true },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
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
                            Icon(Icons.Default.Add, contentDescription = "Open AI", tint = Color.White)
                        }
                    } else {
                        // The Expanded Interface
                        Card(
                            modifier = Modifier
                                .width(300.dp)
                                .wrapContentHeight(),
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
                                    value = "", 
                                    onValueChange = {}, 
                                    label = { Text("Prompt") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { /* Trigger NativeLib.txt2img */ },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Generate")
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
}
