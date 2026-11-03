// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import helium314.keyboard.ShadowInputMethodService
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.ToolbarMode
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodService::class, ShadowLooper::class])
class StripEmojiKeyTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val keyboardSwitcher = KeyboardSwitcher.getInstance()
    private lateinit var inputView: View

    private fun stripStart(enabled: Boolean): View {
        latinIME.prefs().edit { putBoolean(StripEmojiKey.PREF, enabled) }
        Settings.getInstance().loadSettings(latinIME)
        inputView = latinIME.onCreateInputView()
        latinIME.setInputView(inputView)
        keyboardSwitcher.reloadMainKeyboard()
        val wrapper = inputView.findViewById<ViewGroup>(R.id.suggestions_strip_wrapper)
        return wrapper.getChildAt(0)
    }

    // The input view is never attached to a window here, so KeyboardSwitcher.isShowingEmojiPalettes (isShown) stays false.
    private fun emojiShown() = inputView.findViewById<View>(R.id.emoji_palettes_view).visibility == View.VISIBLE

    @Test fun emojiKeyIsAtTheStartOfTheStrip() {
        val start = stripStart(enabled = true)
        assertEquals(ToolbarKey.EMOJI, start.tag)
        val wrapper = start.parent as ViewGroup
        assertEquals(R.id.suggestions_strip_toolbar_key, wrapper.getChildAt(1).id)
    }

    @Test fun emojiKeyOpensEmojiAndReturnsToLetters() {
        val key = stripStart(enabled = true)
        key.performClick()
        assertTrue(emojiShown())
        key.performClick()
        assertFalse(emojiShown())
    }

    @Test fun disabledSettingLeavesStripUnchanged() {
        val start = stripStart(enabled = false)
        assertNotEquals<Any?>(ToolbarKey.EMOJI, start.tag)
        assertEquals(R.id.suggestions_strip_toolbar_key, start.id)
    }

    @Test fun settingIsOfferedUnlessToolbarIsHidden() {
        assertTrue(StripEmojiKey.PREF in einkToolbarItems(ToolbarMode.EXPANDABLE))
        assertTrue(StripEmojiKey.PREF in einkToolbarItems(ToolbarMode.SUGGESTION_STRIP))
        assertFalse(StripEmojiKey.PREF in einkToolbarItems(ToolbarMode.HIDDEN))
    }
}
