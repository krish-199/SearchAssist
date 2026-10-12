package com.krishdev.searchassist

import android.content.Context
import android.view.View
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/** Edge zone settings; every change is saved immediately and shown on the live preview. */
class ConfigScreen(
    private val activity: MainActivity,
    root: View,
    private val preview: ScreenOverlayPreview
) {
    companion object {
        const val WIDTH_KEY = "width"
        const val HEIGHT_KEY = "height"
        const val HEIGHT_OFFSET_KEY = "heightOffset"
        const val DEBUG_KEY = "debug"
        const val DEFAULT_WIDTH = 20f
        const val DEFAULT_HEIGHT = 40f
        const val DEFAULT_OFFSET = 0f
    }

    private val prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
    private val widthSlider: SeekBar = root.findViewById(R.id.slider_width)
    private val heightSlider: SeekBar = root.findViewById(R.id.slider_height)
    private val offsetSlider: SeekBar = root.findViewById(R.id.slider_height_offset)
    private val widthValue: TextView = root.findViewById(R.id.width_value)
    private val heightValue: TextView = root.findViewById(R.id.height_value)
    private val offsetValue: TextView = root.findViewById(R.id.height_offset_value)
    private val debugToggle: Switch = root.findViewById(R.id.toggle_debug_mode)
    private val message = TransientMessage(root.findViewById(R.id.config_output))
    private var dirty = false

    init {
        widthSlider.progress = prefs.getFloat(WIDTH_KEY, DEFAULT_WIDTH).toInt()
        heightSlider.progress = prefs.getFloat(HEIGHT_KEY, DEFAULT_HEIGHT).toInt()
        // Zone must stay on screen: position + height <= 100%
        offsetSlider.max = 100 - heightSlider.progress
        offsetSlider.progress = prefs.getFloat(HEIGHT_OFFSET_KEY, DEFAULT_OFFSET).toInt()

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) dirty = true
                if (seekBar === heightSlider) offsetSlider.max = 100 - progress
                updateValues()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) = save()
        }
        widthSlider.setOnSeekBarChangeListener(listener)
        heightSlider.setOnSeekBarChangeListener(listener)
        offsetSlider.setOnSeekBarChangeListener(listener)

        if (BuildConfig.DEBUG) {
            debugToggle.isChecked = prefs.getBoolean(DEBUG_KEY, false)
            debugToggle.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean(DEBUG_KEY, isChecked).apply()
                message.show(activity.getString(if (isChecked) R.string.msg_debug_on else R.string.msg_debug_off))
            }
        } else {
            debugToggle.visibility = View.GONE
            root.findViewById<View>(R.id.advanced_header).visibility = View.GONE
        }
        updateValues()
    }

    fun onShown() {
        preview.visibility = View.VISIBLE
        preview.setOverlayVisible(true)
    }

    fun onHidden() {
        save()
        preview.setOverlayVisible(false)
        preview.visibility = View.GONE
        message.cancel()
    }

    /** Writes the sliders, only if the user changed one (opening the screen never rewrites values). */
    fun save() {
        if (!dirty) return
        dirty = false
        prefs.edit()
            .putFloat(WIDTH_KEY, widthSlider.progress.toFloat())
            .putFloat(HEIGHT_KEY, heightSlider.progress.toFloat())
            .putFloat(HEIGHT_OFFSET_KEY, offsetSlider.progress.toFloat())
            .apply()
    }

    private fun updateValues() {
        widthValue.text = activity.getString(R.string.percent_format, widthSlider.progress)
        heightValue.text = activity.getString(R.string.percent_format, heightSlider.progress)
        offsetValue.text = activity.getString(R.string.position_format, offsetSlider.progress)
        preview.updateGestureAreas(
            widthSlider.progress.toFloat(),
            heightSlider.progress.toFloat(),
            offsetSlider.progress.toFloat()
        )
    }
}
