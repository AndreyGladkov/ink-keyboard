// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class A9SettingVisibilityTest {
    private val prefs = ApplicationProvider.getApplicationContext<Application>().prefs()

    private fun showsClearSetting() = A9ScreenClearer.PREF_CLEAR_ON_HIDE in einkAppearanceItems(prefs)

    @Test fun shownOnHisenseA9() {
        ShadowBuild.setModel("HLTE556N")
        ShadowBuild.setDevice("HLTE556N")
        assertTrue(showsClearSetting())
    }

    @Test fun shownWhenOnlyDeviceMatches() {
        ShadowBuild.setModel("TrebleDroid")
        ShadowBuild.setDevice("hlte556n")
        assertTrue(showsClearSetting())
    }

    @Test fun hiddenOnOtherDevices() {
        ShadowBuild.setModel("Pixel 8")
        ShadowBuild.setDevice("shiba")
        assertFalse(showsClearSetting())
    }
}
