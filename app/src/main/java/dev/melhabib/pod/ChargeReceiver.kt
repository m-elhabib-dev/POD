package dev.melhabib.pod

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * "Working in charge only" (Pasha's call): the AOD overlay now requires
 * the phone to be charging, replacing the old pocket/face-down sensor
 * suppression entirely -- a phone on a charger is assumed to be sitting
 * on a dock/nightstand, not in a pocket, so the sensors were redundant.
 *
 * ACTION_POWER_CONNECTED is NOT on Android 8+'s implicit-broadcast
 * exception list, so a manifest-declared receiver never fires for it.
 * Like ScreenStateReceiver, this is registered at runtime in
 * AodForegroundService.onCreate instead.
 *
 * Plugging in shows the overlay right away if the phone is locked or the
 * screen is off. Checking isInteractive alone isn't enough: most phones
 * (Xperia included) wake the screen on plug-in, so by the time this runs
 * the screen is usually already on -- but still on the lock screen. If
 * the phone is unlocked and in use, this leaves it alone;
 * ScreenStateReceiver picks it up on the next screen-off.
 */
class ChargeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return
        if (!Prefs.isEnabled(context)) return

        val powerManager = context.getSystemService(PowerManager::class.java)
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        if (!powerManager.isInteractive || keyguardManager.isKeyguardLocked) {
            AodOverlayActivity.show(context)
        }
    }
}
