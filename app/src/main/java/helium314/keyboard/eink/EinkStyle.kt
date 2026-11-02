// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.content.SharedPreferences
import android.util.TypedValue

data class EinkStyle(
    val borderWidthPx: Float,
    val cornerRadiusPx: Float,
    val antiAlias: Boolean,
    val invertPressedKey: Boolean,
    val labelWeight: Int = DEFAULT_LABEL_WEIGHT,
    val monochromeEmoji: Boolean = DEFAULT_MONOCHROME_EMOJI,
) {
    companion object {
        const val PREF_BORDER_WIDTH = "eink_border_width"
        const val PREF_CORNER_RADIUS = "eink_corner_radius"
        const val PREF_ANTIALIAS = "eink_antialias"
        const val PREF_INVERT_PRESSED_KEY = "eink_invert_pressed_key"
        const val PREF_LABEL_WEIGHT = "eink_label_weight"
        const val PREF_MONOCHROME_EMOJI = "eink_monochrome_emoji"

        const val DEFAULT_BORDER_WIDTH_DP = 1.5f
        const val DEFAULT_CORNER_RADIUS_DP = 4f
        const val DEFAULT_ANTIALIAS = false
        const val DEFAULT_INVERT_PRESSED_KEY = false
        const val DEFAULT_LABEL_WEIGHT = 400
        const val DEFAULT_MONOCHROME_EMOJI = true

        fun read(context: Context, prefs: SharedPreferences): EinkStyle {
            val metrics = context.resources.displayMetrics
            fun dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics)
            return EinkStyle(
                borderWidthPx = wholePixels(dpToPx(prefs.getFloat(PREF_BORDER_WIDTH, DEFAULT_BORDER_WIDTH_DP))),
                cornerRadiusPx = dpToPx(prefs.getFloat(PREF_CORNER_RADIUS, DEFAULT_CORNER_RADIUS_DP)),
                antiAlias = prefs.getBoolean(PREF_ANTIALIAS, DEFAULT_ANTIALIAS),
                invertPressedKey = prefs.getBoolean(PREF_INVERT_PRESSED_KEY, DEFAULT_INVERT_PRESSED_KEY),
                labelWeight = prefs.getInt(PREF_LABEL_WEIGHT, DEFAULT_LABEL_WEIGHT),
                monochromeEmoji = prefs.getBoolean(PREF_MONOCHROME_EMOJI, DEFAULT_MONOCHROME_EMOJI),
            )
        }

        // Without antialiasing a fractional stroke rounds differently on each key edge,
        // so borders of equal keys would visibly differ by a pixel.
        private fun wholePixels(px: Float) = maxOf(1f, Math.round(px).toFloat())
    }
}
