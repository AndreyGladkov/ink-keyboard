// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.edit
import helium314.keyboard.ShadowInputMethodManager2
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowInputMethodManager2::class])
class EinkEmojiTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    private fun applyPrefs(monochrome: Boolean) {
        latinIME.prefs().edit {
            clear()
            putBoolean(EinkStyle.PREF_MONOCHROME_EMOJI, monochrome)
        }
        Settings.getInstance().loadSettings(latinIME)
    }

    @Test fun emojiUseMonochromeFontByDefault() {
        applyPrefs(monochrome = true)
        val expected = EinkEmoji.typeface(latinIME, Settings.getValues().mColors)
        assertSame(expected, KeyboardTypeface.resolve("😀"))
    }

    @Test fun lettersKeepTheirTypeface() {
        applyPrefs(monochrome = true)
        assertNotEquals(EinkEmoji.typeface(latinIME, Settings.getValues().mColors), KeyboardTypeface.resolve("a"))
    }

    @Test fun colorEmojiWhenMonochromeIsOff() {
        applyPrefs(monochrome = false)
        val monochromeFont = EinkEmoji.typeface(latinIME, EinkColors(Color.BLACK, Color.WHITE, EinkStyle(2f, 0f, false, false)))
        assertNotEquals(monochromeFont, KeyboardTypeface.resolve("😀"))
    }

    @Test fun commonEmojiNeedNoThreshold() {
        applyPrefs(monochrome = true)
        assertTrue(!EinkEmoji.needsThreshold(latinIME, Settings.getValues().mColors, "😀"))
    }

    @Test fun emojiPresentationSelectorIsDroppedForMonochromeGlyphs() {
        applyPrefs(monochrome = true)
        assertEquals("\u263A", EinkEmoji.displayText(latinIME, Settings.getValues().mColors, "\u263A\uFE0F"))
    }

    @Test fun displayTextIsUnchangedWithoutMonochromeEmoji() {
        applyPrefs(monochrome = false)
        assertEquals("\u263A\uFE0F", EinkEmoji.displayText(latinIME, Settings.getValues().mColors, "\u263A\uFE0F"))
    }

    @Test fun thresholdedEmojiIsTwoTone() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply { textSize = 120f; textAlign = Paint.Align.CENTER }
        EinkEmoji.drawThresholded(Canvas(bitmap), "😀", 100f, 150f, paint, Color.BLACK)
        val pixels = IntArray(200 * 200).also { bitmap.getPixels(it, 0, 200, 0, 0, 200, 200) }
        assertTrue(pixels.all { it == Color.BLACK || it == Color.TRANSPARENT })
        assertTrue(pixels.any { it == Color.BLACK }, "emoji should leave some dark pixels")
    }
}
