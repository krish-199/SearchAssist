package com.krishdev.searchassist

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat


// convert it into view class, and add it to the window manager

class GestureDetectionService : AccessibilityService(),
    ServiceSharedInstance.OnWindowChangeListener {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var leftEdgeView: View
    private lateinit var rightEdgeView: View
    private lateinit var windowManager: WindowManager // Reference to the WindowManager
    private var width = 90
    private var height = WindowManager.LayoutParams.MATCH_PARENT
    private var heightOffset = 0

    companion object {
        const val CHANNEL_ID = "GestureDetectionChannel"
        const val debug = false
    }

    override fun onWindowChange(window: String) {
        try {
            if (!(::leftEdgeView.isInitialized || ::rightEdgeView.isInitialized)) return
            val isSystemServiceOpen = window.toString()
                .let {
                    it.contains("launcher", ignoreCase = true) || it.contains(
                        "input",
                        ignoreCase = true
                    )
                }
            if (isSystemServiceOpen) {
                updateOverlays(4)
            } else {
                updateOverlays()
            }
            Log.d("GestureDetectionService", "Foreground window changed: $window")
        } catch (e: Exception) {
            // in case of any exception destroy service
            Log.d("GestureDetectionService", "err ==>: $e")
            onDestroy()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        intent?.let {
            width = it.getIntExtra("width", 90)
            height = it.getIntExtra("height", 100)
            heightOffset = it.getIntExtra("heightOffset", 0)
            // Use the data as needed
        }

        Handler(Looper.getMainLooper()).post {
            if (::leftEdgeView.isInitialized && ::rightEdgeView.isInitialized) {
                updateOverlays()
            } else {
                Log.e("UpdateView", "View not added to WindowManager yet")
            }
        }
        // Start gesture detection logic (you will need to add the appropriate detection logic)
        Log.v("GDS", "in onStartCommand")
        // startForeground(1, createNotification())
        Log.d("GDS", "foreground started")
        // startGestureDetection()
        return START_NOT_STICKY
    }
//
//    override fun onCreate() {
//        super.onCreate()
//        // createNotificationChannel()
//        val gestureListener = GestureListener(this)
//        gestureDetector = GestureDetector(this, gestureListener)
//    //   startForeground(NOTIFICATION_ID, createNotification())
//        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//        ServiceSharedInstance.registerWindowListener(this)
//
//        // Start gesture detection logic (you will need to add the appropriate detection logic)
//        startGestureDetection()
//       //  if (!showOverlay) addOverlays()
//    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        super.onCreate()
        // createNotificationChannel()
        val gestureListener = GestureListener(this)
        gestureDetector = GestureDetector(this, gestureListener)
        //   startForeground(NOTIFICATION_ID, createNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        ServiceSharedInstance.registerWindowListener(this)

        // Start gesture detection logic (you will need to add the appropriate detection logic)
        startGestureDetection()
        //  if (!showOverlay) addOverlays()
    }

    private fun convertPercentToPixels(context: Context, percent: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        return (displayMetrics.heightPixels * percent / 100.0).toInt()
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
                    "$packageName/${GestureDetectionService::class.java.name}",
                    ignoreCase = true
                )
            ) {
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
        leftEdgeView = View(this)
        rightEdgeView = View(this)
        if (debug) {
            leftEdgeView = View(this).apply {
                setBackgroundColor(Color.parseColor("#80FF0000")) // Set the background color (e.g., semi-transparent red)
            }
            rightEdgeView = View(this).apply {
                setBackgroundColor(Color.parseColor("#8000FF00")) // Set the background color (e.g., semi-transparent green)
            }
        }

        leftEdgeView.setOnTouchListener { _, event ->
            // Pass the event to the gesture detector
            Log.d("GestureService", "Event received: ${event.action}")
            gestureDetector.onTouchEvent(event)
            false
        }

        rightEdgeView.setOnTouchListener { _, event ->
            // Pass the event to the gesture detector
            Log.d("GestureService", "Event received: ${event.action}")
            gestureDetector.onTouchEvent(event)
            false
        }

        Log.d("GestureService", "Gesture detection started in the background")

        addOverlays()
    }

    private fun addOverlays() {
        // Add the view to the window manager to receive touch events globally
        // Create layout parameters for the left edge
        val touchLayoutLeft = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM // Move to the left edge of the screen
            y = heightOffset
        }

        // Create layout parameters for the right edge
        val touchLayoutRight = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM // Move to the right edge of the screen
            y = heightOffset
        }

        windowManager.addView(leftEdgeView, touchLayoutLeft)
        windowManager.addView(rightEdgeView, touchLayoutRight)

        Log.d("GestureDetectionService", "Overlay views added.")
    }

    private fun updateOverlays(
        width: Int = this.width,
        height: Int = this.height,
        heightOffset: Int = this.heightOffset
    ) {
        // Add the view to the window manager to receive touch events globally
        // Create layout parameters for the left edge
        val touchLayoutLeft = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM // Move to the left edge of the screen
            y = heightOffset
        }

        // Create layout parameters for the right edge
        val touchLayoutRight = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM // Move to the right edge of the screen
            y = heightOffset
        }
        if (::leftEdgeView.isInitialized && leftEdgeView.isAttachedToWindow) windowManager.updateViewLayout(
            leftEdgeView,
            touchLayoutLeft
        )
        if (::rightEdgeView.isInitialized && rightEdgeView.isAttachedToWindow) windowManager.updateViewLayout(
            rightEdgeView,
            touchLayoutRight
        )

        Log.d("GDS", "Updating overlay")
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

    private fun removeOverlays() {
        try {
            Log.d("GestureDetectionService", "Attempting to remove overlay views.")
            if (::leftEdgeView.isInitialized && leftEdgeView.isAttachedToWindow) {
                windowManager.removeView(leftEdgeView)
                Log.d("GestureDetectionService", "Left edge view removed.")
            }
            if (::rightEdgeView.isInitialized && rightEdgeView.isAttachedToWindow) {
                windowManager.removeView(rightEdgeView)
                Log.d("GestureDetectionService", "Right edge view removed.")
            }
        } catch (e: Exception) {
            Log.e("GestureDetectionService", "Error removing overlay views: ${e.message}")
        }
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent) {}

    override fun onInterrupt() {}

}