// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import helium314.keyboard.latin.R
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.preferences.Preference

const val EINK_ATTRIBUTIONS = "eink_attributions"

fun createEinkAboutSettings(context: Context) = listOf(
    Setting(context, EINK_ATTRIBUTIONS, R.string.eink_attributions, R.string.eink_attributions_summary) {
        Preference(name = it.title, description = it.description, onClick = {})
    },
)
