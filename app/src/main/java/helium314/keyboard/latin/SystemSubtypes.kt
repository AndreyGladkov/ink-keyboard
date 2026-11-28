// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.annotation.VisibleForTesting
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings

/**
 * Shows the enabled layouts to the system as subtypes of this keyboard.
 *
 * The IME switcher button that the system draws below the keyboard switches to the next enabled
 * subtype when there are several, and opens the input method picker otherwise. method_dummy.xml
 * exposes a single subtype, so without this the button never switches the layout.
 */
object SystemSubtypes {
    private var published: List<InputMethodSubtype> = emptyList()
    // InputMethodSubtype.equals ignores the name override that custom layouts get their name from,
    // so a renamed layout would not be sent again
    private var publishedNames: List<CharSequence> = emptyList()

    /** Set when the system hides its navigation bar below the keyboard and asks for the keyboard's own switcher. */
    var customSwitcherRequested = false

    /**
     * Only a ROM that the keyboard is built into is known to draw the switcher button below it.
     * Vendor ROMs may remove the button without asking the keyboard for its own switcher, and the
     * language switch key would then be gone with nothing in its place.
     */
    private var builtIntoRom = false

    /**
     * Since Android 16 the switcher button below the keyboard cycles through the published layouts,
     * so the keyboard's own language switch key is only needed where the system asks for it.
     */
    val systemSwitchesLayouts get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
            && builtIntoRom && !customSwitcherRequested && published.size > 1
            // the system does not show its switcher on a secure lock screen
            && Settings.getValues()?.mIsLocked == false

    /** Before Android 14 an app cannot enable its own subtypes, only the user in system settings can. */
    private val isSupported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /** @param selected null when the keyboard is about to switch the layout anyway */
    fun sync(ime: InputMethodService, selected: InputMethodSubtype?) {
        // while the device is locked the settings are not readable and only a fallback layout is enabled
        if (!isSupported || Settings.getValues()?.mIsLocked != false) return
        val imm = ime.getSystemService(InputMethodManager::class.java) ?: return
        builtIntoRom = ime.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val subtypes = SubtypeSettings.getEnabledSubtypes(true).filter { it.mode == Constants.Subtype.KEYBOARD_MODE }
        publish(imm, ime.imeId(), subtypes)
        if (selected != null) select(ime, selected)
    }

    /**
     * Moves the system to [subtype] after the keyboard switched to it by itself, so that the
     * switcher button continues from the layout the user sees.
     */
    fun select(ime: InputMethodService, subtype: InputMethodSubtype) {
        val imm = ime.getSystemService(InputMethodManager::class.java) ?: return
        select(subtype, imm.currentInputMethodSubtype, ime.imeId(), ime::switchInputMethod)
    }

    fun publish(imm: InputMethodManager, imeId: String, subtypes: List<InputMethodSubtype>) {
        val names = subtypes.map { it.nameOverride }
        if (!isSupported || subtypes.isEmpty() || (subtypes == published && names == publishedNames)) return
        imm.setAdditionalInputMethodSubtypes(imeId, subtypes.toTypedArray())
        // explicitly enabling only the layouts also disables the dummy subtype, which would otherwise
        // be one step of the switcher button that does nothing
        imm.setExplicitlyEnabledInputMethodSubtypes(imeId, subtypes.map { it.hashCode() }.toIntArray())
        published = subtypes.toList()
        publishedNames = names
    }

    fun select(
        subtype: InputMethodSubtype,
        systemSubtype: InputMethodSubtype?,
        imeId: String,
        switchInputMethod: (String, InputMethodSubtype) -> Unit,
    ) {
        if (!isSupported || subtype == systemSubtype || subtype !in published) return
        switchInputMethod(imeId, subtype)
    }

    @VisibleForTesting
    internal fun reset() {
        published = emptyList()
        publishedNames = emptyList()
        customSwitcherRequested = false
        builtIntoRom = false
    }

    private fun InputMethodService.imeId() = ComponentName(this, javaClass).flattenToShortString()
}
