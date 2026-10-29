package com.pixelcraft.editor.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive crop rectangle drawn on top of the photo preview.
 *
 * The view is laid out in a wrapper that is exactly the displayed size of
 * the bitmap, so view coordinates and displayed bitmap coordinates share the
 * same scale. The displayed bitmap may be a down-scaled version of the real
 * bitmap, so [getCropRectBitmapCoords] converts through that scale factor.
 *
 * Supports dragging the four corners, dragging the four edges and moving
 * the whole rectangle, with a minimum crop size enforced everywhere.
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private enum class Handle { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE, TOP, BOTTOM, LEFT, RIGHT }

    private var bitmapWidth = 0
    private var bitmapHeight = 0

    // Crop rectangle in view coordinates.
    private val cropRect = RectF()

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x8C000000.toInt()
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACCENT
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ACCENT
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
    }

    private val grabRadius = dp(28f)
    private val minCropSize = dp(56f)
    private val handleSize = dp(22f)

    private var activeHandle = Handle.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    /** Tells the overlay the real bitmap size; resets the selection. */
    fun setBitmapSize(width: Int, height: Int) {
        bitmapWidth = width
        bitmapHeight = height
        if (width > 0 && height > 0 && this.width > 0 && this.height > 0) {
            resetToFullArea()
        }
    }

    /** Expands the crop rectangle back over the whole photo. */
    fun resetToFullArea() {
        cropRect.set(0f, 0f, width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Size changes only happen when the photo (or its orientation)
        // changed, so restart from a full-area selection.
        if (w > 0 && h > 0 && bitmapWidth > 0) {
            resetToFullArea()
        }
    }

    /**
     * A plain View asked for wrap_content would fill the whole preview card;
     * we must be exactly as large as the photo preview ImageView beside us,
     * so overlay coordinates match displayed bitmap pixels 1:1.
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentGroup = parent as? ViewGroup
        if (parentGroup != null) {
            for (i in 0 until parentGroup.childCount) {
                val sibling = parentGroup.getChildAt(i)
                if (sibling is ImageView && sibling.measuredWidth > 0 && sibling.measuredHeight > 0) {
                    setMeasuredDimension(sibling.measuredWidth, sibling.measuredHeight)
                    return
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** Converts the current selection into bitmap-pixel coordinates. */
    fun getCropRectBitmapCoords(): Rect? {
        if (bitmapWidth <= 0 || bitmapHeight <= 0 || width <= 0 || height <= 0) return null
        val scale = width.toFloat() / bitmapWidth
        return Rect(
            (cropRect.left / scale).roundToInt().coerceIn(0, bitmapWidth),
            (cropRect.top / scale).roundToInt().coerceIn(0, bitmapHeight),
            (cropRect.right / scale).roundToInt().coerceIn(0, bitmapWidth),
            (cropRect.bottom / scale).roundToInt().coerceIn(0, bitmapHeight)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cropRect.isEmpty) return

        // Darken everything outside the selection.
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, dimPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, dimPaint)

        // Rule-of-thirds grid.
        val thirdW = cropRect.width() / 3f
        val thirdH = cropRect.height() / 3f
        for (i in 1..2) {
            val x = cropRect.left + thirdW * i
            val y = cropRect.top + thirdH * i
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint)
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint)
        }

        canvas.drawRect(cropRect, borderPaint)
        drawHandle(canvas, cropRect.left, cropRect.top)
        drawHandle(canvas, cropRect.right, cropRect.top)
        drawHandle(canvas, cropRect.left, cropRect.bottom)
        drawHandle(canvas, cropRect.right, cropRect.bottom)
    }

    private fun drawHandle(canvas: Canvas, cx: Float, cy: Float) {
        val half = handleSize / 2f
        canvas.drawRoundRect(
            cx - half, cy - half, cx + half, cy + half,
            dp(6f), dp(6f), handleFillPaint
        )
        canvas.drawRoundRect(
            cx - half, cy - half, cx + half, cy + half,
            dp(6f), dp(6f), handleStrokePaint
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = handleAt(event.x, event.y)
                if (activeHandle == Handle.NONE) return false
                lastTouchX = event.x
                lastTouchY = event.y
                // Stop the ViewPager below from treating this as a page swipe.
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                lastTouchX = event.x
                lastTouchY = event.y
                drag(activeHandle, dx, dy)
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                activeHandle = Handle.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleAt(x: Float, y: Float): Handle {
        // Corners win over edges, edges win over the interior.
        if (near(x, cropRect.left) && near(y, cropRect.top)) return Handle.TOP_LEFT
        if (near(x, cropRect.right) && near(y, cropRect.top)) return Handle.TOP_RIGHT
        if (near(x, cropRect.left) && near(y, cropRect.bottom)) return Handle.BOTTOM_LEFT
        if (near(x, cropRect.right) && near(y, cropRect.bottom)) return Handle.BOTTOM_RIGHT

        val onVerticalBand = y > cropRect.top - grabRadius && y < cropRect.bottom + grabRadius
        val onHorizontalBand = x > cropRect.left - grabRadius && x < cropRect.right + grabRadius

        if (onVerticalBand && near(y, cropRect.top)) return Handle.TOP
        if (onVerticalBand && near(y, cropRect.bottom)) return Handle.BOTTOM
        if (onHorizontalBand && near(x, cropRect.left)) return Handle.LEFT
        if (onHorizontalBand && near(x, cropRect.right)) return Handle.RIGHT

        return if (cropRect.contains(x, y)) Handle.MOVE else Handle.NONE
    }

    private fun near(value: Float, target: Float): Boolean = abs(value - target) <= grabRadius

    private fun drag(handle: Handle, dx: Float, dy: Float) {
        val maxX = width.toFloat()
        val maxY = height.toFloat()
        when (handle) {
            Handle.TOP_LEFT -> {
                cropRect.left = clamp(cropRect.left + dx, 0f, cropRect.right - minCropSize)
                cropRect.top = clamp(cropRect.top + dy, 0f, cropRect.bottom - minCropSize)
            }
            Handle.TOP_RIGHT -> {
                cropRect.right = clamp(cropRect.right + dx, cropRect.left + minCropSize, maxX)
                cropRect.top = clamp(cropRect.top + dy, 0f, cropRect.bottom - minCropSize)
            }
            Handle.BOTTOM_LEFT -> {
                cropRect.left = clamp(cropRect.left + dx, 0f, cropRect.right - minCropSize)
                cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minCropSize, maxY)
            }
            Handle.BOTTOM_RIGHT -> {
                cropRect.right = clamp(cropRect.right + dx, cropRect.left + minCropSize, maxX)
                cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minCropSize, maxY)
            }
            Handle.TOP -> cropRect.top = clamp(cropRect.top + dy, 0f, cropRect.bottom - minCropSize)
            Handle.BOTTOM -> cropRect.bottom = clamp(cropRect.bottom + dy, cropRect.top + minCropSize, maxY)
            Handle.LEFT -> cropRect.left = clamp(cropRect.left + dx, 0f, cropRect.right - minCropSize)
            Handle.RIGHT -> cropRect.right = clamp(cropRect.right + dx, cropRect.left + minCropSize, maxX)
            Handle.MOVE -> {
                val newLeft = clamp(cropRect.left + dx, 0f, maxX - cropRect.width())
                val newTop = clamp(cropRect.top + dy, 0f, maxY - cropRect.height())
                cropRect.offsetTo(newLeft, newTop)
            }
            Handle.NONE -> Unit
        }
    }

    private fun clamp(value: Float, lower: Float, upper: Float): Float =
        max(lower, min(upper, value))

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        val ACCENT = 0xFF5B8DEF.toInt()
    }
}
