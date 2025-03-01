package com.krishdev.searchassist

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.WindowManager


// convert it into view class, and add it to the window manager

@SuppressLint("ViewConstructor")
class GestureDetectionOverlay(context: Context, private val windowManager: WindowManager) :
    View(context), ServiceSharedInstance.EnableOverlayListener {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var leftEdgeView: View
    private lateinit var rightEdgeView: View
    private var enableOverlay = false
    private var width = 40
    private var height = WindowManager.LayoutParams.MATCH_PARENT
    private var heightOffset = 0
    private val PREFS_NAME = "GestureLoggerPrefs"
    private val WIDTH_KEY = "width"
    private val HEIGHT_KEY = "height"
    private val HEIGHT_OFFSET_KEY = "heightOffset"
    private val DEBUG_KEY = "debug"
    private var debug = false
    private lateinit var sharedPreferences: SharedPreferences
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences, key ->
            when (key) {
                WIDTH_KEY, HEIGHT_KEY, HEIGHT_OFFSET_KEY -> {
                    val heightPixelMultiplier =
                        0.01f * context.resources.displayMetrics.heightPixels
                    width = sharedPreferences.getFloat("width", 0f).toInt()
                    height =
                        (sharedPreferences.getFloat("height", 0f) * heightPixelMultiplier).toInt()
                    heightOffset = (sharedPreferences.getFloat(
                        "heightOffset",
                        0f
                    ) * heightPixelMultiplier).toInt()
                    Log.d(
                        "GestureDetectionOverlay",
                        "Preferences changed: width=$width, height=$height, heightOffset=$heightOffset, heightPixel-$heightPixelMultiplier"
                    )
                }
                DEBUG_KEY -> {
                    debug = sharedPreferences.getBoolean(DEBUG_KEY, false)
                    removeOverlays()
                    startGestureDetection()
                    Log.d("GestureDetectionOverlay", "Debug mode changed: $debug")
                }
            }
        }

    fun onCreate() {
        val gestureListener = GestureListener(context)
        gestureDetector = GestureDetector(context, gestureListener)
        Log.d("GDO", "OnCreate function is called")
        // Start gesture detection logic (you will need to add the appropriate detection logic)
        startGestureDetection()
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val heightPixelMultiplier = 0.01f * context.resources.displayMetrics.heightPixels
        width = sharedPreferences.getFloat(WIDTH_KEY, 20f).toInt()
        height = (sharedPreferences.getFloat(HEIGHT_KEY, 40f) * heightPixelMultiplier).toInt()
        heightOffset =
            (sharedPreferences.getFloat(HEIGHT_OFFSET_KEY, 0f) * heightPixelMultiplier).toInt()
        debug = sharedPreferences.getBoolean(DEBUG_KEY, false)
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        ServiceSharedInstance.registerOverlayListener(this)
        // get shrerad preferences values
        //  if (!showOverlay) addOverlays()
    }

    private fun startGestureDetection() {
        // Initialize views for left and right edges
        leftEdgeView = View(context)
        rightEdgeView = View(context)
        if (debug) {
            leftEdgeView = View(context).apply {
                setBackgroundColor(Color.parseColor("#40FF0000")) // Set the background color (e.g., semi-transparent red)
            }
            rightEdgeView = View(context).apply {
                setBackgroundColor(Color.parseColor("#4000FF00")) // Set the background color (e.g., semi-transparent green)
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

        updateOverlays(0, 0, 0)
    }

    private fun updateOverlays(
        width: Int = this.width,
        height: Int = this.height,
        heightOffset: Int = this.heightOffset
    ) {
        // Add the view to the window manager to receive touch events globally
        // Create layout parameters for the left edge
        val touchLayoutLeft = getWindowLayout(width, height, heightOffset, true)

        // Create layout parameters for the right edge
        val touchLayoutRight = getWindowLayout(width, height, heightOffset, false)
        if (::leftEdgeView.isInitialized && leftEdgeView.isAttachedToWindow) windowManager.updateViewLayout(
            leftEdgeView,
            touchLayoutLeft
        ) else windowManager.addView(leftEdgeView, touchLayoutLeft)
        if (::rightEdgeView.isInitialized && rightEdgeView.isAttachedToWindow) windowManager.updateViewLayout(
            rightEdgeView,
            touchLayoutRight
        ) else windowManager.addView(rightEdgeView, touchLayoutRight)

        Log.d("GDS", "Updating overlay")
    }

    private fun getWindowLayout(
        width: Int,
        height: Int,
        heightOffset: Int,
        isLeftView: Boolean = true
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT // Make sure the overlay is translucent if needed
        ).apply {
            if (isLeftView) {
                gravity = Gravity.START or Gravity.BOTTOM // Move to the left edge of the screen
            } else {
                gravity = Gravity.END or Gravity.BOTTOM // Move to the right edge of the screen
            }
            y = heightOffset
        }
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

    fun onDestroy() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        removeOverlays()
        ServiceSharedInstance.unregisterOverlayListener(this)
    }

    override fun enableOverlayOnWindowChange(enable: Boolean) {
        if (!enableOverlay) return
        if (enable) updateOverlays()
        else updateOverlays(0)
    }

    override fun enableOverlayListener(enableOverlay: Boolean) {
        Log.d("GDO", "Overlay enabled: $enableOverlay")
        this.enableOverlay = enableOverlay
        if (enableOverlay) {
            updateOverlays()
        } else {
            updateOverlays(0, 0, 0)
        }
    }
}