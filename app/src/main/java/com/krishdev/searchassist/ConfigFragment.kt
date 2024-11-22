
package com.krishdev.searchassist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.krishdev.searchassist.ui.theme.AppTheme

class ConfigFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    ConfigScreen()
                }
            }
        }
    }

    private val PREFS_NAME = "GestureLoggerPrefs"
    private val WIDTH_KEY = "width"
    private val HEIGHT_KEY = "height"
    private val HEIGHT_OFFSET_KEY = "heightOffset"
    private val BLACKLIST_KEY = "blacklist"
    private val DEBUG_KEY = "debug"

    @Composable
    fun ConfigScreen() {
        val context = LocalContext.current
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedWidth = sharedPreferences.getFloat(WIDTH_KEY, 20f)
        val savedHeight = sharedPreferences.getFloat(HEIGHT_KEY, 40f)
        val savedHeightOffset = sharedPreferences.getFloat(HEIGHT_OFFSET_KEY, 0f)
        val editor = sharedPreferences.edit()
        var isGestureDetectionActive by remember { mutableStateOf(false) }
        var width by remember { mutableFloatStateOf(savedWidth) }
        var height by remember { mutableFloatStateOf(savedHeight) }
        var heightOffset by remember { mutableFloatStateOf(savedHeightOffset) }
        var isDebugMode by remember { mutableStateOf(sharedPreferences.getBoolean(DEBUG_KEY, false)) }

        val heightPixelMultiplier =
            0.01f * LocalContext.current.resources.displayMetrics.heightPixels

        fun updatePrefs() {
            editor.putFloat(WIDTH_KEY, width)
            editor.putFloat(HEIGHT_KEY, height)
            editor.putFloat(HEIGHT_OFFSET_KEY, heightOffset)
            editor.apply()
        }

        Column(
            modifier = Modifier.fillMaxSize(),
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
                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Button(onClick = {
                // navigate back to previous fragment
                val activity = context as? MainActivity
                activity?.onBackPressedDispatcher?.onBackPressed()
            }) {
                Text("Back to main screen")
            }
        }
        (activity as MainActivity).EdgeGestureDetector(
            width.toInt(),
            height.toInt() * heightPixelMultiplier.toInt(),
            heightOffset.toInt() * heightPixelMultiplier.toInt()
        )
    }

}