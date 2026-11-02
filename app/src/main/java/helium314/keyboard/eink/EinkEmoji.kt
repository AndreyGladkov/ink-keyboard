// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import android.util.LruCache
import androidx.annotation.ColorInt
import androidx.core.graphics.createBitmap
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.isEmoji

object EinkEmoji {
    private const val FONT_ASSET = "fonts/NotoEmoji.ttf"
    private const val ALPHA_THRESHOLD = 128
    private const val LUMA_THRESHOLD = 128

    private var typeface: Typeface? = null
    private var strictTypeface: Typeface? = null
    private val glyphPresence = HashMap<String, Boolean>()
    private val thresholdedEmojis = LruCache<String, Bitmap>(64)

    @JvmStatic
    fun isMonochrome(colors: Colors?) = colors is EinkColors && colors.style.monochromeEmoji

    @JvmStatic
    fun typeface(context: Context, colors: Colors?): Typeface? {
        if (!isMonochrome(colors)) return null
        return typeface ?: Typeface.createFromAsset(context.assets, FONT_ASSET).also { typeface = it }
    }

    private const val EMOJI_PRESENTATION_SELECTOR = "\uFE0F"

    // The emoji presentation selector makes Android pick the system color font even when the
    // monochrome font has the glyph, so it is dropped for display whenever the glyph exists without it.
    @JvmStatic
    fun displayText(context: Context, colors: Colors?, text: String): String {
        if (!isMonochrome(colors) || !text.contains(EMOJI_PRESENTATION_SELECTOR) || !isEmoji(text)) return text
        val stripped = text.replace(EMOJI_PRESENTATION_SELECTOR, "")
        return if (stripped.isNotEmpty() && hasMonochromeGlyph(context, stripped)) stripped else text
    }

    @JvmStatic
    fun needsThreshold(context: Context, colors: Colors?, text: String): Boolean {
        if (!isMonochrome(colors) || !isEmoji(text)) return false
        return !hasMonochromeGlyph(context, text)
    }

    private fun hasMonochromeGlyph(context: Context, text: String): Boolean = glyphPresence.getOrPut(text) {
        val strict = strictTypeface(context) ?: return@getOrPut true
        Paint().apply { setTypeface(strict) }.hasGlyph(text)
    }

    // A typeface from an asset silently falls back to the system color emoji font,
    // so glyph presence can only be checked on a typeface built without system fallback.
    private fun strictTypeface(context: Context): Typeface? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        strictTypeface?.let { return it }
        val font = Font.Builder(context.assets, FONT_ASSET).build()
        return Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build()).build().also { strictTypeface = it }
    }

    @JvmStatic
    fun drawThresholded(canvas: Canvas, text: String, x: Float, baseline: Float, paint: Paint, @ColorInt color: Int) {
        val textPaint = Paint(paint).apply {
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        val width = textPaint.measureText(text)
        val left = when (paint.textAlign) {
            Paint.Align.CENTER -> x - width / 2
            Paint.Align.RIGHT -> x - width
            else -> x
        }
        val bounds = Rect().also { textPaint.getTextBounds(text, 0, text.length, it) }
        if (bounds.isEmpty) return
        val key = "$text|${textPaint.textSize}|$color"
        val bitmap = thresholdedEmojis.get(key) ?: render(text, textPaint, bounds, color).also { thresholdedEmojis.put(key, it) }
        canvas.drawBitmap(bitmap, left + bounds.left, baseline + bounds.top, null)
    }

    private fun render(text: String, paint: Paint, bounds: Rect, @ColorInt color: Int): Bitmap {
        val bitmap = createBitmap(bounds.width(), bounds.height())
        Canvas(bitmap).drawText(text, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val opaque = color or 0xFF000000.toInt()
        for (i in pixels.indices) {
            val p = pixels[i]
            val luma = (76 * Color.red(p) + 150 * Color.green(p) + 30 * Color.blue(p)) shr 8
            pixels[i] = if (Color.alpha(p) >= ALPHA_THRESHOLD && luma < LUMA_THRESHOLD) opaque else Color.TRANSPARENT
        }
        bitmap.setPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return bitmap
    }
}
