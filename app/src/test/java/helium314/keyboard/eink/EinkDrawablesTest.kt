// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EinkDrawablesTest {
    private val style = EinkStyle(borderWidthPx = 2f, cornerRadiusPx = 0f, antiAlias = false, invertPressedKey = false)
    private val colors = EinkColors(Color.BLACK, Color.WHITE, style)
    private val pressed = intArrayOf(android.R.attr.state_pressed)
    private val released = intArrayOf()

    @Test fun keyOutlineIsBorderOnly() {
        val bitmap = render(colors.outline())
        assertEquals(Color.BLACK, bitmap.getPixel(0, 20))
        assertEquals(Color.BLACK, bitmap.getPixel(1, 20))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(2, 20))
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(20, 20))
    }

    @Test fun pressedKeyLooksUnchangedWithoutInversion() {
        val drawable = colors.outline()
        val normal = render(drawable)
        drawable.state = pressed
        assertTrue(render(drawable).sameAs(normal))
    }

    @Test fun pressedKeyIsFilledWithInversion() {
        val drawable = EinkColors(Color.BLACK, Color.WHITE, style.copy(invertPressedKey = true)).outline()
        drawable.state = pressed
        assertEquals(Color.BLACK, render(drawable).getPixel(20, 20))
    }

    @Test fun panelFillsBackgroundInsideBorder() {
        val bitmap = render(colors.panel())
        assertEquals(Color.BLACK, bitmap.getPixel(0, 20))
        assertEquals(Color.WHITE, bitmap.getPixel(20, 20))
    }

    @Test fun selectableItemShowsThickBorderOnlyWhileSelected() {
        val drawable = colors.selectable()
        assertTrue(render(drawable).pixels().all { it == Color.TRANSPARENT })
        drawable.state = pressed
        val selected = render(drawable)
        assertEquals(Color.BLACK, selected.getPixel(5, 20))
        assertEquals(Color.TRANSPARENT, selected.getPixel(6, 20))
        drawable.state = released
        assertTrue(render(drawable).pixels().all { it == Color.TRANSPARENT })
    }

    @Test fun monochromeDrawableThresholdsCoverage() {
        val faint = GradientDrawable().apply { setColor(Color.argb(0x26, 0, 0, 0)) }
        val solid = GradientDrawable().apply { setColor(Color.argb(0xC0, 0x80, 0x80, 0x80)) }
        assertTrue(render(MonochromeDrawable.wrap(faint, Color.BLACK)).pixels().all { it == Color.TRANSPARENT })
        assertTrue(render(MonochromeDrawable.wrap(solid, Color.BLACK)).pixels().all { it == Color.BLACK })
    }

    @Test fun monochromeDrawableDoesNotNest() {
        val inner = GradientDrawable()
        val wrapped = MonochromeDrawable.wrap(MonochromeDrawable.wrap(inner, Color.BLACK), Color.WHITE)
        assertEquals(inner, (wrapped as MonochromeDrawable).inner)
    }

    private fun render(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, 40, 40)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private fun Bitmap.pixels() = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
}
