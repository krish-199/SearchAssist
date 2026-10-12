package com.krishdev.searchassist

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher


class MainActivity : Activity() {

    companion object {
        const val PREFS_NAME = "GestureLoggerPrefs"
        private const val PENDING_START_KEY = "pendingStartRequestedAt"
        private const val STATE_CONFIG_VISIBLE = "configVisible"
        private const val PENDING_START_TTL_MS = 10 * 60 * 1000L

        /** Remembers that the user asked to start detection once the service is turned on. */
        fun requestStartAfterPermission(context: Context) {
            context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putLong(PENDING_START_KEY, System.currentTimeMillis()).apply()
        }

        var isGestureDetectionActive = false
    }

    private lateinit var infoView: View
    private lateinit var configView: View
    private lateinit var infoScreen: InfoScreen
    private lateinit var configScreen: ConfigScreen
    private var backCallback: OnBackInvokedCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            val intent = Intent(this, IntroductoryActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Restore gesture detection state from SharedPreferences
        val savedGestureState = sharedPreferences.getBoolean("isGestureDetectionActive", false)

        // Verify the real Accessibility Service state via AccessibilityManager
        val serviceActuallyEnabled = isAccessibilityServiceEnabled()

        // Reconcile: gesture detection can only be active if the service is actually enabled
        val reconciledState = savedGestureState && serviceActuallyEnabled
        isGestureDetectionActive = reconciledState

        // Persist the corrected value back to SharedPreferences if it differs
        if (reconciledState != savedGestureState) {
            sharedPreferences.edit()
                .putBoolean("isGestureDetectionActive", reconciledState)
                .apply()
            Log.d("MainActivity", "Reconciled gesture state: saved=$savedGestureState, serviceEnabled=$serviceActuallyEnabled, reconciled=$reconciledState")

            // Update the Quick Settings Tile to reflect the corrected state
            requestTileUpdate()
        }

        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
        findViewById<View>(R.id.content)
            .applySystemBarInsets(resources.getDimensionPixelSize(R.dimen.content_max_width))

        infoView = findViewById(R.id.screen_info)
        configView = findViewById(R.id.screen_config)
        infoScreen = InfoScreen(this, infoView)
        configScreen = ConfigScreen(this, configView, findViewById(R.id.screen_overlay_preview))

        if (savedInstanceState?.getBoolean(STATE_CONFIG_VISIBLE) == true) showConfig()
    }

    override fun onResume() {
        super.onResume()
        if (!::infoScreen.isInitialized) return

        // Finish the "start detection" the user asked for before granting the permission
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        // Honoured only for a while, so an abandoned request can't start detection days later
        val requestedAt = prefs.getLong(PENDING_START_KEY, 0L)
        if (requestedAt > 0L && isAccessibilityServiceEnabled()) {
            prefs.edit().remove(PENDING_START_KEY).apply()
            if (System.currentTimeMillis() - requestedAt < PENDING_START_TTL_MS) startAccessibilityService()
        } else if (requestedAt > 0L && System.currentTimeMillis() - requestedAt >= PENDING_START_TTL_MS) {
            prefs.edit().remove(PENDING_START_KEY).apply()
        }
        infoScreen.refresh()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::configView.isInitialized) {
            outState.putBoolean(STATE_CONFIG_VISIBLE, configView.visibility == View.VISIBLE)
        }
    }

    fun showConfig() {
        infoScreen.onHidden()
        infoView.visibility = View.GONE
        configView.visibility = View.VISIBLE
        configScreen.onShown()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && backCallback == null) {
            val callback = OnBackInvokedCallback { showInfo() }
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback
            )
            backCallback = callback
        }
    }

    private fun showInfo() {
        configScreen.onHidden()
        configView.visibility = View.GONE
        infoView.visibility = View.VISIBLE
        infoScreen.refresh()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
            backCallback = null
        }
    }

    // Back below Android 13 (and on 13-14 when predictive back is off); otherwise showConfig()
    // registers an OnBackInvokedCallback, so the GestureBackNavigation warning does not apply.
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Used below Android 13; newer versions use OnBackInvokedCallback")
    override fun onBackPressed() {
        if (::configView.isInitialized && configView.visibility == View.VISIBLE) {
            showInfo()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        // Persist slider positions when leaving the app from the config screen
        if (::configView.isInitialized && configView.visibility == View.VISIBLE) configScreen.save()
    }


    fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    /**
     * Checks if the Accessibility Service is enabled using AccessibilityManager.
     * This is more reliable than parsing Settings.Secure strings.
     */
    fun isAccessibilityServiceEnabled(): Boolean = isSearchAssistServiceEnabled(this)

    /**
     * Requests the Quick Settings Tile to update its state.
     */
    private fun requestTileUpdate() {
        try {
            android.service.quicksettings.TileService.requestListeningState(
                this,
                ComponentName(this, QuickSettingsService::class.java)
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to request tile update", e)
        }
    }

    // Function to start the Accessibility Service
    fun startAccessibilityService() {
        ServiceSharedInstance.sendOverlayStatus(true)
        Log.d("MainActivity", "Accessibility Service Started")
        isGestureDetectionActive = true
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean("isGestureDetectionActive", true).apply()
        requestTileUpdate()
    }

    // Function to stop the Accessibility Service
    fun stopAccessibilityService() {
        ServiceSharedInstance.sendOverlayStatus(false)
        Log.d("MainActivity", "Accessibility Service Stopped")
        isGestureDetectionActive = false
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean("isGestureDetectionActive", false).apply()
        requestTileUpdate()
    }
}

/** Whether Search Assist's accessibility service is enabled. */
fun isSearchAssistServiceEnabled(context: Context): Boolean {
    val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        ?: return false

    val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
        android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
    )

    val targetServiceName = SimpleAccessibilityService::class.java.name

    return enabledServices.any { serviceInfo ->
        val servicePackageName = serviceInfo.resolveInfo?.serviceInfo?.packageName
        val serviceName = serviceInfo.resolveInfo?.serviceInfo?.name

        servicePackageName == context.packageName && serviceName == targetServiceName
    }
}
