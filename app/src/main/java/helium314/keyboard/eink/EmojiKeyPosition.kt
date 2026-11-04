// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.ListPreference

object EmojiKeyPosition {
    const val PREF = "emoji_key_position"
    const val LEFT = "left"
    const val RIGHT = "right"
    const val DEFAULT = LEFT

    @JvmStatic
    fun isRight(prefs: SharedPreferences) = prefs.getString(PREF, DEFAULT) == RIGHT
}

fun einkAdditionalKeyItems(prefs: SharedPreferences): List<String> =
    if (prefs.getBoolean(Settings.PREF_SHOW_EMOJI_KEY, Defaults.PREF_SHOW_EMOJI_KEY)) listOf(EmojiKeyPosition.PREF) else emptyList()

fun createEinkPreferencesSettings(context: Context) = listOf(
    Setting(context, EmojiKeyPosition.PREF, R.string.eink_emoji_key_position) {
        val prefs = LocalContext.current.prefs()
        ListPreference(
            it,
            listOf(
                stringResource(R.string.eink_emoji_key_left) to EmojiKeyPosition.LEFT,
                stringResource(R.string.eink_emoji_key_right) to EmojiKeyPosition.RIGHT,
            ),
            EmojiKeyPosition.DEFAULT,
            onDefault = { prefs.edit { remove(EmojiKeyPosition.PREF) } },
        ) { KeyboardSwitcher.getInstance().reloadKeyboard() }
    },
)
