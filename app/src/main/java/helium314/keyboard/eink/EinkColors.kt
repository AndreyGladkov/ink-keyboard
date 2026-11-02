// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.res.TypedArray
import android.graphics.ColorFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.ColorType.*
import helium314.keyboard.latin.common.Colors

class EinkColors(
    @ColorInt val foreground: Int,
    @ColorInt val background: Int,
    val style: EinkStyle,
) : Colors {
    override val themeStyle = KeyboardTheme.STYLE_EINK
    override val hasKeyBorders = true
    override val accentShiftedIcon = false

    private val foregroundFilter = srcInFilter(foreground)
    private val backgroundFilter = srcInFilter(background)

    override fun get(color: ColorType): Int = if (color in BACKGROUNDS) background else foreground

    @ColorInt
    fun contentColor(onInvertedKey: Boolean) = if (onInvertedKey) background else foreground

    override fun setColor(drawable: Drawable, color: ColorType) {
        drawable.colorFilter = filterFor(color)
    }

    override fun setColor(view: ImageView, color: ColorType) {
        val drawable = view.drawable
        if (style.antiAlias || drawable == null) view.colorFilter = filterFor(color)
        else view.setImageDrawable(MonochromeDrawable.wrap(drawable, get(color)))
    }

    override fun setBackground(view: View, color: ColorType) {
        view.background = if (color in OUTLINED) panel() else ColorDrawable(background)
    }

    override fun selectAndColorDrawable(attr: TypedArray, color: ColorType): Drawable =
        if (color in SELECTABLE) selectable() else outline()

    private val pressedKeyLook get() =
        if (style.invertPressedKey) EinkKeyDrawable.PressedLook.FILL else EinkKeyDrawable.PressedLook.NONE

    fun outline(): Drawable = EinkKeyDrawable(foreground, null, style, normalBorder = true, pressedKeyLook)

    fun panel(): Drawable = EinkKeyDrawable(foreground, background, style, normalBorder = true, EinkKeyDrawable.PressedLook.NONE)

    fun selectable(): Drawable = EinkKeyDrawable(
        foreground, null, style, normalBorder = false,
        if (style.invertPressedKey) EinkKeyDrawable.PressedLook.FILL else EinkKeyDrawable.PressedLook.THICK_BORDER,
    )

    fun divider(metrics: DisplayMetrics): Drawable = GradientDrawable().apply {
        setColor(foreground)
        setSize(style.borderWidthPx.toInt(), TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DIVIDER_HEIGHT_DP, metrics).toInt())
    }

    private fun filterFor(color: ColorType): ColorFilter = if (color in BACKGROUNDS) backgroundFilter else foregroundFilter

    companion object {
        private const val DIVIDER_HEIGHT_DP = 24f
        private val BACKGROUNDS = setOf(
            ACTION_KEY_BACKGROUND, ACTION_KEY_POPUP_KEYS_BACKGROUND, AUTOFILL_BACKGROUND_CHIP,
            CLIPBOARD_SUGGESTION_BACKGROUND, EMOJI_SEARCH_BACKGROUND, FUNCTIONAL_KEY_BACKGROUND,
            GESTURE_PREVIEW, KEY_BACKGROUND, KEY_PREVIEW_BACKGROUND, MAIN_BACKGROUND,
            MORE_SUGGESTIONS_BACKGROUND, MORE_SUGGESTIONS_WORD_BACKGROUND, NAVIGATION_BAR,
            POPUP_KEYS_BACKGROUND, SPACE_BAR_BACKGROUND, STRIP_BACKGROUND,
            TOOL_BAR_EXPAND_KEY_BACKGROUND, TOOL_BAR_KEY_ENABLED_BACKGROUND,
        )
        private val SELECTABLE = setOf(
            ACTION_KEY_POPUP_KEYS_BACKGROUND, MORE_SUGGESTIONS_WORD_BACKGROUND, POPUP_KEYS_BACKGROUND,
        )
        private val OUTLINED = setOf(
            CLIPBOARD_SUGGESTION_BACKGROUND, FUNCTIONAL_KEY_BACKGROUND, KEY_BACKGROUND,
            KEY_PREVIEW_BACKGROUND, MORE_SUGGESTIONS_BACKGROUND, POPUP_KEYS_BACKGROUND, SPACE_BAR_BACKGROUND,
        )

        private fun srcInFilter(@ColorInt color: Int): ColorFilter =
            BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_IN)!!
    }
}
