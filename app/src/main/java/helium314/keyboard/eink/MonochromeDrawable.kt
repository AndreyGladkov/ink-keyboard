// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt

class MonochromeDrawable(val inner: Drawable, @ColorInt private val color: Int) : Drawable() {
    private var rendered: Bitmap? = null

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()
        if (width <= 0 || height <= 0) return
        val bitmap = rendered?.takeIf { it.width == width && it.height == height }
            ?: EinkRendering.renderThresholded(inner, width, height, color).also { rendered = it }
        canvas.drawBitmap(bitmap, bounds.left.toFloat(), bounds.top.toFloat(), null)
    }

    override fun getIntrinsicWidth() = inner.intrinsicWidth

    override fun getIntrinsicHeight() = inner.intrinsicHeight

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    companion object {
        fun wrap(drawable: Drawable, @ColorInt color: Int): Drawable =
            MonochromeDrawable((drawable as? MonochromeDrawable)?.inner ?: drawable, color)
    }
}
