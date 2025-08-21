package com.krishdev.searchassist

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.app.Activity

class IntroductoryActivity : Activity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var loadingMessage: TextView
    private lateinit var continueButton: Button
    private lateinit var welcomeMessage: TextView
    
    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = getSharedPreferences("GestureLoggerPrefs", MODE_PRIVATE)
        val isFirstRun = sharedPreferences.getBoolean("isFirstRun", true)

        if (!isFirstRun) {
            navigateToMainActivity()
            return
        }

        setContentView(R.layout.activity_introductory)
        
        initializeViews()
        setupIntroduction()
        startLoadingProgress()
    }
    
    private fun initializeViews() {
        loadingMessage = findViewById(R.id.loading_message)
        continueButton = findViewById(R.id.btn_continue_setup)
        welcomeMessage = findViewById(R.id.welcome_message)
    }
    
    private fun setupIntroduction() {
        // Setup continue button
        continueButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstRun", false)
            editor.apply()
            showCompletionMessage()
            
            // Delay navigation to show completion message
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToMainActivity()
            }, 1000)
        }
    }
    
    private fun startLoadingProgress() {
        loadingHandler = Handler(Looper.getMainLooper())
        
        loadingRunnable = object : Runnable {
            override fun run() {
                // Show continue button after simulated loading
                continueButton.visibility = android.view.View.VISIBLE
                loadingMessage.visibility = android.view.View.GONE
            }
        }
        
        // Show continue button after 2 seconds
        loadingHandler?.postDelayed(loadingRunnable!!, 2000)
    }
    
    private fun showCompletionMessage() {
        // Stop loading
        loadingHandler?.removeCallbacks(loadingRunnable!!)
        loadingMessage.text = "Setup completed successfully!"
        loadingMessage.visibility = android.view.View.VISIBLE
        
        // Hide continue button
        continueButton.visibility = android.view.View.INVISIBLE
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        loadingHandler?.removeCallbacks(loadingRunnable!!)
    }
}
