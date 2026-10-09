package dev.melhabib.pod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Screen-off -> show the AOD overlay. Screen-on / user-present -> dismiss
 * it and cancel the per-minute tick alarm (no point redrawing a clock
 * nobody can see). Gated on Prefs.isEnabled() -- the manual toggle is the
 * only on/off switch in this design, there's no scheduled window.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                if (Prefs.isEnabled(context)) {
                    AodOverlayActivity.show(context)
                }
            }
            Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                ClockTickReceiver.cancel(context)
            }
        }
    }
}
