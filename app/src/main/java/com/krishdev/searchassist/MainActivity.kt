package com.krishdev.searchassist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.krishdev.searchassist.ui.theme.AppTheme

class MainActivity : ComponentActivity() {


    companion object {
        private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
        var isGestureDetectionActive = mutableStateOf(false)
    }

    private val PREFS_NAME = "GestureLoggerPrefs"
    private val WIDTH_KEY = "width"
    private val HEIGHT_KEY = "height"
    private val HEIGHT_OFFSET_KEY = "heightOffset"


    private fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            val intent = Intent(this, IntroductoryActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContent {
            // Main content
            AppTheme {
                GestureLoggerApp()
            }
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
    fun startAccessibilityService(width: Int, height: Int, heightOffset: Int = 0) {
//        val intent = Intent(this, GestureDetectionService::class.java).apply {
//            putExtra("width", width)
//            putExtra("height", height)
//            putExtra("heightOffset", heightOffset)
//        }
//        startService(intent)
        ServiceSharedInstance.sendOverlayStatus(true)
        Log.d("MainActivity", "Accessibility Service Started")
        isGestureDetectionActive.value = true
    }

    // Function to stop the Accessibility Service
    fun stopAccessibilityService() {
//        val intent = Intent(this, GestureDetectionService::class.java)
//        stopService(intent)
        ServiceSharedInstance.sendOverlayStatus(false)
        Log.d("MainActivity", "Accessibility Service Stopped")
        isGestureDetectionActive.value = false
    }


    @Composable
    fun GestureLoggerApp() {
        // State to handle if gesture detection is active
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedWidth = sharedPreferences.getFloat(WIDTH_KEY, 20f)
        val savedHeight = sharedPreferences.getFloat(HEIGHT_KEY, 40f)
        val savedHeightOffset = sharedPreferences.getFloat(HEIGHT_OFFSET_KEY, 0f)
        val editor = sharedPreferences.edit()
        var isGestureDetectionActive by remember { mutableStateOf(false) }
        var width by remember { mutableFloatStateOf(savedWidth) }
        var height by remember { mutableFloatStateOf(savedHeight) }
        var heightOffset by remember { mutableFloatStateOf(savedHeightOffset) }

        val heightPixelMultiplier = 0.01f * LocalContext.current.resources.displayMetrics.heightPixels

        fun updatePrefs() {
            editor.putFloat(WIDTH_KEY, width)
            editor.putFloat(HEIGHT_KEY, height)
            editor.putFloat(HEIGHT_OFFSET_KEY, heightOffset)
            editor.apply()
        }

//    LaunchedEffect(width, height, heightOffset) {
//        editor.putFloat(WIDTH_KEY, width)
//        editor.putFloat(HEIGHT_KEY, height)
//        editor.putFloat(HEIGHT_OFFSET_KEY, heightOffset)
//        editor.apply()
//    }

        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            if (isGestureDetectionActive) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    // Stop gesture detection button
                    Button(onClick = {
                        stopAccessibilityService()
                        isGestureDetectionActive = false
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Stop Gesture Detection")
                    }
                }
            } else {
                // Show the start button and sliders when gesture detection is inactive
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Width: ${width.toInt()}", color = MaterialTheme.colorScheme.onBackground)
                    Slider(
                        value = width,
                        onValueChange = { width = it },
                        valueRange = 0f..100f,
                        steps = 9,
                        modifier = Modifier.padding(16.dp),
                        onValueChangeFinished = { updatePrefs() }
                    )

                    Text("Height: ${height.toInt()}", color = MaterialTheme.colorScheme.onBackground)
                    Slider(
                        value = height,
                        onValueChange = { height = it },
                        valueRange = 0f..100f,
                        steps = 9,
                        modifier = Modifier.padding(16.dp),
                        onValueChangeFinished = { updatePrefs() },
                    )

                    Text("Height Offset: ${heightOffset.toInt()}", color = MaterialTheme.colorScheme.onBackground)
                    Slider(
                        value = heightOffset,
                        onValueChange = { heightOffset = it },
                        valueRange = 0f..50f,
                        steps = 9,
                        modifier = Modifier.padding(16.dp),
                        onValueChangeFinished = { updatePrefs() }
                    )

                    Button(onClick = {
                        if (!isAccessibilityServiceEnabled()) {
                            promptEnableAccessibilityService()
                        } else {
                            startAccessibilityService(width.toInt(), height.toInt() * heightPixelMultiplier.toInt(), heightOffset.toInt() * heightPixelMultiplier.toInt())
                            isGestureDetectionActive = true
                        }
                    }) {
                        Text("Start Gesture Detection")
                    }
                }
            }
        }
        EdgeGestureDetector(width.toInt(), height.toInt() * heightPixelMultiplier.toInt(), heightOffset.toInt() * heightPixelMultiplier.toInt())

    }

    @Composable
    fun EdgeGestureDetector(width: Int, height: Int, heightOffset: Int) {
        // Detect gestures in the left 20px-wide region of the screen
        val density = LocalDensity.current

        // Convert pixel values to dp using the density
        val widthDp = with(density) { width.toDp() }
        val heightDp = with(density) { height.toDp() }
        val heightOffsetDp = with(density) { heightOffset.toDp() }
        val totalHeight = heightDp + heightOffsetDp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.Start)
                .wrapContentHeight(Alignment.Bottom)
                .width(widthDp) // Set width to 20dp for gesture detection area
                .height(totalHeight)
                .padding(bottom = heightOffsetDp) // Add height offset
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Perform gesture from the edge", modifier = Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSurface)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentWidth(Alignment.End)
                .wrapContentHeight(Alignment.Bottom)
                .width(widthDp) // Set width to 20dp for gesture detection area
                .height(totalHeight)
                .padding(bottom = heightOffsetDp)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Perform gesture from the edge", modifier = Modifier.padding(4.dp), color = MaterialTheme.colorScheme.onSurface)
        }
    }
//    @Composable
//    fun Main() {
//
//    }

    @Preview(showBackground = true)
    @Composable
    fun GestureLoggerAppPreview() {
        EdgeGestureDetector(60,900,10)
    }
}