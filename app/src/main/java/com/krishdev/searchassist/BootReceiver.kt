package com.krishdev.searchassist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, 
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d("BootReceiver", "Device booted or app updated, checking accessibility services")
                
                // Check if accessibility service is enabled
                if (isAccessibilityServiceEnabled(context)) {
                    Log.d("BootReceiver", "Accessibility service is enabled, initializing services")
                    
                    // Initialize service shared instance and restore overlay settings
                    initializeServices(context)
                    
                    // Show a subtle notification that the service is ready
                    Toast.makeText(context, "SearchAssist is ready", Toast.LENGTH_SHORT).show()
                    
                    // Optional: Start main activity if needed (uncomment if required)
                    // startMainActivity(context)
                } else {
                    Log.d("BootReceiver", "Accessibility service not enabled")
                    
                    // Optionally notify user that accessibility service needs to be enabled
                    Toast.makeText(context, "SearchAssist needs accessibility permission", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        
        if (enabledServices.isNullOrEmpty()) return false
        
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        
        val serviceName = "${context.packageName}/${SimpleAccessibilityService::class.java.name}"
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(serviceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
    
    private fun initializeServices(context: Context) {
        try {
            // Get saved preferences
            val sharedPreferences = context.getSharedPreferences("GestureLoggerPrefs", Context.MODE_PRIVATE)
            val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)
            
            // If not first time, enable overlay
            if (!isFirstRun) {
                Log.d("BootReceiver", "Restoring overlay settings")
                
                // Enable overlay through service shared instance
                // The accessibility service should pick this up when it reconnects
                ServiceSharedInstance.sendOverlayStatus(true)
                ServiceSharedInstance.sendOverlayWindowStatus(true)
            } else {
                Log.d("BootReceiver", "First time boot, overlay skipped")
            }
            
            // Restore any other necessary service states
            Log.d("BootReceiver", "Service initialization completed")
            
        } catch (e: Exception) {
            Log.e("BootReceiver", "Error initializing services: ${e.message}")
        }
    }
} 