// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Region
import android.graphics.RegionIterator
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
import kotlin.math.abs

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
    private const val STEM_FIT_RANGE = 80
    private const val STEM_FIT_STEP = 5
    private val STEM_REFERENCES = listOf("l", "п", "н", "т")
    private const val STEM_MEASURE_SCALE = 8f
    private const val STEM_ROW = 0.3f
    private const val STEM_TOLERANCE = 0.02f

    private data class StemFit(val typeface: Typeface, val textSize: Float, val weight: Int)
    private val stemFits = HashMap<StemFit, Typeface>()

    /**
     * Without antialiasing a stem covers as many pixels as pixel centers it contains, so a 5.5 px stem is drawn
     * 5 or 6 px wide depending on its position and the two stems of "П" differ. A stem of integral width always
     * covers the same number of pixels, so this sets the weight axis closest to the wanted weight at which the
     * stem width at the paint's size is closest to an integer. System Roboto is a variable font without hinting
     * instructions, so the axis is the only way to snap stems; Typeface.create only picks named instances.
     */
    @JvmStatic
    fun fitStems(paint: Paint, colors: Colors) {
        if (colors !is EinkColors || colors.style.antiAlias || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val base = paint.typeface ?: Typeface.DEFAULT
        val wanted = if (base.isBold) maxOf(colors.style.labelWeight, BOLD_WEIGHT) else colors.style.labelWeight
        val key = StemFit(base, paint.textSize, wanted)
        // Setting variation settings on the paint creates a new typeface on every call, too slow for each key draw.
        paint.typeface = synchronized(stemFits) { stemFits.getOrPut(key) { fittedTypeface(base, paint.textSize, wanted) } }
    }

    private fun fittedTypeface(base: Typeface, textSize: Float, wanted: Int): Typeface {
        val probe = Paint().apply {
            typeface = base
            this.textSize = textSize * STEM_MEASURE_SCALE
        }
        var best: String? = null
        var bestError = Float.MAX_VALUE
        for (offset in 0..STEM_FIT_RANGE step STEM_FIT_STEP) {
            for (weight in listOf(wanted - offset, wanted + offset).distinct()) {
                if (weight !in 100..900) continue
                val axis = "'wght' $weight"
                if (!probe.setFontVariationSettings(axis)) return base
                val stems = STEM_REFERENCES.flatMap { stemWidths(probe, it) }
                if (stems.isEmpty()) return base
                val error = stems.sumOf { abs(it - Math.round(it)).toDouble() }.toFloat() / stems.size
                if (error < bestError - STEM_TOLERANCE) {
                    best = axis
                    bestError = error
                }
            }
        }
        probe.typeface = base
        probe.fontVariationSettings = best
        return probe.typeface ?: base
    }

    // Widths of the runs a horizontal line crosses in the upper part of the glyph, which for these
    // references are the vertical stems; measured on the enlarged outline for sub-pixel precision.
    private fun stemWidths(probe: Paint, text: String): List<Float> {
        val path = Path()
        probe.getTextPath(text, 0, text.length, 0f, 0f, path)
        val bounds = RectF()
        path.computeBounds(bounds, true)
        val y = (bounds.top + bounds.height() * STEM_ROW).toInt()
        val region = Region()
        region.setPath(path, Region(bounds.left.toInt() - 1, y, bounds.right.toInt() + 1, y + 1))
        val widths = ArrayList<Float>()
        val iterator = RegionIterator(region)
        val rect = Rect()
        while (iterator.next(rect)) widths.add(rect.width() / STEM_MEASURE_SCALE)
        return widths
    }

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
