// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.annotation.ColorInt
import helium314.keyboard.latin.common.Colors

/** Line along the top edge of the keyboard, so it stays apart from an app with the same white background. */
object KeyboardTopLine {
    @JvmStatic
    fun apply(frame: View, colors: Colors) {
        if (colors !is EinkColors || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        frame.foreground = TopLineDrawable(colors.foreground, colors.style.borderWidthPx.toInt())
    }
}

class TopLineDrawable(@ColorInt color: Int, private val thickness: Int) : Drawable() {
    private val paint = Paint().apply {
        this.color = color
        isAntiAlias = false
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), (bounds.top + thickness).toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
