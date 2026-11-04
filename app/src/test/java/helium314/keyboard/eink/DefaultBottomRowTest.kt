// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.view.inputmethod.EditorInfo
import androidx.core.content.edit
import helium314.keyboard.ShadowInputMethodManager2
import helium314.keyboard.ShadowProximityInfo
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardId
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.internal.KeyboardBuilder
import helium314.keyboard.keyboard.internal.KeyboardParams
import helium314.keyboard.keyboard.internal.UniqueKeysCache
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.RichInputMethodSubtype
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.locale
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class])
class DefaultBottomRowTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    private fun withSystemLanguage(qualifiers: String) {
        RuntimeEnvironment.setQualifiers(qualifiers)
        latinIME.prefs().edit { clear() }
        SubtypeSettings.reloadSystemLocales(latinIME)
        SubtypeSettings.reloadEnabledSubtypes(latinIME)
        Settings.getInstance().loadSettings(latinIME)
    }

    private fun defaultLanguages() = SubtypeSettings.getEnabledSubtypes(true).map { it.locale().toLanguageTag() }

    @Test fun englishIsAddedToNonEnglishSystemLanguage() {
        withSystemLanguage("ru")
        assertEquals(listOf("ru", "en-US"), defaultLanguages())
    }

    @Test fun englishSystemLanguageStaysAlone() {
        withSystemLanguage("en-rUS")
        assertEquals(listOf("en-US"), defaultLanguages())
    }

    @Test fun globeIsShownLeftOfCommaByDefault() {
        withSystemLanguage("ru")
        val bottomRow = bottomRow(buildRussianLetters())
        val globe = bottomRow.indexOf(KeyCode.LANGUAGE_SWITCH)
        assertTrue(globe >= 0, "no globe in $bottomRow")
        assertEquals(','.code, bottomRow[globe + 1])
    }

    @Test fun emojiKeyIsShownBetweenPeriodAndEnterByDefault() {
        withSystemLanguage("ru")
        val bottomRow = bottomRow(buildRussianLetters())
        val emoji = bottomRow.indexOf(KeyCode.EMOJI)
        assertTrue(emoji >= 0, "no emoji key in $bottomRow")
        assertEquals('.'.code, bottomRow[emoji - 1])
        assertEquals(bottomRow.lastIndex, emoji + 1, "emoji key is not next to the action key in $bottomRow")
    }

    private fun bottomRow(keyboard: Keyboard): List<Int> {
        val lastY = keyboard.sortedKeys.maxOf { it.y }
        return keyboard.sortedKeys.filter { it.y == lastY }.sortedBy { it.x }.map { it.code }
    }

    private fun buildRussianLetters(): Keyboard {
        val layoutParams = KeyboardLayoutSet.Params()
        fun set(name: String, value: Any) =
            KeyboardLayoutSet.Params::class.java.getDeclaredField(name).apply { isAccessible = true }.set(layoutParams, value)
        set("editorInfo", EditorInfo())
        set("subtype", RichInputMethodSubtype.get(SubtypeSettings.getEnabledSubtypes(true).first()))
        set("keyboardWidth", 1080)
        set("keyboardHeight", 700)
        set("settingsValues", Settings.getValues())
        val builder = KeyboardBuilder(latinIME, KeyboardParams(UniqueKeysCache.NO_CACHE))
        builder.load(KeyboardId(KeyboardElement.ALPHABET, layoutParams))
        return builder.build()
    }
}
