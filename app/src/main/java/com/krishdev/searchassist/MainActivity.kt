package com.krishdev.searchassist

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
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

    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        Log.d("MainActivity", "Enabled Services: $enabledServices")
        val colonSplitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(
                    "$packageName/${SimpleAccessibilityService::class.java.name}",
                    ignoreCase = true
                )
            ) {
                return true
            }
        }
        return false
    }

    // Function to start the Accessibility Service
    fun startAccessibilityService() {
        ServiceSharedInstance.sendOverlayStatus(true)
        Log.d("MainActivity", "Accessibility Service Started")
        isGestureDetectionActive = true
        updateStatusMessage("Gesture detection active")
    }

    // Function to stop the Accessibility Service
    fun stopAccessibilityService() {
        ServiceSharedInstance.sendOverlayStatus(false)
        Log.d("MainActivity", "Accessibility Service Stopped")
        isGestureDetectionActive = false
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