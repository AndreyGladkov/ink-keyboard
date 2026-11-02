// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Color
import android.graphics.Typeface
import helium314.keyboard.latin.common.Colors
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EinkLabelWeightTest {
    private fun eink(weight: Int) = EinkColors(
        Color.BLACK, Color.WHITE,
        EinkStyle(borderWidthPx = 2f, cornerRadiusPx = 0f, antiAlias = false, invertPressedKey = false, labelWeight = weight),
    )

    @Test fun defaultWeightKeepsTypeface() {
        assertSame(Typeface.DEFAULT, EinkRendering.withLabelWeight(Typeface.DEFAULT, eink(EinkStyle.DEFAULT_LABEL_WEIGHT)))
    }

    @Test fun otherStylesKeepTypeface() {
        assertSame(Typeface.DEFAULT, EinkRendering.withLabelWeight(Typeface.DEFAULT, Mockito.mock(Colors::class.java)))
    }

    @Test fun requestedWeightIsApplied() {
        assertEquals(600, EinkRendering.withLabelWeight(Typeface.DEFAULT, eink(600)).weight)
    }

    @Test fun boldLabelsStayAtLeastBold() {
        assertEquals(700, EinkRendering.withLabelWeight(Typeface.DEFAULT_BOLD, eink(300)).weight)
        assertEquals(900, EinkRendering.withLabelWeight(Typeface.DEFAULT_BOLD, eink(900)).weight)
    }
}
