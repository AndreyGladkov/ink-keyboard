// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.content.edit
import helium314.keyboard.ShadowInputMethodManager2
import helium314.keyboard.ShadowProximityInfo
import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.keyboard.KeyboardElement
import helium314.keyboard.keyboard.KeyboardId
import helium314.keyboard.keyboard.KeyboardLayoutSet
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardView
import helium314.keyboard.keyboard.internal.KeyboardBuilder
import helium314.keyboard.keyboard.internal.KeyboardParams
import helium314.keyboard.keyboard.internal.UniqueKeysCache
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodSubtype
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeUtilsAdditional
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowInputMethodManager2::class, ShadowProximityInfo::class])
class EinkKeyboardRenderingTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)

    @Test fun defaultsSelectEinkStyle() {
        applyPrefs()
        val colors = assertIs<EinkColors>(Settings.getValues().mColors)
        assertEquals(Color.BLACK, colors.foreground)
        assertEquals(Color.WHITE, colors.background)
        assertEquals(false, colors.style.antiAlias)
    }

    @Test fun blackOnWhiteKeyboardIsPureBlackAndWhite() {
        applyPrefs()
        assertPureBlackAndWhite(renderKeyboard(KeyboardElement.ALPHABET))
    }

    @Test fun whiteOnBlackKeyboardIsPureBlackAndWhite() {
        applyPrefs { putString(Settings.PREF_THEME_COLORS, KeyboardTheme.THEME_EINK_BLACK) }
        assertPureBlackAndWhite(renderKeyboard(KeyboardElement.ALPHABET))
    }

    @Test fun symbolsKeyboardIsPureBlackAndWhite() {
        applyPrefs()
        assertPureBlackAndWhite(renderKeyboard(KeyboardElement.SYMBOLS))
    }

    @Test fun invertedPressedKeyIsPureBlackAndWhite() {
        applyPrefs { putBoolean(EinkStyle.PREF_INVERT_PRESSED_KEY, true) }
        val unpressed = renderKeyboard(KeyboardElement.ALPHABET)
        val pressed = renderKeyboard(KeyboardElement.ALPHABET) { it.sortedKeys.first { key -> key.label == "q" }.onPressed() }
        assertPureBlackAndWhite(pressed)
        assertTrue(countPixels(pressed, Color.BLACK) > countPixels(unpressed, Color.BLACK), "pressed key should be filled")
    }

    @Test fun antialiasingProducesIntermediateShades() {
        applyPrefs { putBoolean(EinkStyle.PREF_ANTIALIAS, true) }
        val bitmap = renderKeyboard(KeyboardElement.ALPHABET)
        assertTrue(bitmap.pixels().any { it != Color.BLACK && it != Color.WHITE })
    }

    private fun applyPrefs(changes: SharedPreferences.Editor.() -> Unit = {}) {
        latinIME.prefs().edit {
            clear()
            changes()
        }
        Settings.getInstance().loadSettings(latinIME)
    }

    private fun renderKeyboard(element: KeyboardElement, beforeDraw: (Keyboard) -> Unit = {}): Bitmap {
        val keyboard = buildKeyboard(element)
        beforeDraw(keyboard)
        val themedContext = ContextThemeWrapper(latinIME, KeyboardTheme.getKeyboardTheme(latinIME).mStyleId)
        val view = KeyboardView(themedContext, null)
        view.setKeyboard(keyboard)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(keyboard.mOccupiedWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(keyboard.mOccupiedHeight, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Settings.getValues().mColors.get(ColorType.MAIN_BACKGROUND))
        view.draw(canvas)
        return bitmap
    }

    private fun buildKeyboard(element: KeyboardElement): Keyboard {
        val layoutParams = KeyboardLayoutSet.Params()
        setField(layoutParams, "editorInfo", EditorInfo())
        val subtype = SubtypeUtilsAdditional.createEmojiCapableAdditionalSubtype(Locale.ENGLISH, "qwerty", true)
        setField(layoutParams, "subtype", RichInputMethodSubtype.get(subtype))
        setField(layoutParams, "keyboardWidth", 1080)
        setField(layoutParams, "keyboardHeight", 700)
        val builder = KeyboardBuilder(latinIME, KeyboardParams(UniqueKeysCache.NO_CACHE))
        builder.load(KeyboardId(element, layoutParams))
        return builder.build()
    }

    private fun setField(target: KeyboardLayoutSet.Params, name: String, value: Any) =
        KeyboardLayoutSet.Params::class.java.getDeclaredField(name).apply { isAccessible = true }.set(target, value)

    private fun assertPureBlackAndWhite(bitmap: Bitmap) {
        val pixels = bitmap.pixels()
        val offending = pixels.filter { it != Color.BLACK && it != Color.WHITE }.distinct()
        assertTrue(offending.isEmpty(), "found non black/white colors: ${offending.take(10).map { Integer.toHexString(it) }}")
        val black = pixels.count { it == Color.BLACK }
        val white = pixels.count { it == Color.WHITE }
        assertTrue(black > pixels.size / 50 && white > pixels.size / 50, "keyboard should not be blank (black=$black, white=$white)")
    }

    private fun countPixels(bitmap: Bitmap, color: Int) = bitmap.pixels().count { it == color }

    private fun Bitmap.pixels() = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
}
