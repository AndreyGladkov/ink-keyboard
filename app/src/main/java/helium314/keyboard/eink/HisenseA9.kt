// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.os.Build

object HisenseA9 {
    private const val MODEL = "HLTE556N"

    // GSI builds on the A9 report manufacturer QUALCOMM and brand Crosscall, so only model and device identify it.
    fun isCurrentDevice() = MODEL.equals(Build.MODEL, ignoreCase = true) || MODEL.equals(Build.DEVICE, ignoreCase = true)
}
