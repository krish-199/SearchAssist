package com.krishdev.searchassist

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/**
 * An overlay view that displays gesture detection areas directly on the screen edges
 * This provides a real-time preview of where gesture detection would occur
 */
class ScreenOverlayPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    // Paint objects for drawing the overlay areas
    private val overlayPaint = Paint().apply {
        color = context.getColor(R.color.accent_primary)
        style = Paint.Style.FILL
        alpha = 120 // Semi-transparent overlay
    }

    private val strokePaint = Paint().apply {
        color = context.getColor(R.color.accent_primary)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Gesture area parameters (percentages)
    private var widthPercent: Float = 20f
    private var heightPercent: Float = 40f
    private var heightOffsetPercent: Float = 0f

    // Rectangle objects for gesture areas
    private val leftGestureRect = RectF()
    private val rightGestureRect = RectF()
    
    // Flag to control overlay visibility
    private var showOverlay: Boolean = true
    
    init {
        // This view should not intercept touch events - it's purely visual
        setWillNotDraw(false)
        isClickable = false
        isFocusable = false
    }

    fun updateGestureAreas(width: Float, height: Float, heightOffset: Float) {
        this.widthPercent = width
        this.heightPercent = height
        this.heightOffsetPercent = heightOffset
        invalidate() // Trigger a redraw
    }
    
    fun setOverlayVisible(visible: Boolean) {
        showOverlay = visible
        invalidate()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // This ViewGroup doesn't have children to layout
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateGestureRectangles()
    }
    
    private fun updateGestureRectangles() {
        val screenWidth = width.toFloat()
        val screenHeight = height.toFloat()
        
        if (screenWidth == 0f || screenHeight == 0f) return
        
        // Calculate gesture area dimensions based on percentages
        val gestureWidth = screenWidth * (widthPercent / 100f)
        val gestureHeight = screenHeight * (heightPercent / 100f)
        
        // Calculate vertical position based on offset from top
        val verticalOffset = screenHeight * (heightOffsetPercent / 100f)
        val gestureTop = verticalOffset
        val gestureBottom = (gestureTop + gestureHeight).coerceAtMost(screenHeight)

        // Left gesture area - positioned at the left edge of screen
        leftGestureRect.set(
            0f,
            gestureTop,
            gestureWidth,
            gestureBottom
        )

        // Right gesture area - positioned at the right edge of screen
        rightGestureRect.set(
            screenWidth - gestureWidth,
            gestureTop,
            screenWidth,
            gestureBottom
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!showOverlay) return
        
        updateGestureRectangles()

        // Draw left gesture area
        canvas.drawRect(leftGestureRect, overlayPaint)
        canvas.drawRect(leftGestureRect, strokePaint)

        // Draw right gesture area  
        canvas.drawRect(rightGestureRect, overlayPaint)
        canvas.drawRect(rightGestureRect, strokePaint)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        // Recalculate areas when orientation changes
        post { 
            updateGestureRectangles()
            invalidate()
        }
    }
}
