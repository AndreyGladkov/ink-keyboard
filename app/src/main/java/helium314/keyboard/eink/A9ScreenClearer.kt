// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import helium314.keyboard.latin.utils.prefs

class A9ScreenClearer(
    private val isEnabled: () -> Boolean,
    private val sendClear: () -> Unit,
    private val handler: Handler,
) {
    private val clear = Runnable { sendClear() }

    fun onKeyboardShown() = handler.removeCallbacks(clear)

    fun onKeyboardHidden() {
        handler.removeCallbacks(clear)
        if (isEnabled()) handler.postDelayed(clear, CLEAR_DELAY_MS)
    }

    companion object {
        const val PREF_CLEAR_ON_HIDE = "eink_a9_clear_on_hide"
        const val DEFAULT_CLEAR_ON_HIDE = false

        // Broadcast API of A9PatchingService; it only reacts while "Allow Control via Broadcasts" is on.
        const val A9_SERVICE_PACKAGE = "com.lmqr.ha9_comp_service"
        const val ACTION_FORCE_CLEAR = "EINK_FORCE_CLEAR"

        // The frame without the keyboard is ready within ~300 ms, but the panel keeps working through queued
        // updates for a while longer; clearing before they finish leaves the keyboard's ghost.
        // A9PatchingService waits 700 ms after closing the notification shade for the same reason.
        const val CLEAR_DELAY_MS = 800L

        fun create(context: Context) = A9ScreenClearer(
            isEnabled = { context.prefs().getBoolean(PREF_CLEAR_ON_HIDE, DEFAULT_CLEAR_ON_HIDE) },
            sendClear = { context.sendBroadcast(clearIntent()) },
            handler = Handler(Looper.getMainLooper()),
        )

        fun clearIntent(): Intent = Intent(ACTION_FORCE_CLEAR)
            .setPackage(A9_SERVICE_PACKAGE)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    }
}
