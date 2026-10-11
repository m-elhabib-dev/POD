package dev.melhabib.pod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

/**
 * Screen-off -> show the AOD overlay, but only while charging (Pasha's
 * call -- "working in charge only" replaces the old 24/7 + pocket/
 * face-down sensor suppression). Gated on Prefs.isEnabled() too -- the
 * manual toggle is still the only on/off switch, there's no scheduled
 * window.
 *
 * ChargeReceiver handles the other direction: plugging in while already
 * locked, which this receiver never sees since the screen was already off.
 *
 * The per-minute tick alarm is owned by AodOverlayActivity (armed in
 * onResume, cancelled in onPause), not cancelled here on SCREEN_ON: the
 * overlay's own turnScreenOn fires SCREEN_ON right after onResume arms the
 * alarm, which used to cancel it immediately.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF && Prefs.isEnabled(context) && isCharging(context)) {
            AodOverlayActivity.show(context)
        }
    }

    private fun isCharging(context: Context): Boolean {
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        return batteryManager.isCharging
    }
}
