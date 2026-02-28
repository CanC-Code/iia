package com.canc.iia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
                        onStartInference = { model, vae, threads -> 
                            startAiService(model, vae, threads) 
                        },
                        onToggleOverlay = { checkOverlayPermissionAndStart() }
                    )
                }
            }
        }
    }

    private fun startAiService(modelPath: String, vaePath: String, threads: Int) {
        val intent = Intent(this, InferenceService::class.java).apply {
            putExtra("MODEL_PATH", modelPath)
            putExtra("VAE_PATH", vaePath)
            putExtra("THREADS", threads)
            action = "LOAD_MODEL"
        }
        startService(intent)
    }

    private fun checkOverlayPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            val intent = Intent(this, InferenceService::class.java).apply {
                action = "SHOW_OVERLAY"
            }
            startService(intent)
            // finish() // Keep open for now to verify model loading
        }
    }
}

@Composable
fun MainScreen(
    onStartInference: (String, String, Int) -> Unit, 
    onToggleOverlay: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // State for file paths and parameters
    var modelPath by remember { mutableStateOf("") }
    var vaePath by remember { mutableStateOf("") }
    var threadCount by remember { mutableFloatStateOf(4f) }
    var status by remember { mutableStateOf("Waiting for selection...") }

    // File Pickers
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            status = "Processing model file..."
            modelPath = FilePickerUtils.getPathFromUri(context, it) ?: ""
            status = if (modelPath.isNotEmpty()) "Model ready" else "Error selecting model"
        }
    }

    val vaePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { 
            vaePath = FilePickerUtils.getPathFromUri(context, it) ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = "IIA Core", style = MaterialTheme.typography.headlineLarge)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Model Selection Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Model Configuration", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(onClick = { modelPicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (modelPath.isEmpty()) "Select .gguf Model" else "Model Selected")
                }
                if (modelPath.isNotEmpty()) {
                    Text(modelPath.split("/").last(), style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(onClick = { vaePicker.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vaePath.isEmpty()) "Select VAE (Optional)" else "VAE Selected")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Section
        Text("CPU Threads: ${threadCount.roundToInt()}")
        Slider(
            value = threadCount,
            onValueChange = { threadCount = it },
            valueRange = 1f..8f,
            steps = 7
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { 
                if (modelPath.isNotEmpty()) {
                    status = "Loading engine..."
                    onStartInference(modelPath, vaePath, threadCount.roundToInt())
                } else {
                    status = "Please select a model first"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = modelPath.isNotEmpty()
        ) {
            Text("Initialize AI Engine")
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onToggleOverlay,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Launch Overlay")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Status: $status", color = MaterialTheme.colorScheme.primary)
    }
}
