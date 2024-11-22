
package com.krishdev.searchassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.krishdev.searchassist.ui.theme.AppTheme

class InfoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    InfoScreen()
                }
            }
        }
    }

    @Composable
    fun InfoScreen() {
        var isGestureDetectionActive by remember { mutableStateOf(false) }
        val activity = (activity as MainActivity)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "Search Assist",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(20.dp)
                )
            }
            item {
                Text(
                    text = "“Technology should adjust to us rather than requiring us to adjust to it.”" +
                            "\n\nWith modern designs increasingly focusing user experience around search," +
                            "Search Assist aids those design choices further, by auto selecting the search field in the foreground app" +
                            "\n\nNote:- App currently has two features, \n1. By swiping up on gesture area, it would select the search field in the foreground app," +
                            " if no search field found, it will enable android native one handed mode (only android 12+)" +
                            "\n2. By swiping down on gesture area, it would launch sesame search if app is installed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        top = 20.dp,
                        bottom = 30.dp,
                        start = 16.dp,
                        end = 16.dp
                    )
                )
            }
            item {

                if (isGestureDetectionActive) {
                    Button(onClick = {
                        activity.stopAccessibilityService()
                        isGestureDetectionActive = false
                    }) {
                        Text("Stop Gesture Detection")
                    }
                } else {
                    Button(onClick = {
                        if (!activity.isAccessibilityServiceEnabled()) {
                            activity.promptEnableAccessibilityService()
                        } else {
                            activity.startAccessibilityService()
                            isGestureDetectionActive = true
                        }
                    }) {
                        Text("Start Gesture Detection")
                    }
                }
            }
            item {
                Button(modifier = Modifier.padding(top = 10.dp), onClick = {
                    activity.navigateToConfig()
                }) {
                    Text("Modify Detection Area")
                }
            }
        }
    }
}