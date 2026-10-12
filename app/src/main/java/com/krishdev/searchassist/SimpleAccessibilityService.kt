package com.krishdev.searchassist

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import android.annotation.TargetApi

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

class GestureStrokeView(
    context: Context,
    private val startX: Float,
    private val startY: Float,
    private val color: Int = Color.BLUE
) : View(context) {
    private val paint = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(
            startX,
            startY,
            startX,
            startY + 10,
            paint
        ) // Example line, adjust as needed
    }
}

class SimpleAccessibilityService : AccessibilityService(),
    ServiceSharedInstance.OnGestureDetectedListener {

    private val gestureActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.krishdev.ACTION_GATHER_ACCESSIBILITY_TAGS") {
                if (debug) Log.d(
                    "SimpleAccessibilityService",
                    "Broadcast received: gathering accessibility data"
                )
                gatherAccessibilityData()
            }
        }
    }

    // This method is called when the listener is invoked
    override fun onGestureDetected(isGestureDetected: Boolean) {
        // Handle the gesture detection event here
        if (!isGestureDetected) return
        if (debug) Log.d("SimpleAccessibilityService", "Listener received: gathering accessibility data")
        // You can also trigger additional actions like accessibility-related processing
        gatherAccessibilityData()
    }

    companion object {
        // Static instance to access from other components
        private var instance: SimpleAccessibilityService? = null

        fun getInstance(): SimpleAccessibilityService? {
            return instance
        }

        private const val MIN_SCORE = 2
        // Only an editable/search-class match with a strong label reaches these
        private const val STRONG_SCORE = 8
        private const val EXCELLENT_SCORE = 10
        private const val MAX_NODES = 1500
        private const val SEARCH_BUDGET_MS = 300L
        private const val DEBOUNCE_MS = 400L
        private const val KEYBOARD_WAIT_MS = 300L
    }

    private var isKeyboardOpen = false
    private var debug = BuildConfig.DEBUG
    private lateinit var overlayView: GestureDetectionOverlay
    private var isOverlayDisabled = false
    private var lastSearchStart = 0L
    private var tapFallback: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("GestureLoggerPrefs", MODE_PRIVATE) }
    private val windowManagerCached by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    private val searchWord = Regex("\\bsearch\\b", RegexOption.IGNORE_CASE)
    private val notSearchBoxWords = Regex("history|recent|suggest|result|clear|voice|mic|camera|lens|image|barcode|qr", RegexOption.IGNORE_CASE)

    override fun onServiceConnected() {
        // Register the receiver to listen for gesture actions
        val info = AccessibilityServiceInfo()
        info.eventTypes =
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED

        info.feedbackType =
            AccessibilityServiceInfo.FEEDBACK_GENERIC // Feedback type (e.g., for screen readers)
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

        info.notificationTimeout =
            10L // Set the event notification delay to reduce excessive callbacks

        serviceInfo = info

        ServiceSharedInstance.registerListener(this)
        if (debug) Log.d("SimpleAccessibilityService", "Service connected")

        val sharedPreferences = getSharedPreferences("GestureLoggerPrefs", MODE_PRIVATE)
        debug = sharedPreferences.getBoolean("debug", false)

        val isFirst = sharedPreferences.getBoolean("isFirstRun", true)
        val isGestureActive = sharedPreferences.getBoolean("isGestureDetectionActive", false)

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // code is working as excepted do a cleanup of code to properly manage
        overlayView = GestureDetectionOverlay(this, windowManager)
        overlayView.onCreate()

        if (!isFirst && isGestureActive) {
            overlayView.enableOverlayListener(true)
        }

        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceSharedInstance.unregisterListener(this)
        if (::overlayView.isInitialized) overlayView.onDestroy()
        tapFallback?.let { mainHandler.removeCallbacks(it) }
        instance = null
    }


    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val focusedClassName = event.className?.toString()
        if (focusedClassName == "android.widget.EditText") {
            onKeyboardSignal()
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val currentPackage = event.packageName?.toString()
                val className = event.className?.toString()
                if (currentPackage == null || className == null) return

                // ignoring keyboard event using focused event for disabling keyboard
                if (currentPackage == "com.google.android.inputmethod.latin" || currentPackage == this.packageName) return

                val isSystemServiceOpen = currentPackage.contains("launcher", ignoreCase = true)
                val isInputOpen = className == "android.widget.EditText"
                if (debug) Log.d("SAS", "Acc triggered ==> $currentPackage, $className, $isSystemServiceOpen, $isInputOpen")

                // full screen check is only evaluated when the cheap checks don't already decide
                if (isSystemServiceOpen || isInputOpen || isFullScreenMode()) {
                    overlayView.enableOverlayOnWindowChange(false)
                    isOverlayDisabled = true
                } else if (isOverlayDisabled) {
                    if (prefs.getBoolean("isGestureDetectionActive", false)) {
                        overlayView.enableOverlayOnWindowChange(true)
                        isOverlayDisabled = false
                    }
                }

                if (isInputOpen) onKeyboardSignal()
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                overlayView.enableOverlayOnWindowChange(false)
                isOverlayDisabled = true
            }
        }
    }

    override fun onInterrupt() {
        // Handle interruptions
    }

    private fun enableOneHandedMode() {
        val displayMetrics = DisplayMetrics()
        windowManagerCached.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Starts below the display so it never lands on app content
        val path = Path().apply {
            moveTo(screenWidth / 2f, screenHeight.toFloat() + 50)
            lineTo(screenWidth / 2f, screenHeight.toFloat() + 100)
        }
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 10))
            .build()
        dispatchGesture(gestureDescription, null, null)
        if (debug) Log.d("SAS", "one handed mode enabled")
    }

    // Method to gather accessibility data from the current screen
    fun gatherAccessibilityData() {
        // ignore repeated triggers while a search is still in flight
        val now = SystemClock.uptimeMillis()
        if (now - lastSearchStart < DEBOUNCE_MS) return
        lastSearchStart = now
        tapFallback?.let { mainHandler.removeCallbacks(it) }
        tapFallback = null
        isKeyboardOpen = false

        val rootNode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getRootInActiveWindow(AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST)
        } else {
            rootInActiveWindow
        }
        if (rootNode != null) {
            try {
                if (searchAndAct(rootNode)) return
                enableOneHandedMode()
            } finally {
                rootNode.recycle()
            }
        } else {
            if (debug) Log.d("SimpleAccessibilityService", "Root node is null using windowslist")
            // only app windows, active/focused first (no system UI or keyboard windows)
            val appWindows = windows
                .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                .sortedByDescending { (if (it.isFocused) 2 else 0) + (if (it.isActive) 1 else 0) }
            for (window in appWindows) {
                val windowRoot = window.root ?: continue
                try {
                    if (searchAndAct(windowRoot)) return
                } finally {
                    windowRoot.recycle()
                }
            }
        }
        showErrorToast()
    }

    private fun onKeyboardSignal() {
        isKeyboardOpen = true
    }

    private fun isKeyboardWindowVisible(): Boolean =
        windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }

    private fun isKeyboardOpen(): Boolean {
        if (isKeyboardOpen) return true
        if (isKeyboardWindowVisible()) {
            onKeyboardSignal()
            overlayView.enableOverlayOnWindowChange(false)
            isOverlayDisabled = true
            return true
        }
        return false
    }


    private fun drawBoxOnNode(node: AccessibilityNodeInfo, color: Int = Color.RED) {
        if (!debug) return
        val nodeText = node.text?.toString()
        val nodeDes = node.contentDescription?.toString()
        val nodeId = node.viewIdResourceName?.toString()
        val nodeClass = node.className?.toString()
        val nodeClickacble = node.isClickable
        val nodeEditable = node.isEditable
        val nodeFoucasble = node.isFocusable
        val nodeCheckable = node.isCheckable

        Log.d(
            "DrawNode",
            "Node Text: $nodeText Node Description: $nodeDes Node Id: $nodeId Node Class: $nodeClass Node Clickable: $nodeClickacble Node Editable: $nodeEditable Node Focusable: $nodeFoucasble Node Checkable: $nodeCheckable"
        )
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

    private class Candidate(val node: AccessibilityNodeInfo, val score: Int, val bounds: Rect)

    private fun isSearchBoxBig(bounds: Rect, screenWidth: Int): Boolean {
        val widthPercentage = bounds.width().toFloat() / screenWidth * 100
        return widthPercentage > 40 && bounds.height() in 31..399
    }

    private fun isSearchIcon(bounds: Rect): Boolean {
        // Search icons are small and roughly square
        return bounds.width() in 1..350 && bounds.height() in 1..350 &&
                bounds.width().toDouble() / bounds.height().toDouble() in 0.5..2.8
    }

    /** Scores a node as a search-field candidate, or returns null if it is not one. */
    private fun scoreNode(node: AccessibilityNodeInfo, screenWidth: Int, screenHeight: Int): Candidate? {
        val id = node.viewIdResourceName?.substringAfterLast(":")
        val desc = node.contentDescription?.toString()
        val hint = node.hintText?.toString()
        val editable = node.isEditable
        // The text of an editable node is user input, not a label
        val text = if (editable) null else node.text?.toString()

        val idMatch = id?.contains("search", ignoreCase = true) == true
        val hintMatch = hint?.let { searchWord.containsMatchIn(it) } == true
        val descMatch = desc?.let { searchWord.containsMatchIn(it) } == true
        val textMatch = text?.let { it.length < 15 && searchWord.containsMatchIn(it) } == true
        if (!(idMatch || hintMatch || descMatch || textMatch)) return null
        if (!node.isVisibleToUser) return null

        val isHeader = id?.let {
            it.contains("header", ignoreCase = true) || it.contains("title", ignoreCase = true)
        } == true
        if (isHeader && !editable) return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.isEmpty) return null

        val iconLike = isSearchIcon(bounds)
        val boxLike = isSearchBoxBig(bounds, screenWidth)
        if (!(node.isClickable || editable || node.isFocusable || iconLike || boxLike)) return null

        var score = 0
        if (idMatch) score += 3
        if (hintMatch) score += 3
        if (descMatch) score += 2
        if (textMatch) score += 2
        val className = node.className?.toString().orEmpty()
        if (editable || className.contains("EditText") || className.contains("SearchView")) score += 4
        if (node.isClickable || node.isFocusable) score += 1
        if (iconLike) score += 1
        if (boxLike) score += 1
        if (bounds.top < screenHeight / 3) score += 1
        if (!editable && listOf(id, desc, text).any { it != null && notSearchBoxWords.containsMatchIn(it) }) score -= 5
        return if (score >= MIN_SCORE) Candidate(node, score, bounds) else null
    }

    /**
     * Finds the best search-field candidate under [root] and activates it.
     * Uses cheap platform lookups first and only walks the tree (breadth-first, time and node
     * limited, skipping invisible subtrees) when they don't give a confident result.
     */
    private fun searchAndAct(root: AccessibilityNodeInfo): Boolean {
        val obtained = ArrayList<AccessibilityNodeInfo>()
        try {
            val metrics = resources.displayMetrics
            var best: Candidate? = null
            fun consider(node: AccessibilityNodeInfo) {
                val c = scoreNode(node, metrics.widthPixels, metrics.heightPixels) ?: return
                val b = best
                if (b == null || c.score > b.score) best = c
            }

            // 1. an input field that already has focus
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let {
                obtained.add(it)
                consider(it)
            }
            // 2. one cross-process call that matches text and content description
            if ((best?.score ?: 0) < STRONG_SCORE) {
                root.findAccessibilityNodeInfosByText("search")?.forEach {
                    obtained.add(it)
                    consider(it)
                }
            }
            // 3. bounded breadth-first walk (also catches view ids and hints)
            if ((best?.score ?: 0) < STRONG_SCORE) {
                val deadline = SystemClock.uptimeMillis() + SEARCH_BUDGET_MS
                val queue = ArrayDeque<AccessibilityNodeInfo>()
                queue.add(root)
                var visited = 0
                while (queue.isNotEmpty() && visited < MAX_NODES &&
                    SystemClock.uptimeMillis() < deadline && (best?.score ?: 0) < EXCELLENT_SCORE
                ) {
                    val node = queue.removeFirst()
                    visited++
                    if (!node.isVisibleToUser) continue
                    consider(node)
                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let {
                            obtained.add(it)
                            queue.add(it)
                        }
                    }
                }
                if (debug) Log.d("SAS", "Visited $visited nodes")
            }

            val chosen = best ?: return false
            if (debug) Log.d("SAS", "Chosen score=${chosen.score} bounds=${chosen.bounds}")
            activate(chosen, obtained)
            return true
        } finally {
            obtained.forEach { it.recycle() }
        }
    }

    private fun activate(c: Candidate, obtained: MutableList<AccessibilityNodeInfo>) {
        val node = c.node
        var done: Boolean
        var clickedTarget = node.isClickable
        if (node.isEditable) {
            // focus first so the keyboard opens
            val focused = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            done = focused || clicked
        } else {
            var target: AccessibilityNodeInfo? = node
            var depth = 0
            while (target != null && !target.isClickable && depth < 3) {
                target = target.parent?.also { obtained.add(it) }
                depth++
            }
            if (target != null && target.isClickable) {
                done = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (target !== node) clickedTarget = true
            } else {
                done = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            }
        }
        drawBoxOnNode(node, if (done) Color.GREEN else Color.RED)
        if (!done) {
            performClickAtBounds(c.bounds)
        } else if (!clickedTarget) {
            // A non-clickable node only got focus: tap it if the keyboard doesn't show up
            val bounds = Rect(c.bounds)
            tapFallback = Runnable {
                if (!isKeyboardWindowVisible()) performClickAtBounds(bounds)
            }
            mainHandler.postDelayed(tapFallback!!, KEYBOARD_WAIT_MS)
        }
    }

    private fun showErrorToast() {
        Toast.makeText(this, getString(R.string.toast_no_search_field), Toast.LENGTH_SHORT).show()
    }

    private fun drawStroke(centerX: Int, centerY: Int) {
        // return
        if (!debug) return
        val gestureStrokeView =
            GestureStrokeView(this, centerX.toFloat(), centerY.toFloat(), Color.RED)
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

    private fun performClickAtBounds(bounds: Rect) {
        // getBoundsInScreen is already in screen coordinates, so no inset adjustment is needed
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        if (debug) Log.d("SAS", "Tap at ($centerX, $centerY) in $bounds")

        drawStroke(centerX, centerY)

        val path = Path().apply { moveTo(centerX.toFloat(), centerY.toFloat()) }
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gestureDescription, null, null)
    }

    @TargetApi(Build.VERSION_CODES.P)
    fun lockDevice() {
        try {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            if (debug) Log.i("SAS", "Device locked successfully")
        } catch (e: Exception) {
            Log.e("SAS", "Failed to lock device", e)
        }
    }


    private fun isFullScreenMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Use WindowInsets
            val insets = windowManagerCached.currentWindowMetrics.windowInsets
            !insets.isVisible(WindowInsets.Type.navigationBars())
        } else {
            // API < 30: a window that covers the whole display means full screen
            val displayMetrics = DisplayMetrics()
            windowManagerCached.defaultDisplay.getMetrics(displayMetrics)
            val rect = Rect()
            windows.any { window ->
                window.getBoundsInScreen(rect)
                rect.width() == displayMetrics.widthPixels && rect.height() == displayMetrics.heightPixels
            }
        }
    }
}