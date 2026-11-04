// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class A9SettingVisibilityTest {
    private val prefs = ApplicationProvider.getApplicationContext<Application>().prefs()

    private val a9Settings = listOf(A9PanelController.PREF_SPEED_WHILE_TYPING, A9PanelController.PREF_CLEAR_ON_HIDE)

    private fun shownA9Settings() = einkAppearanceItems(prefs).filter { it in a9Settings }

    @Test fun shownOnHisenseA9() {
        ShadowBuild.setModel("HLTE556N")
        ShadowBuild.setDevice("HLTE556N")
        assertEquals(a9Settings, shownA9Settings())
    }

    @Test fun shownWhenOnlyDeviceMatches() {
        ShadowBuild.setModel("TrebleDroid")
        ShadowBuild.setDevice("hlte556n")
        assertEquals(a9Settings, shownA9Settings())
    }

    @Test fun hiddenOnOtherDevices() {
        ShadowBuild.setModel("Pixel 8")
        ShadowBuild.setDevice("shiba")
        assertEquals(emptyList(), shownA9Settings())
    }
}
