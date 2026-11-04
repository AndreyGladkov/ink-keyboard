// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.common.Colors
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class KeyboardTopLineTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val thickness = 2
    private val colors = EinkColors(
        Color.BLACK, Color.WHITE,
        EinkStyle(borderWidthPx = thickness.toFloat(), cornerRadiusPx = 0f, antiAlias = false, invertPressedKey = false),
    )

    private fun Bitmap.row(y: Int) = (0 until width).joinToString("") { if (getPixel(it, y) == Color.BLACK) "#" else "." }

    @Test fun lineCoversTheTopEdgeWithBorderThickness() {
        val bitmap = Bitmap.createBitmap(40, 6, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        TopLineDrawable(Color.BLACK, thickness).apply { setBounds(0, 0, 40, 6) }.draw(Canvas(bitmap))
        assertEquals("#".repeat(40), bitmap.row(0))
        assertEquals("#".repeat(40), bitmap.row(thickness - 1))
        assertEquals(".".repeat(40), bitmap.row(thickness))
    }

    @Test fun einkStylesGetTheLine() {
        val frame = LinearLayout(app)
        KeyboardTopLine.apply(frame, colors)
        assertIs<TopLineDrawable>(frame.foreground)
    }

    @Test fun otherStylesGetNoLine() {
        val frame = LinearLayout(app)
        KeyboardTopLine.apply(frame, Mockito.mock(Colors::class.java))
        assertNull(frame.foreground)
    }
}
