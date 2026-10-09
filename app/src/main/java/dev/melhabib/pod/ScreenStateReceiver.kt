package dev.melhabib.pod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Screen-off -> show the AOD overlay. Gated on Prefs.isEnabled() -- the
 * manual toggle is the only on/off switch in this design, there's no
 * scheduled window.
 *
 * The per-minute tick alarm is owned by AodOverlayActivity (armed in
 * onResume, cancelled in onPause), not cancelled here on SCREEN_ON: the
 * overlay's own turnScreenOn fires SCREEN_ON right after onResume arms the
 * alarm, which used to cancel it immediately.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF && Prefs.isEnabled(context)) {
            AodOverlayActivity.show(context)
        }
    }
}
