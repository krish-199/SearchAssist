
package com.krishdev.searchassist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.app.Fragment

class InfoFragment : Fragment() {

    private lateinit var serviceStatusView: TextView
    private lateinit var gestureStatusView: TextView
    private lateinit var toggleDetectionButton: Button
    private lateinit var configureAreaButton: Button
    private lateinit var statusOutput: TextView
    
    private var isGestureDetectionActive = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupButtons()
        updateServiceStatus()
        updateGestureStatus()
    }
    
    private fun initializeViews(view: View) {
        serviceStatusView = view.findViewById(R.id.service_status)
        gestureStatusView = view.findViewById(R.id.gesture_status)
        toggleDetectionButton = view.findViewById(R.id.btn_toggle_detection)
        configureAreaButton = view.findViewById(R.id.btn_configure_area)
        statusOutput = view.findViewById(R.id.status_output)
    }
    
    private fun setupButtons() {
        // Setup toggle detection button
        updateToggleButton()
        
        toggleDetectionButton.setOnClickListener {
            val activity = (activity as MainActivity)
            
            if (isGestureDetectionActive) {
                activity.stopAccessibilityService()
                isGestureDetectionActive = false
                showStatusOutput("Gesture detection stopped")
            } else {
                if (!activity.isAccessibilityServiceEnabled()) {
                    activity.promptEnableAccessibilityService()
                    showStatusOutput("Please enable accessibility service in Settings")
                } else {
                    activity.startAccessibilityService()
                    isGestureDetectionActive = true
                    showStatusOutput("Gesture detection started successfully")
                }
            }
            
            updateToggleButton()
            updateGestureStatus()
        }
        
        // Setup configure area button
        configureAreaButton.setOnClickListener {
            val activity = (activity as MainActivity)
            activity.navigateToConfig()
        }
    }
    
    private fun updateToggleButton() {
        toggleDetectionButton.text = if (isGestureDetectionActive) {
            "Stop Gesture Detection"
        } else {
            "Start Gesture Detection"
        }
    }
    
    private fun updateServiceStatus() {
        val activity = (activity as MainActivity)
        val isServiceEnabled = activity.isAccessibilityServiceEnabled()
        
        serviceStatusView.text = if (isServiceEnabled) {
            "Active"
        } else {
            "Inactive"
        }
    }
    
    private fun updateGestureStatus() {
        gestureStatusView.text = if (isGestureDetectionActive) {
            "Enabled"
        } else {
            "Disabled"
        }
    }
    
    private fun showStatusOutput(message: String) {
        statusOutput.visibility = View.VISIBLE
        statusOutput.text = message
        
        // Hide the output after 3 seconds
        statusOutput.postDelayed({
            statusOutput.visibility = View.GONE
        }, 3000)
    }
    
    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        
        // Sync with MainActivity state
        (activity as MainActivity)
        isGestureDetectionActive = MainActivity.isGestureDetectionActive
        updateToggleButton()
        updateGestureStatus()
    }
}