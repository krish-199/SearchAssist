package com.krishdev.searchassist

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        var isGestureDetectionActive = mutableStateOf(false)
        var isDebugMode = mutableStateOf(false)
    }

    private val PREFS_NAME = "GestureLoggerPrefs"
    private val WIDTH_KEY = "width"
    private val HEIGHT_KEY = "height"
    private val HEIGHT_OFFSET_KEY = "heightOffset"
    private val BLACKLIST_KEY = "blacklist"
    private val DEBUG_KEY = "debug"


    private fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", false)

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

        // Remove the call to selectAppsForBlacklist()
        // selectAppsForBlacklist()
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
            if (componentName.equals(
                    "$packageName/${SimpleAccessibilityService::class.java.name}",
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

    // Function to start the Accessibility Service
    fun startAccessibilityService() {
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
        var showAppSelector by remember { mutableStateOf(false) }
        var isDebugMode by remember { mutableStateOf(sharedPreferences.getBoolean(DEBUG_KEY, false)) }

        val heightPixelMultiplier =
            0.01f * LocalContext.current.resources.displayMetrics.heightPixels

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
        Column(
            modifier = Modifier.fillMaxSize(),
//            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Header
            Text(
                text = "Search Assist",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
//                    .fillMaxWidth()
                    .padding(20.dp)
            )

            // Description
            Text(
                text = "“Technology should adjust to us rather than requiring us to adjust to it.”" +
                        "\n\nWith modern designs increasingly focusing user experience around search," +
                        "Search Assist aids those design choices further, by auto selecting the search field in the foreground app" +
                        "\n\nNote:- App currently has two features, \n1. By swiping up on gesture area, it would select the search field in the foreground app," +
                        " if no search field found, it will enable android native one handed mode (only android 12+)" +
                        "\n2. By swiping down on gesture area, it would launch sesame search if app is installed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = 20.dp, bottom = 30.dp, start = 16.dp, end = 16.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                // App name header
//                Text("SearchAssist", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

                if (isGestureDetectionActive) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {

                        // Stop gesture detection button
                        Button(
                            onClick = {
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
                        Text("Width", color = MaterialTheme.colorScheme.onBackground)
                        Slider(
                            value = width,
                            onValueChange = { width = it },
                            valueRange = 0f..100f,
                            steps = 9,
                            modifier = Modifier.padding(16.dp),
                            onValueChangeFinished = { updatePrefs() }
                        )

                        Text("Height", color = MaterialTheme.colorScheme.onBackground)
                        Slider(
                            value = height,
                            onValueChange = { height = it },
                            valueRange = 10f..100f,
                            steps = 9,
                            modifier = Modifier.padding(16.dp),
                            onValueChangeFinished = { updatePrefs() },
                        )

                        Text("Height Offset", color = MaterialTheme.colorScheme.onBackground)
                        Slider(
                            value = heightOffset,
                            onValueChange = { heightOffset = it },
                            valueRange = 0f..50f,
                            steps = 9,
                            modifier = Modifier.padding(16.dp),
                            onValueChangeFinished = { updatePrefs() }
                        )

                        // Debug mode switch
                        if (BuildConfig.DEBUG) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Debug Mode", color = MaterialTheme.colorScheme.onBackground)
                                Switch(
                                    checked = isDebugMode,
                                    onCheckedChange = {
                                        isDebugMode = it
                                        editor.putBoolean(DEBUG_KEY, it).apply()
                                        MainActivity.isDebugMode.value = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        Button(onClick = {
                            if (!isAccessibilityServiceEnabled()) {
                                promptEnableAccessibilityService()
                            } else {
                                startAccessibilityService()
                                isGestureDetectionActive = true
                            }
                        }) {
                            Text("Start Gesture Detection")
                        }

                        // OutlinedButton(onClick = { showAppSelector = true }) {
                        //     Text("Select Apps to Blacklist")
                        // }

                        // if (showAppSelector) {
                        //     SelectAppsForBlacklistDialog(onDismiss = { showAppSelector = false })
                        // }
                    }
                }
            }
        }
        EdgeGestureDetector(
            width.toInt(),
            height.toInt() * heightPixelMultiplier.toInt(),
            heightOffset.toInt() * heightPixelMultiplier.toInt()
        )

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
            Text(
                text = "Perform gesture from the edge",
                modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
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
            Text(
                text = "Perform gesture from the edge",
                modifier = Modifier.padding(4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    @Composable
    fun SelectAppsForBlacklistDialog(onDismiss: () -> Unit) {
        val packageManager = LocalContext.current.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appNames = apps.map { it.loadLabel(packageManager).toString() to it.packageName }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Apps to Blacklist") },
            text = {
                LazyColumn {
                    items(appNames) { app ->
                        Text(
                            text = app.first,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    addToBlacklist(app.second)
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        )
    }

    // Function to select apps and add to blacklist
    private fun selectAppsForBlacklist() {
        val packageManager = packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        val appNames = apps.map { it.loadLabel(packageManager).toString() to it.packageName }

        setContent {
            AppTheme {
                LazyColumn {
                    items(appNames) { app ->
                        Text(
                            text = app.first,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    addToBlacklist(app.second)
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

    private fun addToBlacklist(packageName: String) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val blacklist =
            sharedPreferences.getStringSet(BLACKLIST_KEY, mutableSetOf()) ?: mutableSetOf()
        // TODO: Fix below message
        blacklist.add(packageName)
        editor.putStringSet(BLACKLIST_KEY, blacklist)
        editor.apply()
    }

    @Preview(showBackground = true)
    @Composable
    fun GestureLoggerAppPreview() {
        EdgeGestureDetector(60, 900, 10)
    }
}