// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.ShadowInputMethodManager2
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.createPrefKeyForBooleanSettings
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.defaultPinnedToolbarPref
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.writeCustomKeyCodes
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.EnumMap
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowInputMethodManager2::class])
class AppUpgradeTest {
    private val latinIME = Robolectric.setupService(LatinIME::class.java)
    private val prefs = latinIME.prefs()
    private val portraitKeyGap = createPrefKeyForBooleanSettings(Settings.PREF_KEY_GAP_SCALE_PREFIX, 0, 2)

    @Test fun settingsWithoutHeliBoardVersionTakeItFromVersionCode() {
        assertEquals(0, heliBoardVersionWithStoredVersionCode(null))
        assertEquals(4201, heliBoardVersionWithStoredVersionCode(1))
        assertEquals(4201, heliBoardVersionWithStoredVersionCode(999))
        assertEquals(1000, heliBoardVersionWithStoredVersionCode(1000))
        assertEquals(3901, heliBoardVersionWithStoredVersionCode(3901))
    }

    @Test fun storedHeliBoardVersionWinsOverVersionCode() {
        prefs.edit().clear().putInt(Settings.PREF_VERSION_CODE, 4205).putInt(PREF_HELIBOARD_VERSION_CODE, 3901).commit()

        assertEquals(3901, settingsHeliBoardVersion(prefs))
    }

    @Test fun freshInstallRecordsBothVersions() {
        prefs.edit().clear().commit()

        checkVersionUpgrade(latinIME)

        assertEquals(BuildConfig.VERSION_CODE, prefs.getInt(Settings.PREF_VERSION_CODE, 0))
        assertEquals(BuildConfig.HELIBOARD_VERSION_CODE, prefs.getInt(PREF_HELIBOARD_VERSION_CODE, 0))
    }

    @Test fun upgradeFromFirstInkReleaseKeepsSettings() {
        customizeSettings()
        val before = snapshot()
        prefs.edit().putInt(Settings.PREF_VERSION_CODE, 1).remove(PREF_HELIBOARD_VERSION_CODE).commit()

        checkVersionUpgrade(latinIME)

        assertEquals(before, snapshot())
        assertEquals(BuildConfig.VERSION_CODE, prefs.getInt(Settings.PREF_VERSION_CODE, 0))
        assertEquals(BuildConfig.HELIBOARD_VERSION_CODE, prefs.getInt(PREF_HELIBOARD_VERSION_CODE, 0))
    }

    @Test fun inkOnlyReleaseKeepsSettings() {
        customizeSettings()
        val before = snapshot()
        prefs.edit()
            .putInt(Settings.PREF_VERSION_CODE, BuildConfig.VERSION_CODE - 1)
            .putInt(PREF_HELIBOARD_VERSION_CODE, BuildConfig.HELIBOARD_VERSION_CODE)
            .commit()

        checkVersionUpgrade(latinIME)

        assertEquals(before, snapshot())
    }

    @Test fun mergedHeliBoardMigrationsRunWithoutVersionCodeChange() {
        prefs.edit()
            .putInt(Settings.PREF_VERSION_CODE, BuildConfig.VERSION_CODE)
            .putInt(PREF_HELIBOARD_VERSION_CODE, 3901)
            .commit()

        checkVersionUpgrade(latinIME)

        assertEquals(1.75f, prefs.getFloat(portraitKeyGap, 0f))
        assertEquals(BuildConfig.HELIBOARD_VERSION_CODE, prefs.getInt(PREF_HELIBOARD_VERSION_CODE, 0))
    }

    @Test fun backupFromOldHeliBoardStillMigrates() {
        prefs.edit().clear().putInt(Settings.PREF_VERSION_CODE, 3901).commit()

        checkVersionUpgrade(latinIME)

        assertEquals(1.75f, prefs.getFloat(portraitKeyGap, 0f))
    }

    private fun heliBoardVersionWithStoredVersionCode(versionCode: Int?): Int {
        prefs.edit().clear().apply { if (versionCode != null) putInt(Settings.PREF_VERSION_CODE, versionCode) }.commit()
        return settingsHeliBoardVersion(prefs)
    }

    private fun customizeSettings() {
        val editedRussian = "ru§KeyboardLayoutSet=MAIN:russian|SYMBOLS:symbols_arabic,SupportTouchPositionCorrection"
        prefs.edit()
            .putString(Settings.PREF_ENABLED_SUBTYPES, "en-US§SupportTouchPositionCorrection,TrySuppressingImeSwitcher;$editedRussian")
            .putString(Settings.PREF_ADDITIONAL_SUBTYPES, editedRussian)
            .putString(Settings.PREF_PINNED_TOOLBAR_KEYS, defaultPinnedToolbarPref.replace("CLIPBOARD:false", "CLIPBOARD:true"))
            .putFloat(portraitKeyGap, 1.0f)
            .putBoolean(Settings.PREF_SUGGEST_PUNCTUATION, true)
            .putBoolean(Settings.PREF_BIGRAM_PREDICTIONS, true)
            .putBoolean(Settings.PREF_SHOW_LANGUAGE_SWITCH_KEY, false)
            .putString(Settings.PREF_LANGUAGE_SWITCH_KEY, "both")
            .commit()
        writeCustomKeyCodes(prefs, EnumMap<ToolbarKey, Pair<Int?, Int?>>(ToolbarKey::class.java).apply { put(ToolbarKey.UNDO, 65 to 66) })
    }

    private fun snapshot() = prefs.all
        .filterKeys { it != Settings.PREF_VERSION_CODE && it != PREF_HELIBOARD_VERSION_CODE }
        .mapValues { it.value.toString() }
}
