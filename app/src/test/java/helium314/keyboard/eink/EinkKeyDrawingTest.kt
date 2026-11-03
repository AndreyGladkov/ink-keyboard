// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Color
import helium314.keyboard.latin.common.Colors
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EinkKeyDrawingTest {
    private fun eink(antiAlias: Boolean = false) = EinkColors(
        Color.BLACK, Color.WHITE,
        EinkStyle(borderWidthPx = 2f, cornerRadiusPx = 0f, antiAlias = antiAlias, invertPressedKey = false, labelWeight = 500),
    )

    @Test fun onlyEinkRedrawsChangedKeysAlone() {
        assertTrue(EinkRendering.redrawsOnlyChangedKeys(eink()))
        assertFalse(EinkRendering.redrawsOnlyChangedKeys(Mockito.mock(Colors::class.java)))
    }
}
