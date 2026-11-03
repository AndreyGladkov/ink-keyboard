// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import helium314.keyboard.latin.common.Colors
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EinkKeyDrawingTest {
    private fun eink(antiAlias: Boolean = false) = EinkColors(
        Color.BLACK, Color.WHITE,
        EinkStyle(borderWidthPx = 2f, cornerRadiusPx = 0f, antiAlias = antiAlias, invertPressedKey = false, labelWeight = 500),
    )

    private fun labelPaint(size: Float = 40f) = Paint().apply {
        typeface = Typeface.DEFAULT
        textSize = size
    }

    @Test fun stemsAreFittedForAliasedEinkLabels() {
        val paint = labelPaint()
        EinkRendering.fitStems(paint, eink())
        assertNotSame(Typeface.DEFAULT, paint.typeface)
    }

    @Test fun antiAliasedLabelsKeepTypeface() {
        val paint = labelPaint()
        EinkRendering.fitStems(paint, eink(antiAlias = true))
        assertSame(Typeface.DEFAULT, paint.typeface)
    }

    @Test fun otherStylesKeepTypeface() {
        val paint = labelPaint()
        EinkRendering.fitStems(paint, Mockito.mock(Colors::class.java))
        assertSame(Typeface.DEFAULT, paint.typeface)
    }

    @Test fun fittedTypefaceIsReusedForTheSameSize() {
        val first = labelPaint().also { EinkRendering.fitStems(it, eink()) }
        val second = labelPaint().also { EinkRendering.fitStems(it, eink()) }
        assertSame(first.typeface, second.typeface)
    }

    @Test fun onlyEinkRedrawsChangedKeysAlone() {
        assertTrue(EinkRendering.redrawsOnlyChangedKeys(eink()))
        assertFalse(EinkRendering.redrawsOnlyChangedKeys(Mockito.mock(Colors::class.java)))
    }
}
