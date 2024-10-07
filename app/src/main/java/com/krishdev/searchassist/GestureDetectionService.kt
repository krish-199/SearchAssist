package com.krishdev.searchassist

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat

class GestureDetectionService : Service() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var leftEdgeView: View
    private lateinit var rightEdgeView: View
    private lateinit var windowManager: WindowManager // Reference to the WindowManager

    companion object {
        const val CHANNEL_ID = "GestureDetectionChannel"
    }

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val width = intent?.getIntExtra("width", 90) ?: 40
            val height = intent?.getIntExtra("height", WindowManager.LayoutParams.MATCH_PARENT) ?: WindowManager.LayoutParams.MATCH_PARENT
            val colorLeft = intent?.getStringExtra("colorLeft") ?: "#80FF0000"
            val colorRight = intent?.getStringExtra("colorRight") ?: "#8000FF00"

            // updateViews(width, height, colorLeft, colorRight)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val gestureListener = GestureListener(this)
        gestureDetector = GestureDetector(this, gestureListener)
//       startForeground(NOTIFICATION_ID, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Start gesture detection logic (you will need to add the appropriate detection logic)
        startGestureDetection()
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
            if (componentName.equals("$packageName/${GestureDetectionService::class.java.name}", ignoreCase = true)) {
                return true
            }
        }
        return false
    }


    private fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Add this line
        }
        startActivity(intent)
    }

    private fun startGestureDetection() {
        // Initialize views for left and right edges
        leftEdgeView = View(this).apply {
            setBackgroundColor(Color.parseColor("#80FF0000")) // Set the background color (e.g., semi-transparent red)
        }
        rightEdgeView = View(this).apply {
            setBackgroundColor(Color.parseColor("#8000FF00")) // Set the background color (e.g., semi-transparent green)
        }

        // Create layout parameters for the left edge
        val touchLayoutLeft = WindowManager.LayoutParams(
            70,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL // Move to the left edge of the screen
        }

        // Create layout parameters for the right edge
        val touchLayoutRight = WindowManager.LayoutParams(
            40,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL // Move to the right edge of the screen
        }

        leftEdgeView.setOnTouchListener { _, event ->
            // Pass the event to the gesture detector
            Log.d("GestureService", "Event received: ${event.action}")
            gestureDetector.onTouchEvent(event)
        }

        rightEdgeView.setOnTouchListener { _, event ->
            // Pass the event to the gesture detector
            Log.d("GestureService", "Event received: ${event.action}")
            gestureDetector.onTouchEvent(event)
        }

        // Add the view to the window manager to receive touch events globally
        windowManager.addView(leftEdgeView, touchLayoutLeft)
        windowManager.addView(rightEdgeView, touchLayoutRight)

        Log.d("GestureService", "Gesture detection started in the background")
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gesture Detection Running")
            .setContentText("Gesture detection is active, even in the background.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Gesture Detection Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the service running until explicitly stopped
        return START_STICKY
    }

    // Call this to stop gesture detection and stop the service
    private fun stopGestureDetection() {
        // Stop the service
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop gesture detection when the service is destroyed
        removeOverlays()
        stopGestureDetection()
    }
    private fun addViews(width: Int, height: Int) {
        // Create layout parameters for the left edge
        val touchLayoutLeft = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL // Move to the left edge of the screen
        }

        // Create layout parameters for the right edge
        val touchLayoutRight = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL // Move to the right edge of the screen
        }

        // Add views to the windowManager
        windowManager.addView(leftEdgeView, touchLayoutLeft)
        windowManager.addView(rightEdgeView, touchLayoutRight)
    }

    private fun removeOverlays() {
        try {
            Log.d("GestureDetectionService", "Attempting to remove overlay views.")
            if (::leftEdgeView.isInitialized) {
                windowManager.removeView(leftEdgeView)
                Log.d("GestureDetectionService", "Left edge view removed.")
            }
            if (::rightEdgeView.isInitialized) {
                windowManager.removeView(rightEdgeView)
                Log.d("GestureDetectionService", "Right edge view removed.")
            }
        } catch (e: Exception) {
            Log.e("GestureDetectionService", "Error removing overlay views: ${e.message}")
        }
    }

    override fun onBind(p0: Intent): IBinder? { return null }

}