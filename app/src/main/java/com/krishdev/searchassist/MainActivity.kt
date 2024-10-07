package com.krishdev.searchassist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher


class MainActivity : ComponentActivity() {


    companion object {
        private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    }


    private fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        overlayPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            if (Settings.canDrawOverlays(this)) {
                // Permission granted
                Log.d("MainActivity", "Overlay permission granted")
            } else {
                // Permission denied
                Log.d("MainActivity", "Overlay permission denied")
            }
        }


        setContent {
                // Main content
                GestureLoggerApp()
        }
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