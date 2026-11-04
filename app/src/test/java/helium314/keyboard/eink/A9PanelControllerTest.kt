// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.eink

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.utils.prefs
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class A9PanelControllerTest {
    private var clearEnabled = true
    private var speedEnabled = false
    private val sent = mutableListOf<Intent>()
    private val controller = A9PanelController({ clearEnabled }, { speedEnabled }, { sent += it }, Handler(Looper.getMainLooper()))

    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    private fun actions() = sent.map { it.action }

    @Test fun clearsAfterKeyboardIsGone() {
        controller.onKeyboardHidden()
        advance(A9PanelController.CLEAR_DELAY_MS - 1)
        assertEquals(emptyList(), actions())
        advance(1)
        assertEquals(listOf(A9PanelController.ACTION_FORCE_CLEAR), actions())
    }

    @Test fun reshowingKeyboardCancelsClear() {
        controller.onKeyboardHidden()
        advance(A9PanelController.CLEAR_DELAY_MS / 2)
        controller.onKeyboardShown()
        advance(A9PanelController.CLEAR_DELAY_MS)
        assertEquals(emptyList(), actions())
    }

    @Test fun repeatedHidesClearOnce() {
        controller.onKeyboardHidden()
        controller.onKeyboardHidden()
        advance(A9PanelController.CLEAR_DELAY_MS)
        assertEquals(listOf(A9PanelController.ACTION_FORCE_CLEAR), actions())
    }

    @Test fun disabledSettingsSendNothing() {
        clearEnabled = false
        controller.onKeyboardShown()
        controller.onKeyboardHidden()
        advance(A9PanelController.CLEAR_DELAY_MS)
        assertEquals(emptyList(), actions())
    }

    @Test fun speedWhileTypingIsRestoredBeforeClear() {
        speedEnabled = true
        controller.onKeyboardShown()
        assertEquals(A9PanelController.MODE_SPEED, sent.single().getStringExtra(A9PanelController.EXTRA_MODE))
        controller.onKeyboardHidden()
        advance(A9PanelController.CLEAR_DELAY_MS)
        assertEquals(
            listOf(A9PanelController.ACTION_TEMPORARY_MODE, A9PanelController.ACTION_RESTORE_MODE, A9PanelController.ACTION_FORCE_CLEAR),
            actions(),
        )
    }

    @Test fun modeIsRestoredEvenIfSettingWasTurnedOffMeanwhile() {
        speedEnabled = true
        clearEnabled = false
        controller.onKeyboardShown()
        speedEnabled = false
        controller.onKeyboardHidden()
        assertEquals(listOf(A9PanelController.ACTION_TEMPORARY_MODE, A9PanelController.ACTION_RESTORE_MODE), actions())
    }

    @Test fun broadcastsTargetA9ServiceInForeground() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.prefs().edit {
            putBoolean(A9PanelController.PREF_SPEED_WHILE_TYPING, true)
            putBoolean(A9PanelController.PREF_CLEAR_ON_HIDE, true)
        }
        A9PanelController.create(app).apply { onKeyboardShown(); onKeyboardHidden() }
        advance(A9PanelController.CLEAR_DELAY_MS)
        val intents = shadowOf(app).broadcastIntents
        assertEquals(3, intents.size)
        intents.forEach {
            assertEquals(A9PanelController.A9_SERVICE_PACKAGE, it.`package`)
            assertEquals(Intent.FLAG_RECEIVER_FOREGROUND, it.flags and Intent.FLAG_RECEIVER_FOREGROUND)
        }
        app.prefs().edit { clear() }
    }

    @Test fun defaultsAreOff() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.prefs().edit { clear() }
        A9PanelController.create(app).apply { onKeyboardShown(); onKeyboardHidden() }
        advance(A9PanelController.CLEAR_DELAY_MS)
        assertEquals(emptyList(), shadowOf(app).broadcastIntents.map { it.action })
    }
}
