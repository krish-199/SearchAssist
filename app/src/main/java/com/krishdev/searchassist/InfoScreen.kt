package com.krishdev.searchassist

import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView

/** Main screen: one status with its next action, the current edge zones, and the gestures. */
class InfoScreen(private val activity: MainActivity, root: View) {

    private enum class State { NEEDS_PERMISSION, READY, RUNNING }

    private val statusCard: View = root.findViewById(R.id.status_card)
    private val statusState: TextView = root.findViewById(R.id.status_state)
    private val statusDetail: TextView = root.findViewById(R.id.status_detail)
    private val statusAction: Button = root.findViewById(R.id.btn_status_action)
    private val currentConfiguration: TextView = root.findViewById(R.id.current_configuration)
    private val message = TransientMessage(root.findViewById(R.id.status_output))

    private var state = State.NEEDS_PERMISSION

    init {
        root.findViewById<TextView>(R.id.version_info).text =
            activity.getString(R.string.version_format, BuildConfig.VERSION_NAME)

        statusAction.setOnClickListener {
            when (state) {
                State.NEEDS_PERMISSION -> {
                    // Detection starts automatically once the user comes back with the service on
                    MainActivity.requestStartAfterPermission(activity)
                    activity.promptEnableAccessibilityService()
                    message.show(activity.getString(R.string.msg_enable_service))
                }
                State.READY -> {
                    activity.startAccessibilityService()
                    message.show(activity.getString(R.string.msg_detection_started))
                }
                State.RUNNING -> {
                    activity.stopAccessibilityService()
                    message.show(activity.getString(R.string.msg_detection_stopped))
                }
            }
            refresh()
        }

        root.findViewById<Button>(R.id.btn_configure_area).setOnClickListener {
            activity.showConfig()
        }
    }

    fun refresh() {
        val prefs = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val running = prefs.getBoolean("isGestureDetectionActive", false)
        MainActivity.isGestureDetectionActive = running
        state = when {
            !activity.isAccessibilityServiceEnabled() -> State.NEEDS_PERMISSION
            running -> State.RUNNING
            else -> State.READY
        }

        val (label, detail, color, action) = when (state) {
            State.NEEDS_PERMISSION -> Quad(
                R.string.status_needs_permission, R.string.status_needs_permission_detail,
                R.color.accent_danger, R.string.action_open_accessibility_settings
            )
            State.READY -> Quad(
                R.string.status_ready, R.string.status_ready_detail,
                R.color.accent_warning, R.string.action_start_detection
            )
            State.RUNNING -> Quad(
                R.string.status_running, R.string.status_running_detail,
                R.color.accent_secondary, R.string.action_stop_detection
            )
        }
        statusState.setText(label)
        statusState.setTextColor(activity.getColor(color))
        statusDetail.setText(detail)
        statusAction.setText(action)
        statusCard.contentDescription = activity.getString(
            R.string.status_a11y_format, activity.getString(label), activity.getString(detail)
        )

        currentConfiguration.text = activity.getString(
            R.string.config_summary_format,
            prefs.getFloat(ConfigScreen.WIDTH_KEY, ConfigScreen.DEFAULT_WIDTH).toInt(),
            prefs.getFloat(ConfigScreen.HEIGHT_KEY, ConfigScreen.DEFAULT_HEIGHT).toInt(),
            prefs.getFloat(ConfigScreen.HEIGHT_OFFSET_KEY, ConfigScreen.DEFAULT_OFFSET).toInt()
        )
    }

    fun onHidden() = message.cancel()

    private data class Quad(val label: Int, val detail: Int, val color: Int, val action: Int)
}
