package com.krishdev.searchassist

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.concurrent.CompletableFuture

class SimpleAccessibilityService : AccessibilityService() {

    private val gestureActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.krishdev.ACTION_GATHER_ACCESSIBILITY_TAGS") {
                Log.d("SimpleAccessibilityService", "Broadcast received: gathering accessibility data")
                gatherAccessibilityData()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onServiceConnected() {
        // Register the receiver to listen for gesture actions
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK

        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN // Feedback type (e.g., for screen readers)
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

        info.notificationTimeout = 100L // Set the event notification delay to reduce excessive callbacks

        serviceInfo = info
        val filter = IntentFilter("com.krishdev.ACTION_GATHER_ACCESSIBILITY_TAGS")
        registerReceiver(gestureActionReceiver, filter, Context.RECEIVER_EXPORTED)
        Log.d("SimpleAccessibilityService", "Service connected")
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("SimpleAccessibilityService", "Accessibility event received ${event?.eventType}")
    }

    override fun onInterrupt() {
        // Handle interruptions
    }

    override fun onDestroy() {
//        unregisterReceiver(gestureActionReceiver)
        super.onDestroy()
    }

    // Method to gather accessibility data from the current screen
    private fun gatherAccessibilityData() {
        var rootNode = rootInActiveWindow
        if (rootNode != null) {
            if (extractTextFromNode(rootNode)) return;
        } else {
            val windowList = windows
            if (windowList.isNotEmpty()) {
                for (window in windowList) {
                    Log.d("SimpleAccessibilityService", "Window Title: ${window.title}, IsActive: ${window.isActive}, IsFocused: ${window.isFocused}");
                    rootNode = window.root
                    if (rootNode != null) {
                        if (extractTextFromNode(rootNode)) return;
                    } else {
                        Log.d("SimpleAccessibilityService", "Window root node is null for window: ${window.title}")
                    }
                }
            } else {
                Log.d("SimpleAccessibilityService", "No active windows available")
            }
            Log.d("SimpleAccessibilityService", "No active window found")
        }
        showErrorToast()
    }

    private fun getClickAbleNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d("SAS", "Node clicked")
            return true;
        }
        // if (node.parent == null) return false;
        var flag = false
        performClickAtNodeCoordinates(node) { event ->
            flag = event
        };
        return flag
    }

    private fun extractTextFromNode(node: AccessibilityNodeInfo): Boolean {
        val nodeDes = node.contentDescription?.toString()?.contains("search", ignoreCase = true)
        val nodeText = node.text?.toString()?.contains("search", ignoreCase = true)
        // Perform click action on the node
        if ((nodeDes == true || nodeText == true)) {
            node.text?.toString()?.let { Log.d("SAS", "text"+it) }
            node.contentDescription?.toString()?.let { Log.d("SAS", "ctdesc"+it) }
            // Get node coordinates
            val rect = Rect()
            node.getBoundsInScreen(rect)
            Log.d("SAS", "Node coordinates: (${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom})")
            return getClickAbleNode(node)
        } else {
            Log.d("SAS", "Node is not clickable")
        }
        // Recursively traverse child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if(extractTextFromNode(childNode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private fun showErrorToast() {
        Toast.makeText(this, "No search field found", Toast.LENGTH_SHORT).show()
    }


    private fun performClickAtNodeCoordinates(node: AccessibilityNodeInfo, callback: (Boolean) -> Unit) {
        // Get node coordinates
        val rect = Rect()
        node.getBoundsInScreen(rect)
        Log.d("SAS", "Node coordinates: (${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom})")

        // Calculate the center of the node
        val centerX = (rect.left + rect.right) / 2
        val centerY = (rect.top + rect.bottom) / 2

        // Create a gesture description for the click action
        val path = Path().apply {
            moveTo(centerX.toFloat(), centerY.toFloat())
        }
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
            
            // Dispatch the gesture with a callback
        dispatchGesture(gestureDescription, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d("SAS", "Gesture completed successfully")
                callback(true);
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d("SAS", "Gesture cancelled")
                showErrorToast();
                callback(false);
            }
        }, null)
    }
}