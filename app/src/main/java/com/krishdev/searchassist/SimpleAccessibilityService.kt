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
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.annotation.TargetApi
import java.util.ArrayDeque

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
                Log.d(
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
        Log.d("SimpleAccessibilityService", "Listener received: gathering accessibility data")
        // You can also trigger additional actions like accessibility-related processing
        gatherAccessibilityData()
    }

    companion object {
        // Static instance to access from other components
        private var instance: SimpleAccessibilityService? = null

        fun getInstance(): SimpleAccessibilityService? {
            return instance
        }
    }

    // Pre-compiled regex patterns for faster matching
    private val searchPattern = Regex("search|query|suche|buscar|recherche|find", RegexOption.IGNORE_CASE)
    private val headerPattern = Regex("header|title|toolbar_title|action_bar", RegexOption.IGNORE_CASE)

    // Keyboard state caching
    private var lastKeyboardCheck: Long = 0
    private var cachedKeyboardState: Boolean = false
    private val keyboardCacheTtlMs = 100L

    // Search candidate with priority score
    private data class SearchCandidate(val node: AccessibilityNodeInfo, val score: Int)

    private var isKeyboardOpen = false
    private var searchNodes = mutableListOf<AccessibilityNodeInfo>()
    private var debug = BuildConfig.DEBUG
    private lateinit var overlayView: GestureDetectionOverlay
    private var isOverlayDisabled = false

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
        Log.d("SimpleAccessibilityService", "Service connected")

        val sharedPreferences = getSharedPreferences("GestureLoggerPrefs", MODE_PRIVATE)
        debug = sharedPreferences.getBoolean("debug", false)

        val isFirst = sharedPreferences.getBoolean("isFirst", true)

        if (!isFirst) {
            overlayView.enableOverlayOnWindowChange(true)
        }

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // code is working as excepted do a cleanup of code to properly manage
        overlayView = GestureDetectionOverlay(this, windowManager)
        overlayView.onCreate()

        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }


    private fun checkImeVisibility(): Boolean {
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val isImeVisible = inputMethodManager.isAcceptingText
        if (isImeVisible) isKeyboardOpen = true
        Log.d("IME_VISIBILITY", "IME is ${if (isImeVisible) "visible" else "not visible"}")
        return isImeVisible
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //Log.d("SimpleAccessibilityService", "Accessibility event received ${event?.eventType}")
        if (event == null) return

        val focusedClassName = event.className?.toString()
//        var isKeyboardOpen = isKeyboardOpen
        if (focusedClassName == "android.widget.EditText") {
            Log.d("SAS", "A text field was focused, keyboard might be opening soon.")
            isKeyboardOpen = true
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val currentPackage = event.packageName?.toString()
                val className = event.className?.toString()
                if (currentPackage == null || className == null) return

                val isSystemServiceOpen = currentPackage.contains("launcher", ignoreCase = true)
                val isInputOpen = className == "android.widget.EditText"
                val isFullScreen = isFullScreenMode()

                // ignoring keyboard event usint foucsed event for diabling keybaord
                if (currentPackage == "com.google.android.inputmethod.latin" || currentPackage == this.packageName) return

//                ServiceSharedInstance.sendForegroundWindow(currentPackage)
//                val isSystemServiceOpen = currentPackage
//                    .let { it.contains("launcher", ignoreCase = true) || it.contains("input", ignoreCase = true) }
//
                Log.d(
                    "SAS",
                    "Acc triggered ==> $currentPackage, $className, $isSystemServiceOpen, $isInputOpen"
                )
                if ((isSystemServiceOpen || isInputOpen || isFullScreen)) {
                    overlayView.enableOverlayOnWindowChange(false)
                    isOverlayDisabled = true
                } else if (isOverlayDisabled) {
                    overlayView.enableOverlayOnWindowChange(true)
                    isOverlayDisabled = false
                }

                if (isInputOpen) isKeyboardOpen = true

//                // Check if the window class name is related to the input method
//                 if (event.className?.contains("InputMethod", ignoreCase = true) == true || event.className == "com.android.inputmethod.latin") {
//                     Log.d("SAS", "Keyboard is likely open (window state changed).")
//                 }
//                isKeyboardOpen = true
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val className = event.className?.toString()
                Log.d("SAS", "A text field edited $className")
                overlayView.enableOverlayOnWindowChange(false)
                isOverlayDisabled = true
            }

//            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
//                Log.d("ACC", "New view focused $focusedClassName")
//            }
//
//            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
//                Log.d("SAS", "Window content changed class $focusedClassName")
//                // You could further analyze this event to see if it's due to the keyboard opening.
//            }
        }
    }

    override fun onInterrupt() {
        // Handle interruptions
    }

    private fun enableOneHandedMode() {
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Define the gesture path for the swipe
        val path = Path().apply {
            moveTo(screenWidth / 2f, screenHeight.toFloat() + 50)
            lineTo(screenWidth / 2f, screenHeight.toFloat() + 100)
        }

        // Create a GestureDescription using the path
        val gestureDescription = GestureDescription.Builder()
            .addStroke(
                GestureDescription.StrokeDescription(
                    path,
                    0,
                    10
                )
            ) // Duration of 500ms for the swipe
            .build()

        // Dispatch the gesture
        dispatchGesture(gestureDescription, null, null)
        Log.d("SAS", "one handed mode enabled")
    }

    // Method to gather accessibility data from the current screen
    fun gatherAccessibilityData() {
        isKeyboardOpen = false
        cachedKeyboardState = false  // Reset keyboard cache
        lastKeyboardCheck = 0L
        searchNodes.clear()
        var rootNode = rootInActiveWindow
        if (rootNode != null) {
            try {
                // Use optimized BFS traversal with priority scoring
                if (extractTextFromNodeBFS(rootNode)) return
                else {
                    enableOneHandedMode()
                }
            } finally {
                rootNode.recycle()
            }
        } else {
            Log.d("SimpleAccessibilityService", "Root node is null using windowslist")
            val windowList = windows
            if (windowList.isNotEmpty()) {
                for (window in windowList) {
                    Log.d(
                        "SimpleAccessibilityService",
                        "Window Title: ${window.title}, IsActive: ${window.isActive}, IsFocused: ${window.isFocused}"
                    )
                    rootNode = window.root
                    if (rootNode != null) {
                        try {
                            // Use optimized BFS traversal
                            if (extractTextFromNodeBFS(rootNode)) return
                        } finally {
                            rootNode.recycle()
                        }
                    } else {
                        Log.d(
                            "SimpleAccessibilityService",
                            "Window root node is null for window: ${window.title}"
                        )
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
    private fun isKeyboardOpenCached(): Boolean {
        val now = System.currentTimeMillis()
        // Return cached value if within TTL
        if (now - lastKeyboardCheck < keyboardCacheTtlMs) {
            return cachedKeyboardState
        }
        
        lastKeyboardCheck = now
        
        // Quick check using IME visibility
        if (checkImeVisibility()) {
            cachedKeyboardState = true
            return true
        }
        
        if (isKeyboardOpen) {
            cachedKeyboardState = true
            return true
        }
        
        // Check windows for input method
        cachedKeyboardState = windows.any { window ->
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                isKeyboardOpen = true
                overlayView.enableOverlayOnWindowChange(false)
                isOverlayDisabled = true
                true
            } else {
                false
            }
        }
        
        return cachedKeyboardState
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

    private fun isSearchBoxBig(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val displayMetrics = resources.displayMetrics
        val density = displayMetrics.density
        val screenWidth = displayMetrics.widthPixels

        // Convert to density-independent pixels
        val nodeWidthDp = bounds.width() / density
        val nodeHeightDp = bounds.height() / density
        
        // Calculate width percentage
        val widthPercentage = (bounds.width().toFloat() / screenWidth) * 100

        Log.d("NODE", "Node width: $nodeWidthDp dp, height: $nodeHeightDp dp, width%: $widthPercentage%")
        
        // Search boxes are typically >40% width and between ~20-120dp height
        if (widthPercentage > 40 && nodeHeightDp in 20f..120f) {
            drawBoxOnNode(node, Color.YELLOW)
            return true
        }
        return false
    }


    private fun isSearchIcon(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        // Convert to density-independent pixels
        val density = resources.displayMetrics.density
        val widthDp = bounds.width() / density
        val heightDp = bounds.height() / density
        
        // Icons are typically 24-56dp, allow some margin (20-80dp)
        val isIconSize = widthDp in 20f..80f && heightDp in 20f..80f
        
        // Aspect ratio should be roughly square (0.6 to 1.6)
        val aspectRatio = if (heightDp > 0) widthDp / heightDp else 0f
        val isSquarish = aspectRatio in 0.6f..1.6f
        
        if (isIconSize && isSquarish) {
            drawBoxOnNode(node, Color.CYAN)
            return true
        }
        return false
    }

    private fun performAction(node: AccessibilityNodeInfo) {
        if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK) || node.performAction(
                AccessibilityNodeInfo.ACTION_FOCUS
            ) || node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) || node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        ) {
            Log.d("SAS", "Click action performed")
            drawBoxOnNode(node, Color.GREEN)
        } else {
            Log.d("SAS", "Performing gesture")
            drawBoxOnNode(node)
            performClickAtNodeCoordinates(node)
        }
        if (!isKeyboardOpenCached() && !isKeyboardOpen && !node.isClickable) {
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
        // if (node.isClickable) searchNodes.add(node)
        // only go to parent node if node is focusable if not then return false
        return false

        // collect all the nodes with search in it and at the emd peromg gesutre interaction, by sorting on the basis of biggest area of the node
        // var flag = false
        // performClickAtNodeCoordinates(node) { event ->
        //     flag = event
        // };
        // return flag
    }

    // Optimized BFS traversal with priority scoring and timeout
    private val traversalTimeoutMs = 500L

    /**
     * Calculate a priority score for a node to determine how likely it is to be a search field.
     * Higher scores indicate higher likelihood.
     */
    private fun calculateSearchScore(node: AccessibilityNodeInfo): Int {
        var score = 0
        val screenHeight = resources.displayMetrics.heightPixels
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Editable fields get highest priority (direct text input)
        if (node.isEditable) score += 50
        if (node.className == "android.widget.EditText") score += 40
        if (node.className == "android.widget.SearchView") score += 45

        // ID match is very reliable
        node.viewIdResourceName?.substringAfterLast(":")?.let { shortId ->
            if (searchPattern.containsMatchIn(shortId)) score += 30
        }

        // Content description is reliable
        node.contentDescription?.let {
            if (searchPattern.containsMatchIn(it)) score += 20
        }

        // Hint text is a good indicator
        node.hintText?.let {
            if (searchPattern.containsMatchIn(it)) score += 25
        }

        // Text match (less reliable, could be just a label)
        node.text?.let {
            if (it.length < 15 && searchPattern.containsMatchIn(it)) score += 10
        }

        // Position bonus - search bars are usually in top 25% of screen
        if (bounds.top < screenHeight * 0.25) score += 15

        return score
    }

    /**
     * Check if a node should be excluded (e.g., headers, titles)
     */
    private fun isExcludedNode(node: AccessibilityNodeInfo): Boolean {
        val shortId = node.viewIdResourceName?.substringAfterLast(":")
        return shortId?.let { headerPattern.containsMatchIn(it) } == true
    }

    /**
     * BFS traversal to find search nodes with priority scoring.
     * Returns the best candidate based on score.
     */
    private fun findBestSearchNode(rootNode: AccessibilityNodeInfo, startTime: Long): AccessibilityNodeInfo? {
        val candidates = mutableListOf<SearchCandidate>()
        val queue: ArrayDeque<AccessibilityNodeInfo> = ArrayDeque()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            // Timeout protection
            if (System.currentTimeMillis() - startTime > traversalTimeoutMs) {
                Log.w("SAS", "Search traversal timeout after ${traversalTimeoutMs}ms")
                break
            }

            // Early exit if keyboard is already open
            if (isKeyboardOpen) break

            val node = queue.removeFirst()

            // Skip excluded nodes like headers/titles
            if (!isExcludedNode(node)) {
                val score = calculateSearchScore(node)
                if (score > 0) {
                    Log.d("SAS", "Found candidate with score $score: ${node.viewIdResourceName}")
                    candidates.add(SearchCandidate(node, score))
                }
            }

            // Add children to queue (BFS)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        // Return the highest scoring candidate
        return candidates.maxByOrNull { it.score }?.node
    }

    /**
     * Optimized extraction using BFS and priority scoring.
     * Replaces the old DFS extractTextFromNode method.
     */
    private fun extractTextFromNodeBFS(rootNode: AccessibilityNodeInfo): Boolean {
        if (isKeyboardOpen) return true

        val startTime = System.currentTimeMillis()
        val bestNode = findBestSearchNode(rootNode, startTime)

        if (bestNode != null) {
            Log.d("SAS", "Best search node found: ${bestNode.viewIdResourceName}")
            return getClickAbleNode(bestNode)
        }

        return false
    }

    // Keep old DFS method as fallback (renamed)
    private fun extractTextFromNodeDFS(node: AccessibilityNodeInfo): Boolean {
//        if (node.isEditable) drawBoxOnNode(node);
        if (isKeyboardOpen) return true
        val nCo = node.contentDescription?.toString()
        val nTxt = node.text?.toString()
        val nId = node.viewIdResourceName?.toString()
        val nHint = node.hintText?.toString()
        
        // Use compiled regex for faster matching
        val nodeDes = nCo?.let { searchPattern.containsMatchIn(it) }
        val nodeText = nTxt?.let { it.length < 15 && searchPattern.containsMatchIn(it) }
        val nodeId = nId?.substringAfterLast(":")?.let { searchPattern.containsMatchIn(it) }
        val nodeHint = nHint?.let { searchPattern.containsMatchIn(it) }
        val isHeader = nId?.substringAfterLast(":")?.let { headerPattern.containsMatchIn(it) }
        
        if (isHeader == true) {
            Log.d("SAS", "Header found")
            return false
        }
        //Perform click action on the node
        if (nodeDes == true || nodeText == true || nodeId == true || nodeHint == true) {
            node.text?.toString()?.let { Log.d("NODE", "text:: " + it) }
            node.contentDescription?.toString()?.let { Log.d("NODE", "Cont:: " + it) }
            node.viewIdResourceName?.toString()?.let { Log.d("NODE", "Id:: " + it) }
            return getClickAbleNode(node)
        }
        // Recursively traverse child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                val found = extractTextFromNodeDFS(childNode)
                childNode.recycle()
                if (found) {
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

    private fun performClickAtNodeCoordinates(node: AccessibilityNodeInfo) {
        // Get node coordinates
        val rect = Rect()
        node.getBoundsInScreen(rect)
        Log.d("SAS", "Node coordinates: (${rect.left}, ${rect.top}, ${rect.right}, ${rect.bottom})")


        val navigationBarHeight = getNavigationBarHeight()
        rect.bottom -= navigationBarHeight

        rect.bottom -= 10

        // Calculate the center of the node
        val centerX = (rect.left + rect.right) / 2
        val centerY = (rect.top + rect.bottom) / 2


        drawStroke(centerX, centerY)

        // Create a gesture description for the click action
        val path = Path().apply {
            moveTo(centerX.toFloat(), centerY.toFloat())
        }
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        // Dispatch the gesture with a callback
        dispatchGesture(gestureDescription, null, null)
    }

    @TargetApi(Build.VERSION_CODES.P)
    fun lockDevice() {
        try {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            Log.i("SAS", "Device locked successfully")
        } catch (e: Exception) {
            Log.e("SAS", "Failed to lock device", e)
        }
    }


    private fun isFullScreenMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Use WindowInsets
            val insets = (getSystemService(WINDOW_SERVICE) as? WindowManager)?.currentWindowMetrics?.windowInsets
            val isNavigationBarVisible = insets?.isVisible(WindowInsets.Type.navigationBars()) ?: true
            !isNavigationBarVisible
        } else {
            // API < 30: Use legacy flags
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        
            // Compare current screen dimensions to app dimensions
            val screenHeight = displayMetrics.heightPixels
            val screenWidth = displayMetrics.widthPixels
        
            val windowList = windows
            for (window in windowList) {
                val rect = Rect()
                window.root?.getBoundsInScreen(rect)
        
                if (rect.width() == screenWidth && rect.height() == screenHeight) {
                    return true
                }
            }
        
            return false
        }
    }
}