// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.content.SharedPreferences
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.SliderPreference
import helium314.keyboard.settings.preferences.SwitchPreference

fun einkAppearanceItems(prefs: SharedPreferences): List<String> = einkStyleItems(prefs) + a9Items()

private fun a9Items(): List<String> = if (HisenseA9.isCurrentDevice()) listOf(A9ScreenClearer.PREF_CLEAR_ON_HIDE) else emptyList()

private fun einkStyleItems(prefs: SharedPreferences): List<String> =
    if (prefs.getString(Settings.PREF_THEME_STYLE, Defaults.PREF_THEME_STYLE) != KeyboardTheme.STYLE_EINK) emptyList()
    else listOf(
        EinkStyle.PREF_BORDER_WIDTH,
        EinkStyle.PREF_CORNER_RADIUS,
        EinkStyle.PREF_LABEL_WEIGHT,
        EinkStyle.PREF_ANTIALIAS,
        EinkStyle.PREF_INVERT_PRESSED_KEY,
        EinkStyle.PREF_MONOCHROME_EMOJI,
    )

fun createEinkAppearanceSettings(context: Context) = listOf(
    Setting(context, EinkStyle.PREF_BORDER_WIDTH, R.string.eink_border_width) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = EinkStyle.DEFAULT_BORDER_WIDTH_DP,
            range = 0.5f..4f,
            description = { "%.1f dp".format(it) },
        ) { reloadKeyboardTheme() }
    },
    Setting(context, EinkStyle.PREF_CORNER_RADIUS, R.string.eink_corner_radius) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = EinkStyle.DEFAULT_CORNER_RADIUS_DP,
            range = 0f..16f,
            stepSize = 1,
            description = { "${it.toInt()} dp" },
        ) { reloadKeyboardTheme() }
    },
    Setting(context, EinkStyle.PREF_LABEL_WEIGHT, R.string.eink_label_weight, R.string.eink_label_weight_summary) { setting ->
        SliderPreference(
            name = setting.title,
            key = setting.key,
            default = EinkStyle.DEFAULT_LABEL_WEIGHT,
            range = 100f..900f,
            stepSize = 100,
            description = { it.toString() },
        ) { reloadKeyboardTheme() }
    },
    Setting(context, EinkStyle.PREF_ANTIALIAS, R.string.eink_antialias, R.string.eink_antialias_summary) {
        SwitchPreference(it, EinkStyle.DEFAULT_ANTIALIAS) { reloadKeyboardTheme() }
    },
    Setting(context, EinkStyle.PREF_INVERT_PRESSED_KEY, R.string.eink_invert_pressed_key, R.string.eink_invert_pressed_key_summary) {
        SwitchPreference(it, EinkStyle.DEFAULT_INVERT_PRESSED_KEY) { reloadKeyboardTheme() }
    },
    Setting(context, EinkStyle.PREF_MONOCHROME_EMOJI, R.string.eink_monochrome_emoji, R.string.eink_monochrome_emoji_summary) {
        SwitchPreference(it, EinkStyle.DEFAULT_MONOCHROME_EMOJI) { reloadKeyboardTheme() }
    },
    Setting(context, A9ScreenClearer.PREF_CLEAR_ON_HIDE, R.string.eink_a9_clear_on_hide, R.string.eink_a9_clear_on_hide_summary) {
        SwitchPreference(it, A9ScreenClearer.DEFAULT_CLEAR_ON_HIDE)
    },
)

private fun reloadKeyboardTheme() = KeyboardSwitcher.getInstance().setThemeNeedsReload()
