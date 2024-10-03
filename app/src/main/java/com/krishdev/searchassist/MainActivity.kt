package com.krishdev.searchassist

import android.content.Intent
import android.content.Context
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.media.projection.MediaProjectionManager
import android.media.projection.MediaProjection
import android.app.Activity
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startForegroundService

class MainActivity : ComponentActivity() {


    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    }


    private fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
                // Main content
                GestureLoggerApp()
        }

        // Check overlay permission
//        if (!Settings.canDrawOverlays(this)) {
//            val intent = Intent(
//                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                Uri.parse("package:$packageName")
//            )
//            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
//        } else {
//            // Check if the accessibility service is enabled
//            if (!isAccessibilityServiceEnabled()) {
//                promptEnableAccessibilityService()
//            }
//        }
    }


    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals("$packageName/${SimpleAccessibilityService::class.java.name}", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    // Function to start the Accessibility Service
    private fun startAccessibilityService() {
                // Check if the SYSTEM_ALERT_WINDOW permission is granted
        if (!Settings.canDrawOverlays(this)) {
            // Request the permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
        val intent = Intent(this, GestureDetectionService::class.java)
        startService(intent)
        Log.d("MainActivity", "Accessibility Service Started")
    }

    // Function to stop the Accessibility Service
    private fun stopAccessibilityService() {
        val intent = Intent(this, GestureDetectionService::class.java)
        stopService(intent)
        Log.d("MainActivity", "Accessibility Service Stopped")
    }


@Composable
fun GestureLoggerApp() {
    // State to handle if gesture detection is active
    var isGestureDetectionActive by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isGestureDetectionActive) {
            // Show the gesture detection area and the stop button
            // Edge gesture detector with translucent pink box
            EdgeGestureDetector { gestureType ->
                // Log the gesture action
                Log.d("GestureAction", gestureType)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Stop gesture detection button
                Button(onClick = {
                    stopAccessibilityService()
                    isGestureDetectionActive = false
                }) {
                    Text("Stop Accessibility Service")
                }
            }
        } else {
            // Show the start button when gesture detection is inactive
            Button(onClick = {
                if (!isAccessibilityServiceEnabled()) {
                    promptEnableAccessibilityService()
                } else {
                    startAccessibilityService()
                    isGestureDetectionActive = true
                }
            }) {
                Text("Start Accessibility Service")
            }
        }
    }
}

@Composable
fun GestureDetector(onGestureDetected: (String) -> Unit) {
    // A Box that listens for gestures
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onGestureDetected("Tap Detected")
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onGestureDetected("Drag Started")
                    },
                    onDragEnd = {
                        onGestureDetected("Drag Ended")
                    },
                    onDrag = { change, dragAmount ->
                        // Here you can handle the drag itself (e.g., for dragging UI elements)
                        onGestureDetected("Dragging... dx: ${dragAmount.x}, dy: ${dragAmount.y}")
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Perform a gesture")
    }
}

@Composable
fun EdgeGestureDetector(onGestureDetected: (String) -> Unit) {
    // Detect gestures in the left 20px-wide region of the screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentWidth(Alignment.Start)
            .width(20.dp) // Set width to 20dp for gesture detection area
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onGestureDetected("Tap Detected from Edge")
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onGestureDetected("Drag Started from Edge")
                    },
                    onDragEnd = {
                        onGestureDetected("Drag Ended from Edge")
                    },
                    onDrag = { change, dragAmount ->
                        // Handle drag gesture from the edge
                        onGestureDetected("Dragging from Edge... dx: ${dragAmount.x}, dy: ${dragAmount.y}")
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Perform gesture from the edge", modifier = Modifier.padding(4.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun GestureLoggerAppPreview() {
        GestureLoggerApp()
}

}