// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.ToolbarMode
import helium314.keyboard.latin.utils.createToolbarKey
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.SwitchPreference

/** Emoji key fixed at the start of the suggestion strip, so the globe key only has to switch languages. */
object StripEmojiKey {
    const val PREF = "strip_emoji_key"
    const val DEFAULT = true

    fun isEnabled(prefs: SharedPreferences) = prefs.getBoolean(PREF, DEFAULT)

    fun addTo(strip: ViewGroup, size: Int, colors: Colors, onClick: View.OnClickListener) {
        if (!isEnabled(strip.context.prefs())) return
        val key = createToolbarKey(strip.context, ToolbarKey.EMOJI)
        key.layoutParams = LinearLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER_VERTICAL }
        key.setOnClickListener(onClick)
        colors.setColor(key, ColorType.TOOL_BAR_KEY)
        colors.setBackground(key, ColorType.STRIP_BACKGROUND)
        strip.addView(key, 0)
    }
}

fun einkToolbarItems(toolbarMode: ToolbarMode): List<String> =
    if (toolbarMode == ToolbarMode.HIDDEN) emptyList() else listOf(StripEmojiKey.PREF)

fun createEinkToolbarSettings(context: Context) = listOf(
    Setting(context, StripEmojiKey.PREF, R.string.eink_strip_emoji_key, R.string.eink_strip_emoji_key_summary) {
        SwitchPreference(it, StripEmojiKey.DEFAULT) { KeyboardSwitcher.getInstance().setThemeNeedsReload() }
    },
)
