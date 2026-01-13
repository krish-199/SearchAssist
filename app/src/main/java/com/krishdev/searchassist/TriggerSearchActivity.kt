package com.krishdev.searchassist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

class TriggerSearchActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No UI needed

        val service = SimpleAccessibilityService.getInstance()
        if (service != null) {
            service.gatherAccessibilityData()
        } else {
            Toast.makeText(this, "Accessibility Service not running", Toast.LENGTH_SHORT).show()
            // Optionally guide user to settings
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } catch (e: Exception) {
                // Ignore if settings cannot be opened
            }
        }

        finish()
    }
}
