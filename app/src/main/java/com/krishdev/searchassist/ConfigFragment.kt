
package com.krishdev.searchassist

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.app.Fragment

class ConfigFragment : Fragment() {

    private lateinit var widthSlider: SeekBar
    private lateinit var heightSlider: SeekBar
    private lateinit var heightOffsetSlider: SeekBar
    private lateinit var widthValue: TextView
    private lateinit var heightValue: TextView
    private lateinit var heightOffsetValue: TextView
    private lateinit var debugToggle: Switch
    private lateinit var saveConfigButton: Button
    private lateinit var backButton: Button
    private lateinit var configOutput: TextView
    private lateinit var screenOverlay: ScreenOverlayPreview
    
    private val PREFS_NAME = "GestureLoggerPrefs"
    private val WIDTH_KEY = "width"
    private val HEIGHT_KEY = "height"
    private val HEIGHT_OFFSET_KEY = "heightOffset"
    private val DEBUG_KEY = "debug"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        loadSavedSettings()
        setupSliders()
        setupToggle()
        setupButtons()
        
        // Initialize the overlay with current settings
        initializeOverlay()
    }
    
    override fun onResume() {
        super.onResume()
        // Show the overlay when the fragment is visible
        if (::screenOverlay.isInitialized) {
            screenOverlay.setOverlayVisible(true)
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Hide the overlay when leaving the fragment
        if (::screenOverlay.isInitialized) {
            screenOverlay.setOverlayVisible(false)
        }
    }
    
    private fun initializeViews(view: View) {
        widthSlider = view.findViewById(R.id.slider_width)
        heightSlider = view.findViewById(R.id.slider_height)
        heightOffsetSlider = view.findViewById(R.id.slider_height_offset)
        widthValue = view.findViewById(R.id.width_value)
        heightValue = view.findViewById(R.id.height_value)
        heightOffsetValue = view.findViewById(R.id.height_offset_value)
        debugToggle = view.findViewById(R.id.toggle_debug_mode)
        saveConfigButton = view.findViewById(R.id.btn_save_config)
        backButton = view.findViewById(R.id.btn_back_to_main)
        configOutput = view.findViewById(R.id.config_output)
        screenOverlay = view.findViewById(R.id.screen_overlay_preview)
    }
    
    private fun loadSavedSettings() {
        val sharedPreferences = activity!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedWidth = sharedPreferences.getFloat(WIDTH_KEY, 20f)
        val savedHeight = sharedPreferences.getFloat(HEIGHT_KEY, 40f)
        val savedHeightOffset = sharedPreferences.getFloat(HEIGHT_OFFSET_KEY, 0f)
        val savedDebugMode = sharedPreferences.getBoolean(DEBUG_KEY, false)
        
        widthSlider.progress = savedWidth.toInt()
        heightSlider.progress = savedHeight.toInt()
        heightOffsetSlider.progress = savedHeightOffset.toInt()
        debugToggle.isChecked = savedDebugMode
        
        updateValueDisplays()
    }
    
    private fun initializeOverlay() {
        // Set up the overlay with current slider values
        screenOverlay.updateGestureAreas(
            widthSlider.progress.toFloat(),
            heightSlider.progress.toFloat(),
            heightOffsetSlider.progress.toFloat()
        )
        screenOverlay.setOverlayVisible(true)
    }
    
    private fun setupSliders() {
        // Width Slider (limited to 40% max)
        widthSlider.max = 40
        widthSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateValueDisplays()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })
        
        // Height Slider
        heightSlider.max = 100
        heightSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateValueDisplays()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })
        
        // Height Offset Slider
        heightOffsetSlider.max = 100
        heightOffsetSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateValueDisplays()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                saveSettings()
            }
        })
    }
    
    private fun setupToggle() {
        // Only show debug toggle in debug builds
        if (BuildConfig.DEBUG) {
            debugToggle.visibility = View.VISIBLE
            debugToggle.setOnCheckedChangeListener { _, isChecked ->
                saveSettings()
                MainActivity.isDebugMode = isChecked
                showConfigOutput("Debug mode ${if (isChecked) "enabled" else "disabled"}")
            }
        } else {
            debugToggle.visibility = View.GONE
        }
    }
    
    private fun setupButtons() {
        saveConfigButton.setOnClickListener {
            saveSettings()
            showConfigOutput("Configuration saved successfully")
        }
        
        backButton.setOnClickListener {
            val activity = activity as? MainActivity
            activity?.onBackPressed()
        }
    }
    
    private fun saveSettings() {
        val sharedPreferences = activity!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        
        editor.putFloat(WIDTH_KEY, widthSlider.progress.toFloat())
        editor.putFloat(HEIGHT_KEY, heightSlider.progress.toFloat())
        editor.putFloat(HEIGHT_OFFSET_KEY, heightOffsetSlider.progress.toFloat())
        editor.putBoolean(DEBUG_KEY, debugToggle.isChecked)
        editor.apply()
    }
    
    private fun updateValueDisplays() {
        widthValue.text = "${widthSlider.progress}%"
        heightValue.text = "${heightSlider.progress}%"
        heightOffsetValue.text = "${heightOffsetSlider.progress}% from top"
        
        // Update screen overlay preview
        screenOverlay.updateGestureAreas(
            widthSlider.progress.toFloat(),
            heightSlider.progress.toFloat(),
            heightOffsetSlider.progress.toFloat()
        )
    }
    
    private fun showConfigOutput(message: String) {
        configOutput.visibility = View.VISIBLE
        configOutput.text = message
        
        // Hide the output after 3 seconds
        configOutput.postDelayed({
            configOutput.visibility = View.GONE
        }, 3000)
    }
}