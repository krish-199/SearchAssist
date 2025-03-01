package com.krishdev.searchassist

//import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.annotation.RequiresApi

class GestureListener(private val context: Context) : GestureDetector.SimpleOnGestureListener() {

    companion object {
        const val TAG = "GestureListener"

        /**
         * Returns a human-readable string describing the type of touch that triggered a MotionEvent.
         */
        private fun getTouchType(e: MotionEvent): String {
            var touchTypeDescription = " "
            val touchType = e.getToolType(0)

            touchTypeDescription += when (touchType) {
                MotionEvent.TOOL_TYPE_FINGER -> "(finger)"
                MotionEvent.TOOL_TYPE_STYLUS -> {
                    var description = "(stylus, pressure: ${e.pressure}"
                        description += ", buttons pressed: ${getButtonsPressed(e)}"
                    description + ")"
                }
                MotionEvent.TOOL_TYPE_ERASER -> "(eraser)"
                MotionEvent.TOOL_TYPE_MOUSE -> "(mouse)"
                else -> "(unknown tool)"
            }

            return touchTypeDescription
        }

        /**
         * Returns a human-readable string listing all the stylus buttons that were pressed when the
         * input MotionEvent occurred.
         */
        private fun getButtonsPressed(e: MotionEvent): String {
            var buttons = ""

            if (e.isButtonPressed(MotionEvent.BUTTON_PRIMARY)) {
                buttons += " primary"
            }

            if (e.isButtonPressed(MotionEvent.BUTTON_SECONDARY)) {
                buttons += " secondary"
            }

            if (e.isButtonPressed(MotionEvent.BUTTON_TERTIARY)) {
                buttons += " tertiary"
            }

            if (e.isButtonPressed(MotionEvent.BUTTON_BACK)) {
                buttons += " back"
            }

            if (e.isButtonPressed(MotionEvent.BUTTON_FORWARD)) {
                buttons += " forward"
            }

            if (buttons.isEmpty()) {
                buttons = "none"
            }

            return buttons
        }
    }

    // BEGIN_INCLUDE(init_gestureListener)
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        // Up motion completing a single tap occurred.
        Log.i(TAG, "Single Tap Up" + getTouchType(e))
        return super.onSingleTapUp(e)
    }

    override fun onLongPress(e: MotionEvent) {
        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        Log.i(TAG, "Long Press" + getTouchType(e))
        ServiceSharedInstance.sendOverlayWindowStatus(false)
//        return super.onLongPress(e)
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        // User attempted to scroll
        Log.i(TAG, "Scroll" + e1?.let { getTouchType(it) })
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        // Fling event occurred. Notification of this one happens after an "up" event.
//        val intent = Intent("com.krishdev.ACTION_GATHER_ACCESSIBILITY_TAGS")
        Log.i(TAG, "Fling" + e1?.let { getTouchType(it) })
        if (velocityY > 0) {
//            context.sendBroadcast(intent)
//            ServiceSharedInstance.sendAccessibilityData(true)
            Log.i(TAG, "Fling downward")
            // pausing fling downward as it causing issue with navigation
            return super.onFling(e1, e2, velocityX, velocityY)
                    // Create an intent to launch the desired application
//            val packageName = "ninja.sesame.app.edge" // Replace with the package name of the app you want to launch
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                val launchIntentSender = context.packageManager.getLaunchIntentSenderForPackage(packageName)
//                try {
//                    context.startIntentSender(launchIntentSender, null, 0, 0, 0)
//                } catch (e: ActivityNotFoundException) {
//                    Log.e(TAG, "Activity not found for package: $packageName")
//                }
//
//            } else {
//                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
//                if (launchIntent != null) {
//                    // Ensure the intent has the FLAG_ACTIVITY_NEW_TASK flag
//                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    context.startActivity(launchIntent)
//                } else {
//                    Log.e(TAG, "Launch intent not found for package: $packageName")
//                }
//            }
        } else {
            Log.i(TAG, "fling upward")
            ServiceSharedInstance.sendAccessibilityData(true)

        }
        return super.onFling(e1, e2, velocityX, velocityY)
    }

    override fun onShowPress(e: MotionEvent) {
        // User performed a down event, and hasn't moved yet.
        Log.i(TAG, "Show Press" + getTouchType(e))
        return super.onShowPress(e)
    }

    override fun onDown(e: MotionEvent): Boolean {
        // "Down" event - User touched the screen.
        Log.i(TAG, "Down" + getTouchType(e))
        return super.onDown(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        // User tapped the screen twice.
        Log.i(TAG, "Double tap" + getTouchType(e))
        return super.onDoubleTap(e)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the doubletap
        // occurring. This occurs for down, up, and move.
        val accessibilityService = SimpleAccessibilityService.getInstance()

        // Only attempt to lock if the service is running
        if (accessibilityService != null) {
            accessibilityService.lockDevice()
            Log.i(TAG, "Event within double tap" + getTouchType(e))
        } else {
            Log.e(TAG, "Accessibility service not running")
        }
        Log.i(TAG, "Event within double tap" + getTouchType(e))
        return super.onDoubleTapEvent(e)
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        // A confirmed single-tap event has occurred. Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        // Open notification panel on single tap
        try {
            val statusBarService = context.getSystemService("statusbar")
            val statusBarManager = Class.forName("android.app.StatusBarManager")
            val method = statusBarManager.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
        } catch (e: Exception) {
            context.sendBroadcast(Intent("android.intent.action.EXPAND_STATUS_BAR"))
        }

        return super.onSingleTapConfirmed(e)
    }
    // END_INCLUDE(init_gestureListener)
}