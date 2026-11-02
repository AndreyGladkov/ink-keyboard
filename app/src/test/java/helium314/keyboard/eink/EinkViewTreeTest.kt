// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.graphics.Color
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.common.Colors
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EinkViewTreeTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val eink = EinkColors(Color.BLACK, Color.WHITE, EinkStyle(2f, 0f, antiAlias = false, invertPressedKey = false))

    private class Tree(context: android.content.Context) {
        val text = TextView(context)
        val scroll = HorizontalScrollView(context).apply { addView(LinearLayout(context).apply { addView(text) }) }
        val recycler = RecyclerView(context)
        val root = LinearLayout(context).apply { addView(scroll); addView(recycler) }
    }

    @Test fun einkDisablesAntialiasAndScrollDecorationsInWholeTree() {
        val tree = Tree(context)
        EinkRendering.applyToViewTree(tree.root, eink)
        assertFalse(tree.text.paint.isAntiAlias)
        assertFalse(tree.scroll.isHorizontalScrollBarEnabled)
        assertEquals(View.OVER_SCROLL_NEVER, tree.scroll.overScrollMode)
        assertNull(tree.recycler.itemAnimator)
    }

    @Test fun otherStylesAreLeftAlone() {
        val tree = Tree(context)
        EinkRendering.applyToViewTree(tree.root, Mockito.mock(Colors::class.java))
        assertTrue(tree.text.paint.isAntiAlias)
        assertTrue(tree.recycler.itemAnimator != null)
    }
}
