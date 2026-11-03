// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import helium314.keyboard.keyboard.Key
import helium314.keyboard.latin.common.Colors
import java.util.WeakHashMap

object EinkRendering {
    private const val ALPHA_THRESHOLD = 128
    private val iconCache = WeakHashMap<Drawable, HashMap<RenderedIcon, Bitmap>>()

    private data class RenderedIcon(val width: Int, val height: Int, val color: Int)

    @JvmStatic
    fun antiAlias(colors: Colors) = (colors as? EinkColors)?.style?.antiAlias ?: true

    /**
     * With hardware acceleration KeyboardView redraws every key on each key press; on a slow e-ink phone that
     * costs over 10 ms per frame. Its offscreen buffer path redraws only the changed keys, which also keeps the
     * rest of the panel untouched.
     */
    @JvmStatic
    fun redrawsOnlyChangedKeys(colors: Colors) = colors is EinkColors

    @JvmStatic
    fun hintBorderInset(colors: Colors, context: Context): Float {
        if (colors !is EinkColors) return 0f
        return colors.style.borderWidthPx + TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)
    }

    @JvmStatic
    fun applyTextAntiAlias(view: TextView, colors: Colors) {
        view.paint.isAntiAlias = antiAlias(colors)
    }

    private const val BOLD_WEIGHT = 700

    fun withLabelWeight(typeface: Typeface, colors: Colors?): Typeface {
        if (colors !is EinkColors || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return typeface
        val requested = colors.style.labelWeight
        if (requested == EinkStyle.DEFAULT_LABEL_WEIGHT) return typeface
        val weight = if (typeface.isBold) maxOf(requested, BOLD_WEIGHT) else requested
        return Typeface.create(typeface, weight, typeface.isItalic)
    }

    @JvmStatic
    fun applyToViewTree(view: View, colors: Colors) {
        if (colors !is EinkColors) return
        if (view is TextView) applyTextAntiAlias(view, colors)
        view.isHorizontalScrollBarEnabled = false
        view.isVerticalScrollBarEnabled = false
        view.overScrollMode = View.OVER_SCROLL_NEVER
        if (view is RecyclerView) view.itemAnimator = null
        if (view is ViewGroup) view.children.forEach { applyToViewTree(it, colors) }
    }

    @JvmStatic
    fun isInverted(colors: Colors, key: Key) =
        colors is EinkColors && colors.style.invertPressedKey && key.isPressed

    @JvmStatic
    fun drawsMonochromeIcons(colors: Colors) = colors is EinkColors && !colors.style.antiAlias

    @JvmStatic
    fun drawMonochromeIcon(canvas: Canvas, icon: Drawable, x: Int, y: Int, width: Int, height: Int, @ColorInt color: Int) {
        if (width <= 0 || height <= 0) return
        val renderings = iconCache.getOrPut(icon) { HashMap() }
        val bitmap = renderings.getOrPut(RenderedIcon(width, height, color)) { renderThresholded(icon, width, height, color) }
        canvas.drawBitmap(bitmap, x.toFloat(), y.toFloat(), null)
    }

    // Vector drawables are always rasterized with antialiasing, so the only way to get a
    // pure two-tone icon is to rasterize it and threshold the coverage afterwards.
    internal fun renderThresholded(icon: Drawable, width: Int, height: Int, @ColorInt color: Int): Bitmap {
        val bitmap = createBitmap(width, height)
        icon.setBounds(0, 0, width, height)
        icon.draw(Canvas(bitmap))
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val opaque = color or 0xFF000000.toInt()
        for (i in pixels.indices)
            pixels[i] = if (Color.alpha(pixels[i]) >= ALPHA_THRESHOLD) opaque else Color.TRANSPARENT
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
