package com.krishdev.searchassist

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView

/** First-run setup: explains the accessibility access, then sends the user to enable it. */
class IntroductoryActivity : Activity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var primaryButton: Button
    private lateinit var skipButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (!isFirstRun) {
            navigateToMainActivity()
            return
        }

        setContentView(R.layout.activity_introductory)
        enableEdgeToEdge()
        findViewById<View>(R.id.intro_root)
            .applySystemBarInsets(resources.getDimensionPixelSize(R.dimen.content_max_width))

        statusText = findViewById(R.id.intro_status)
        primaryButton = findViewById(R.id.btn_intro_primary)
        skipButton = findViewById(R.id.btn_intro_skip)
        skipButton.setOnClickListener { completeSetup() }
    }

    override fun onResume() {
        super.onResume()
        if (!::primaryButton.isInitialized) return

        // Re-check after returning from the accessibility settings
        if (isSearchAssistServiceEnabled(this)) {
            statusText.setText(R.string.intro_status_on)
            statusText.setTextColor(getColor(R.color.accent_secondary))
            primaryButton.setText(R.string.action_continue)
            primaryButton.setOnClickListener { completeSetup() }
            skipButton.visibility = View.GONE
        } else {
            statusText.setText(R.string.intro_status_off)
            statusText.setTextColor(getColor(R.color.text_secondary))
            primaryButton.setText(R.string.action_open_accessibility_settings)
            primaryButton.setOnClickListener {
                // Start detection automatically once the service is on
                MainActivity.requestStartAfterPermission(this)
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            skipButton.visibility = View.VISIBLE
        }
    }

    private fun completeSetup() {
        sharedPreferences.edit().putBoolean("isFirstRun", false).apply()
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
