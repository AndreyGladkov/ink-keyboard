// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.content.pm.ApplicationInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.SystemSubtypes
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import helium314.keyboard.latin.settings.Settings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowInputMethodManager2::class,
    ShadowLocaleManagerCompat::class,
])
class SystemSubtypesTest {
    private lateinit var latinIME: LatinIME
    private val russian get() = SubtypeSettings.getResourceSubtypesForLocale("ru".constructLocale()).first()
    private val english get() = SubtypeSettings.getResourceSubtypesForLocale("en-US".constructLocale()).first()

    @BeforeTest fun setUp() {
        SystemSubtypes.reset()
        ShadowInputMethodManager2.additionalSubtypes = emptyList()
        ShadowInputMethodManager2.explicitlyEnabledSubtypes = emptyList()
        ShadowInputMethodManager2.explicitlyEnabledCalls = 0
        latinIME = Robolectric.setupService(LatinIME::class.java)
    }

    @AfterTest fun tearDown() {
        SystemSubtypes.reset()
    }

    @Test fun `enabled layouts become the only enabled system subtypes`() {
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), russian)

        SystemSubtypes.sync(latinIME, english)

        val enabled = SubtypeSettings.getEnabledSubtypes(true)
        assertTrue(enabled.containsAll(listOf(english, russian)))
        assertEquals(enabled, ShadowInputMethodManager2.additionalSubtypes)
        assertEquals(enabled.map { it.hashCode() }, ShadowInputMethodManager2.explicitlyEnabledSubtypes)
        assertTrue(0x7000000f !in ShadowInputMethodManager2.explicitlyEnabledSubtypes)
    }

    @Test fun `unchanged layouts are not sent to the system again`() {
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), russian)
        SystemSubtypes.sync(latinIME, russian)
        val calls = ShadowInputMethodManager2.explicitlyEnabledCalls

        SystemSubtypes.sync(latinIME, russian)

        assertEquals(calls, ShadowInputMethodManager2.explicitlyEnabledCalls)
    }

    @Test fun `switching the layout in the keyboard moves the system to it`() {
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), russian)
        SystemSubtypes.sync(latinIME, english)
        val switched = mutableListOf<InputMethodSubtype>()

        SystemSubtypes.select(russian, english, "ime") { _, subtype -> switched.add(subtype) }
        SystemSubtypes.select(russian, russian, "ime") { _, subtype -> switched.add(subtype) }

        assertEquals(listOf(russian), switched)
    }

    @Test fun `layouts the system does not know are not selected`() {
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        SystemSubtypes.sync(latinIME, english)
        val notEnabled = SubtypeSettings.getResourceSubtypesForLocale("de".constructLocale()).first()
        val switched = mutableListOf<InputMethodSubtype>()

        SystemSubtypes.select(notEnabled, english, "ime") { _, subtype -> switched.add(subtype) }

        assertEquals(emptyList(), switched)
    }

    @Test fun `own language switch key gives way to the switcher button below the keyboard`() {
        latinIME.applicationInfo.flags = latinIME.applicationInfo.flags or ApplicationInfo.FLAG_SYSTEM
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), russian)
        SystemSubtypes.sync(latinIME, english)

        assertFalse(Settings.getValues().isLanguageSwitchKeyEnabled)
    }

    @Test fun `own language switch key stays when the keyboard is installed as an app`() {
        latinIME.applicationInfo.flags = latinIME.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM.inv()
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), russian)
        SystemSubtypes.sync(latinIME, english)

        assertTrue(Settings.getValues().isLanguageSwitchKeyEnabled)
    }

    @Test fun `own language switch key comes back when the system asks for it`() {
        latinIME.applicationInfo.flags = latinIME.applicationInfo.flags or ApplicationInfo.FLAG_SYSTEM
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), russian)
        SystemSubtypes.sync(latinIME, english)

        latinIME.onCustomImeSwitcherButtonRequestedVisible(true)

        assertTrue(Settings.getValues().isLanguageSwitchKeyEnabled)
    }

    @Test fun `the system cannot switch to a layout that is not enabled`() {
        SubtypeSettings.addEnabledSubtype(latinIME.prefs(), english)
        latinIME.switchToSubtype(english)
        val notEnabled = SubtypeSettings.getResourceSubtypesForLocale("de".constructLocale()).first()

        latinIME.onCurrentInputMethodSubtypeChanged(notEnabled)

        assertEquals(english, RichInputMethodManager.getInstance().currentSubtype.rawSubtype)
    }

    @Test fun `a renamed custom layout is sent to the system again`() {
        val imm = latinIME.getSystemService(InputMethodManager::class.java)
        fun custom(name: String) = InputMethodSubtype.InputMethodSubtypeBuilder()
            .setSubtypeLocale("en_US").setSubtypeMode("keyboard").setSubtypeExtraValue("KeyboardLayoutSet=custom")
            .setSubtypeNameOverride(name).build()
        SystemSubtypes.publish(imm, "ime", listOf(custom("Mine"), english))
        val calls = ShadowInputMethodManager2.explicitlyEnabledCalls

        SystemSubtypes.publish(imm, "ime", listOf(custom("Work"), english))

        assertEquals(calls + 1, ShadowInputMethodManager2.explicitlyEnabledCalls)
        assertEquals("Work", ShadowInputMethodManager2.additionalSubtypes.first().nameOverride)
    }
}
