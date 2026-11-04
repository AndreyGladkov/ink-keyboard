// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import helium314.keyboard.latin.utils.prefs

/** Talks to A9PatchingService on Hisense A9: fast refresh while typing and a full refresh after the keyboard hides. */
class A9PanelController(
    private val isClearEnabled: () -> Boolean,
    private val isSpeedEnabled: () -> Boolean,
    private val send: (Intent) -> Unit,
    private val handler: Handler,
) {
    private val clear = Runnable { send(intent(ACTION_FORCE_CLEAR)) }
    private var speedRequested = false

    fun onKeyboardShown() {
        handler.removeCallbacks(clear)
        if (isSpeedEnabled()) {
            send(intent(ACTION_TEMPORARY_MODE).putExtra(EXTRA_MODE, MODE_SPEED))
            speedRequested = true
        }
    }

    fun onKeyboardHidden() {
        handler.removeCallbacks(clear)
        // Restored even if the setting was turned off meanwhile, otherwise the panel would stay in fast mode.
        if (speedRequested) {
            send(intent(ACTION_RESTORE_MODE))
            speedRequested = false
        }
        if (isClearEnabled()) handler.postDelayed(clear, CLEAR_DELAY_MS)
    }

    companion object {
        const val PREF_CLEAR_ON_HIDE = "eink_a9_clear_on_hide"
        const val DEFAULT_CLEAR_ON_HIDE = false
        const val PREF_SPEED_WHILE_TYPING = "eink_a9_speed_while_typing"
        const val DEFAULT_SPEED_WHILE_TYPING = false

        // Broadcast API of A9PatchingService; it only reacts while "Allow Control via Broadcasts" is on.
        const val A9_SERVICE_PACKAGE = "com.lmqr.ha9_comp_service"
        const val ACTION_FORCE_CLEAR = "EINK_FORCE_CLEAR"
        const val ACTION_TEMPORARY_MODE = "EINK_REFRESH_TEMPORARY"
        const val ACTION_RESTORE_MODE = "EINK_REFRESH_RESTORE"
        const val EXTRA_MODE = "mode"
        const val MODE_SPEED = "SPEED"

        // The frame without the keyboard is ready within ~300 ms, but the panel keeps working through queued
        // updates for a while longer; clearing before they finish leaves the keyboard's ghost.
        // A9PatchingService waits 700 ms after closing the notification shade for the same reason.
        const val CLEAR_DELAY_MS = 800L

        fun create(context: Context) = A9PanelController(
            isClearEnabled = { context.prefs().getBoolean(PREF_CLEAR_ON_HIDE, DEFAULT_CLEAR_ON_HIDE) },
            isSpeedEnabled = { context.prefs().getBoolean(PREF_SPEED_WHILE_TYPING, DEFAULT_SPEED_WHILE_TYPING) },
            send = { context.sendBroadcast(it) },
            handler = Handler(Looper.getMainLooper()),
        )

        fun intent(action: String): Intent = Intent(action)
            .setPackage(A9_SERVICE_PACKAGE)
            .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    }
}
