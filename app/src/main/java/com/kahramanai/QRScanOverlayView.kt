package com.kahramanai

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import androidx.core.graphics.toColorInt

class QRScanOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val borderPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val dimPaint = Paint().apply {
        color = "#AA000000".toColorInt() // semi-transparent black
    }

    private val scanLinePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        isAntiAlias = true
    }

    private var scanLinePosition = 0f
    private var frame: RectF = RectF() // Initialize as empty
    private var animator: ValueAnimator? = null

    // The init block is now empty because initialization depends on size.
    init {
        // We ensure onDraw is called for custom drawing.
        setWillNotDraw(false)
    }

    /**
     * This is the correct lifecycle method to initialize size-dependent properties.
     * It's called when the view is first laid out and whenever its size changes.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calculate the frame dimensions based on the new width and height
        frame.set(
            w * 0.15f,
            h * 0.3f,
            w * 0.85f,
            h * 0.7f
        )

        // Stop any existing animator before creating a new one
        animator?.cancel()

        // Create and configure the animator with the correct frame dimensions
        animator = ValueAnimator.ofFloat(frame.top, frame.bottom).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()

            addUpdateListener {
                scanLinePosition = it.animatedValue as Float
                invalidate() // Redraw the view on each animation frame
            }
        }

        // If the view is already visible, start the animation.
        if (isVisible) {
            animator?.start()
        }
    }

    fun stopOverlay() {
        visibility = GONE
        animator?.cancel()
    }

    fun startOverlay() {
        visibility = VISIBLE
        // The animator is already configured, we just need to start it.
        // It might already be running if onSizeChanged was called while visible.
        if (animator?.isStarted == false) {
            animator?.start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw if the frame is empty (hasn't been laid out yet)
        if (frame.isEmpty) {
            return
        }

        // Dim background outside of frame
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top, dimPaint)
        canvas.drawRect(0f, frame.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, frame.top, frame.left, frame.bottom, dimPaint)
        canvas.drawRect(frame.right, frame.top, width.toFloat(), frame.bottom, dimPaint)

        // Draw green border
        canvas.drawRect(frame, borderPaint)

        // Draw animated scan line
        canvas.drawLine(frame.left, scanLinePosition, frame.right, scanLinePosition, scanLinePaint)
    }
}