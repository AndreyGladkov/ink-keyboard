// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.spellcheck.AndroidSpellCheckerService
import helium314.keyboard.latin.spellcheck.AndroidSpellCheckerSession
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.xmlpull.v1.XmlPullParser
import kotlin.test.Test
import kotlin.test.assertEquals

@Implements(InputMethodManager::class)
class ShadowInputMethodManagerOnDummySubtype : ShadowInputMethodManager2() {
    @Implementation
    override fun getCurrentInputMethodSubtype(): InputMethodSubtype = InputMethodSubtype.InputMethodSubtypeBuilder()
        .setSubtypeLocale("en")
        .setSubtypeExtraValue("dummy")
        .setSubtypeId(0x7000000f)
        .build()
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowInputMethodManagerOnDummySubtype::class,
    ShadowDictionaryFacilitatorImpl::class,
])
class SystemSubtypeTest {
    private val checkService = Robolectric.setupService(AndroidSpellCheckerService::class.java)

    @Test fun `dummy subtype is English so that Android enables the keyboard on a fresh install`() {
        val parser = checkService.resources.getXml(R.xml.method_dummy)
        var locale: String? = null
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "subtype")
                locale = parser.getAttributeValue(ANDROID_NAMESPACE, "imeSubtypeLocale")
        }
        assertEquals("en", locale)
    }

    @Test fun `spell checker follows the selected layout, not the locale of the dummy subtype`() {
        val russian = SubtypeSettings.getResourceSubtypesForLocale("ru".constructLocale()).first()
        SubtypeSettings.addEnabledSubtype(checkService.prefs(), russian)
        SubtypeSettings.setSelectedSubtype(checkService.prefs(), russian)

        assertEquals("ru", AndroidSpellCheckerSession(checkService).locale)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
