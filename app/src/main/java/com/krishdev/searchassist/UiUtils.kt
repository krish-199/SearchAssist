package com.krishdev.searchassist

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.widget.TextView

/** Draws behind the system bars on every API level, as Android 15+ already does. */
fun Activity.enableEdgeToEdge() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }
}

/**
 * Pads this view for the system bars and display cutout (Android 15+ draws edge-to-edge),
 * and adds extra side padding so the content is centred and at most [maxContentWidthPx] wide.
 */
fun View.applySystemBarInsets(maxContentWidthPx: Int) {
    var left = 0
    var top = 0
    var right = 0
    var bottom = 0
    fun update() {
        val extra = ((width - left - right - maxContentWidthPx) / 2).coerceAtLeast(0)
        setPadding(left + extra, top, right + extra, bottom)
    }
    setOnApplyWindowInsetsListener { _, insets ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom
        } else {
            @Suppress("DEPRECATION")
            left = insets.systemWindowInsetLeft
            @Suppress("DEPRECATION")
            top = insets.systemWindowInsetTop
            @Suppress("DEPRECATION")
            right = insets.systemWindowInsetRight
            @Suppress("DEPRECATION")
            bottom = insets.systemWindowInsetBottom
        }
        update()
        insets
    }
    addOnLayoutChangeListener { _, l, _, r, _, oldL, _, oldR, _ ->
        if (r - l != oldR - oldL) post { update() }
    }
    requestApplyInsets()
}

/** Shows a message in [view] for a few seconds; a newer message restarts the timer. */
class TransientMessage(private val view: TextView) {
    private val hide = Runnable { view.visibility = View.GONE }

    fun show(message: CharSequence) {
        view.removeCallbacks(hide)
        view.text = message
        view.visibility = View.VISIBLE
        view.postDelayed(hide, 3000)
    }

    fun cancel() {
        view.removeCallbacks(hide)
    }
}
