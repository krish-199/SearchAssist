package com.krishdev.searchassist

import android.util.Log

object ServiceSharedInstance {
    var gestureDetected: Boolean = false // For example, a flag to communicate gestures
    var accessibilityTags: String? = null // This will store data from Accessibility Service
    // List of registered listeners
    private val listeners = mutableListOf<OnGestureDetectedListener>()
    // Interface for listeners to implement
    interface OnGestureDetectedListener {
        fun onGestureDetected(isGestureDetected: Boolean)
    }
    // Register a listener
    fun registerListener(listener: OnGestureDetectedListener) {
        listeners.add(listener)
    }
    // Unregister a listener
    fun unregisterListener(listener: OnGestureDetectedListener) {
        listeners.remove(listener)
    }
    // Method to notify all registered listeners of the event
    private fun notifyGestureDetected(isGestureDetected: Boolean) {
        for (listener in listeners) {
            listener.onGestureDetected(isGestureDetected)  // Invoke the listener method
        }
    }
    // Add more shared properties or methods as needed
    fun sendAccessibilityData(isGestureDetected: Boolean) {
        Log.d("SSI", "Sending accessibility data to listeners")
        notifyGestureDetected(isGestureDetected)
        // Optionally notify listeners or handle any communication here
    }

    fun reset() {
        gestureDetected = false
        accessibilityTags = null
    }
}