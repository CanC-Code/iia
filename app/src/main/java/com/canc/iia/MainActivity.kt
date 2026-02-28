package com.canc.iia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartInference = { startAiService() },
                        onToggleOverlay = { checkOverlayPermissionAndStart() }
                    )
                }
            }
        }
    }

    private fun startAiService() {
        val intent = Intent(this, InferenceService::class.java)
        startService(intent)
    }

    private fun checkOverlayPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            // Request permission for the floating button
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            // Start the overlay via the service
            val intent = Intent(this, InferenceService::class.java).apply {
                action = "SHOW_OVERLAY"
            }
            startService(intent)
            finish() // Optional: Close main UI to show multitasking in action
        }
    }
}

@Composable
fun MainScreen(onStartInference: () -> Unit, onToggleOverlay: () -> Unit) {
    var prompt by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ready") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "IIA: Intelligent Image Assistant",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Positive Prompt") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { 
                status = "Model Loading..."
                onStartInference() 
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load GGUF Model")
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onToggleOverlay,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Switch to Floating Overlay")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(text = "Status: $status", style = MaterialTheme.typography.bodySmall)
    }
}
