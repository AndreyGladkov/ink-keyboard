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
class A9ScreenClearerTest {
    private var enabled = true
    private var clears = 0
    private val clearer = A9ScreenClearer({ enabled }, { clears++ }, Handler(Looper.getMainLooper()))

    private fun advance(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))

    @Test fun clearsAfterKeyboardIsGone() {
        clearer.onKeyboardHidden()
        advance(A9ScreenClearer.CLEAR_DELAY_MS - 1)
        assertEquals(0, clears)
        advance(1)
        assertEquals(1, clears)
    }

    @Test fun reshowingKeyboardCancelsClear() {
        clearer.onKeyboardHidden()
        advance(A9ScreenClearer.CLEAR_DELAY_MS / 2)
        clearer.onKeyboardShown()
        advance(A9ScreenClearer.CLEAR_DELAY_MS)
        assertEquals(0, clears)
    }

    @Test fun repeatedHidesClearOnce() {
        clearer.onKeyboardHidden()
        clearer.onKeyboardHidden()
        advance(A9ScreenClearer.CLEAR_DELAY_MS)
        assertEquals(1, clears)
    }

    @Test fun disabledSettingSendsNothing() {
        enabled = false
        clearer.onKeyboardHidden()
        advance(A9ScreenClearer.CLEAR_DELAY_MS)
        assertEquals(0, clears)
    }

    @Test fun broadcastTargetsA9Service() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.prefs().edit { putBoolean(A9ScreenClearer.PREF_CLEAR_ON_HIDE, true) }
        A9ScreenClearer.create(app).onKeyboardHidden()
        advance(A9ScreenClearer.CLEAR_DELAY_MS)
        val intent = shadowOf(app).broadcastIntents.single()
        assertEquals(A9ScreenClearer.ACTION_FORCE_CLEAR, intent.action)
        assertEquals(A9ScreenClearer.A9_SERVICE_PACKAGE, intent.`package`)
        assertEquals(Intent.FLAG_RECEIVER_FOREGROUND, intent.flags and Intent.FLAG_RECEIVER_FOREGROUND)
    }

    @Test fun defaultIsOff() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.prefs().edit { remove(A9ScreenClearer.PREF_CLEAR_ON_HIDE) }
        A9ScreenClearer.create(app).onKeyboardHidden()
        advance(A9ScreenClearer.CLEAR_DELAY_MS)
        assertEquals(emptyList(), shadowOf(app).broadcastIntents.map { it.action })
    }
}
