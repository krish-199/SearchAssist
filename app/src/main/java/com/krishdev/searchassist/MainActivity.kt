package com.krishdev.searchassist

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.app.Activity


class MainActivity : Activity() {

    companion object {
        var isGestureDetectionActive = false
        var isDebugMode = false
    }

    private var statusMessageView: TextView? = null

    private val PREFS_NAME = "GestureLoggerPrefs"

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
        
        initializeViews()
        setupInterface()

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, InfoFragment())
                .commit()
        }
    }
    
    private fun initializeViews() {
        statusMessageView = findViewById(R.id.status_message)
    }
    
    private fun setupInterface() {
        // Interface is now handled via themes and styles
        updateStatusMessage("Search Assist ready")
    }

    fun navigateToConfig() {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ConfigFragment())
            .addToBackStack(null)
            .commit()
        updateStatusMessage("Configuration screen loaded")
    }

    fun promptEnableAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    /**
     * Checks if the Accessibility Service is enabled using AccessibilityManager.
     * This is more reliable than parsing Settings.Secure strings.
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val targetServiceName = SimpleAccessibilityService::class.java.name
        
        return enabledServices.any { serviceInfo ->
            val servicePackageName = serviceInfo.resolveInfo?.serviceInfo?.packageName
            val serviceName = serviceInfo.resolveInfo?.serviceInfo?.name
            
            servicePackageName == packageName && serviceName == targetServiceName
        }
    }
    
    /**
     * Requests the Quick Settings Tile to update its state.
     */
    private fun requestTileUpdate() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.service.quicksettings.TileService.requestListeningState(
                    this,
                    ComponentName(this, QuickSettingsService::class.java)
                )
            }
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
        updateStatusMessage("Gesture detection active")
    }

    // Function to stop the Accessibility Service
    fun stopAccessibilityService() {
        ServiceSharedInstance.sendOverlayStatus(false)
        Log.d("MainActivity", "Accessibility Service Stopped")
        isGestureDetectionActive = false
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putBoolean("isGestureDetectionActive", false).apply()
        updateStatusMessage("Gesture detection stopped")
    }
    
    private fun updateStatusMessage(message: String) {
        statusMessageView?.let {
            it.text = message
            it.visibility = android.view.View.VISIBLE
            
            // Hide status message after 3 seconds
            it.postDelayed({
                it.visibility = android.view.View.GONE
            }, 3000)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Update section when navigating back
        updateStatusMessage("Returned to main screen")
    }






}