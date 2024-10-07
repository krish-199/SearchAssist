package com.krishdev.searchassist

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi

class BoundingBoxView(context: Context, private val col: Int = Color.RED) : View(context) {
    private val paint = Paint().apply {
        color = col
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private var bounds: Rect? = null

    fun setBounds(bounds: Rect) {
        this.bounds = bounds
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bounds?.let {
            canvas.drawRect(it, paint)
        }
    }
}

class GestureStrokeView(context: Context, private val startX: Float, private val startY: Float, private val color: Int = Color.BLUE) : View(context) {
    private val paint = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(startX, startY, startX, startY + 100, paint) // Example line, adjust as needed
    }
}

class SimpleAccessibilityService : AccessibilityService(), ServiceSharedInstance.OnGestureDetectedListener {

    private val gestureActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.krishdev.ACTION_GATHER_ACCESSIBILITY_TAGS") {
                Log.d("SimpleAccessibilityService", "Broadcast received: gathering accessibility data")
                gatherAccessibilityData()
            }
        }
    }
    // This method is called when the listener is invoked
    override fun onGestureDetected(isGestureDetected: Boolean) {
        // Handle the gesture detection event here
        if (!isGestureDetected) return
        Log.d("SimpleAccessibilityService", "Listener received: gathering accessibility data")
        // You can also trigger additional actions like accessibility-related processing
        gatherAccessibilityData()
    }

    private var isKeyboardOpen = false
    private var searchNodes = mutableListOf<AccessibilityNodeInfo>()

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
//        val filter = IntentFilter("com.krishdev.ACTION_GATHER_ACCESSIBILITY_TAGS")
//        registerReceiver(gestureActionReceiver, filter, Context.RECEIVER_EXPORTED)
        ServiceSharedInstance.registerListener(this)
        Log.d("SimpleAccessibilityService", "Service connected")
        super.onServiceConnected()
    }


    private fun checkImeVisibility(): Boolean {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val isImeVisible = inputMethodManager.isAcceptingText
        if (isImeVisible) isKeyboardOpen = true
        Log.d("IME_VISIBILITY", "IME is ${if (isImeVisible) "visible" else "not visible"}")
        return isImeVisible
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //Log.d("SimpleAccessibilityService", "Accessibility event received ${event?.eventType}")
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val className = event.className?.toString()
                Log.d("SAS", "Window state changed: $className")

                // Check if the window class name is related to the input method
                // if (className?.contains("InputMethod") == true || className == "com.android.inputmethod.latin.LatinIME") {
                //     Log.d("SAS", "Keyboard is likely open (window state changed).")
                // }
                isKeyboardOpen = true
            }

            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.d("SAS", "Window content changed")
                // You could further analyze this event to see if it's due to the keyboard opening.
            }

            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val focusedClassName = event.className?.toString()
                Log.d("SAS", "View focused: $focusedClassName")

                // If a text input field has been focused, it's a sign that the keyboard might open
                if (focusedClassName == "android.widget.EditText") {
                    Log.d("SAS", "A text field was focused, keyboard might be opening soon.")
                    isKeyboardOpen = true
                }
            }
        }
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
        isKeyboardOpen = false
        var rootNode = rootInActiveWindow
        if (rootNode != null) {
            if (extractTextFromNode(rootNode)) return
            else if (searchNodes.isNotEmpty()) {
                // for (node in searchNodes) {
                //     // if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                //     //     performClickAtNodeCoordinates(node)
                //     // }
                //     drawBoxOnNode(node, Color.YELLOW);
                // }
                performAction(searchNodes[0])
            }
        } else {
            Log.d("SimpleAccessibilityService", "Root node is null using windowslist")
            val windowList = windows
            if (windowList.isNotEmpty()) {
                for (window in windowList) {
                    Log.d("SimpleAccessibilityService", "Window Title: ${window.title}, IsActive: ${window.isActive}, IsFocused: ${window.isFocused}")
                    rootNode = window.root
                    if (rootNode != null) {
                        if (extractTextFromNode(rootNode)) return
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

//    @RequiresApi(Build.VERSION_CODES.R)
//    private fun isKeyboardOpen(): Boolean {
//        val rect = Rect()
//        rootInActiveWindow.getBoundsInScreen(rect)
//        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        val displayMetrics = DisplayMetrics()
//        val windowMetrics = windowManager.currentWindowMetrics
//        displayMetrics.widthPixels = windowMetrics.bounds.width()
//        displayMetrics.heightPixels = windowMetrics.bounds.height()
//        val screenHeight = displayMetrics.heightPixels
//        val keypadHeight = screenHeight - rect.bottom
//        return keypadHeight > screenHeight * 0.15
//    }
     private fun isKeyboardOpen(): Boolean {
         // dealy of 100 ms
         if(checkImeVisibility()) return true
         if (isKeyboardOpen) return true
    Log.d("SAS", "Checking for keyboard open before delay")

        Log.d("SAS", "Checking for keyboard open after delay")
         val windowsList = windows // Get the list of active windows
         if (windowsList.isEmpty()) {
             Log.d("SAS", "No active windows found")
             return false
         }
         for (window in windowsList) {
             // Check if the window is an input method (keyboard) window
             val className = window.root?.className?.toString()
             val windowType = window.type

             Log.d("SAS", "Window class name: $className, type: $windowType")

             // You can identify the keyboard window by its class name or type
             if (windowType == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                 Log.d("SAS", "Keyboard window detected!")
                 isKeyboardOpen = true
                 return true
             }
         }
         return false
     }


    private fun drawBoxOnNode(node: AccessibilityNodeInfo, color: Int = Color.RED) {
        return
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val boundingBoxView = BoundingBoxView(this, color)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(boundingBoxView, params)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val navigationBarHeight = getNavigationBarHeight()
        bounds.bottom -= navigationBarHeight

        boundingBoxView.setBounds(bounds)
            // Remove the box after 300ms
    Handler(Looper.getMainLooper()).postDelayed({
        windowManager.removeView(boundingBoxView)
    }, 3000)
    }

    // Function to get the height of the navigation bar
    private fun getNavigationBarHeight(): Int {
        val resources = resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
    }

    private fun isSearchBoxBig(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Get screen width
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Calculate node width
        val nodeWidth = bounds.right - bounds.left

        // Calculate percentage
        val widthPercentage = (nodeWidth.toFloat() / screenWidth) * 100

        Log.d("NODE", "Node width percentage: $widthPercentage% and height: ${bounds.height()}")
        drawBoxOnNode(node, Color.CYAN)
        return (widthPercentage > 40 && 10 < bounds.height() && bounds.height() < 400 )
    }


    private fun isSearchIcon(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        // Assuming search icon is usually small and located at the top right corner
        if ( bounds.width() in 1..199 && bounds.height() in 1..199) {
            return true
        }
        return false
    }

    private fun performAction(node: AccessibilityNodeInfo) {
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.d("SAS", "Click action performed")
            drawBoxOnNode(node, Color.GREEN)
        }
        else {
            Log.d("SAS", "Performing gesture")
            drawBoxOnNode(node)
            performClickAtNodeCoordinates(node)
        }
        if (isKeyboardOpen() && !isKeyboardOpen && !node.isClickable) {
            Log.d("SAS", "Performing gesture")
            drawBoxOnNode(node)
            performClickAtNodeCoordinates(node)
        }
    }

    private fun getClickAbleNode(node: AccessibilityNodeInfo): Boolean {
        // drawBoxOnNode(node)
        if ((node.isClickable && (node.isEditable)) || isSearchIcon(node) || isSearchBoxBig(node)) {
            performAction(node)
            // check if keyboard open intent has been thrown
                    // Check if the keyboard is open
//            val isKeyboardOpen = isKeyboardOpen();
//            Log.d("SAS", "keyboard check done: ==>$isKeyboardOpen");
            return true
        }
        if (node.isClickable) searchNodes.add(node)
        // only go to parent node if node is focusable if not then return false
        return false

        // collect all the nodes with search in it and at the emd peromg gesutre interaction, by sorting on the basis of biggest area of the node
        // var flag = false
        // performClickAtNodeCoordinates(node) { event ->
        //     flag = event
        // };
        // return flag
    }

    // TODO: make extraction process time limited, if more time is consumed kill the process

    private fun extractTextFromNode(node: AccessibilityNodeInfo): Boolean {
//        if (node.isEditable) drawBoxOnNode(node);
        if (isKeyboardOpen) return true
        val nodeDes = node.contentDescription?.toString()?.contains("search", ignoreCase = true)
        val nodeText = node.text?.toString()?.contains("search", ignoreCase = true)
        val nodeId = node.viewIdResourceName?.toString()?.substringAfterLast(":")?.contains("search", ignoreCase = true)
        //Perform click action on the node
        if ((nodeDes == true || nodeText == true) || nodeId == true) {
            node.text?.toString()?.let { Log.d("NODE", "text:: "+it) }
            node.contentDescription?.toString()?.let { Log.d("NODE", "Cont:: "+it) }
            node.viewIdResourceName?.toString()?.let { Log.d("NODE", "Id:: "+it) }
            if (getClickAbleNode(node)) return true
            return false
            // drawBoxOnNode(node);
                // Get node bounds
                    // if (getClickAbleNode(node)) return true;
        } else {
            Log.d("SAS", "Node is not clickable")
        }
        // Recursively traverse child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                if(extractTextFromNode(childNode)) {
                    return true
                }
            }
        }

        // processing collected nodes with search in them to process it later

        return false
    }

    private fun showErrorToast() {
        Toast.makeText(this, "No search field found", Toast.LENGTH_SHORT).show()
    }

    private fun drawStroke(centerX: Int, centerY: Int) {
        return
        val gestureStrokeView = GestureStrokeView(this, centerX.toFloat(), centerY.toFloat(), Color.RED)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(gestureStrokeView, params)

        // Remove the custom view after the gesture is completed
        Handler(Looper.getMainLooper()).postDelayed({
            windowManager.removeView(gestureStrokeView)
        }, 1000) // A
    }

    private fun performClickAtNodeCoordinates(node: AccessibilityNodeInfo) {
        // Get node coordinates
        val rect = Rect()
        node.getBoundsInScreen(rect)
        Log.d("SAS", "Node coordinates: (${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom})")


        val navigationBarHeight = getNavigationBarHeight()
        rect.bottom -= navigationBarHeight

        // Calculate the center of the node
        val centerX = (rect.left + rect.right) / 2
        val centerY = (rect.top + rect.bottom) / 2

        drawStroke(centerX, centerY)

        // Create a gesture description for the click action
        val path = Path().apply {
            moveTo(centerX.toFloat(), centerY.toFloat())
        }
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
            
            // Dispatch the gesture with a callback
        dispatchGesture(gestureDescription, null, null)
    }
}