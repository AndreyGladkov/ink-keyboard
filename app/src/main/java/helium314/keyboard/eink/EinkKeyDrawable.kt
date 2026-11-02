// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class EinkKeyDrawable(
    @ColorInt foreground: Int,
    @ColorInt private val fill: Int?,
    private val einkStyle: EinkStyle,
    private val normalBorder: Boolean,
    private val pressedLook: PressedLook,
) : Drawable() {
    enum class PressedLook { NONE, FILL, THICK_BORDER }

    private val borderPaint = strokePaint(foreground, einkStyle.borderWidthPx)
    private val thickBorderPaint = strokePaint(foreground, einkStyle.borderWidthPx * THICK_BORDER_FACTOR)
    private val foregroundFillPaint = fillPaint(foreground)
    private val backgroundFillPaint = fill?.let { fillPaint(it) }
    private val rect = RectF()
    private var pressed = false

    override fun draw(canvas: Canvas) {
        backgroundFillPaint?.let { drawShape(canvas, 0f, it) }
        val look = if (pressed) pressedLook else PressedLook.NONE
        when (look) {
            PressedLook.FILL -> drawShape(canvas, einkStyle.borderWidthPx, foregroundFillPaint)
            PressedLook.THICK_BORDER -> drawShape(canvas, thickBorderPaint.strokeWidth, thickBorderPaint)
            PressedLook.NONE -> if (normalBorder) drawShape(canvas, borderPaint.strokeWidth, borderPaint)
        }
    }

    private fun drawShape(canvas: Canvas, strokeWidth: Float, paint: Paint) {
        rect.set(bounds)
        rect.inset(strokeWidth / 2, strokeWidth / 2)
        val radius = einkStyle.cornerRadiusPx
        if (radius > 0) canvas.drawRoundRect(rect, radius, radius, paint)
        else canvas.drawRect(rect, paint)
    }

    private fun strokePaint(@ColorInt color: Int, width: Float) = Paint().apply {
        style = Paint.Style.STROKE
        this.color = color
        strokeWidth = width
        isAntiAlias = einkStyle.antiAlias
    }

    private fun fillPaint(@ColorInt color: Int) = Paint().apply {
        style = Paint.Style.FILL
        this.color = color
        isAntiAlias = einkStyle.antiAlias
    }

    override fun isStateful() = pressedLook != PressedLook.NONE

    override fun onStateChange(state: IntArray): Boolean {
        val nowPressed = android.R.attr.state_pressed in state
        if (nowPressed == pressed) return false
        pressed = nowPressed
        return true
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    private companion object {
        const val THICK_BORDER_FACTOR = 3
    }
}
