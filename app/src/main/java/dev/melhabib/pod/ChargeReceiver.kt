package dev.melhabib.pod

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
 * ACTION_POWER_CONNECTED/DISCONNECTED (unlike SCREEN_ON/OFF) ARE
 * delivered to manifest-declared receivers, so this one is static.
 * It only has to handle the "plug in while already locked" case --
 * ScreenStateReceiver's ACTION_SCREEN_OFF handler covers "screen turns
 * off while already charging".
 */
class ChargeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_POWER_CONNECTED) return
        if (!Prefs.isEnabled(context)) return

        val powerManager = context.getSystemService(PowerManager::class.java)
        if (!powerManager.isInteractive) {
            AodOverlayActivity.show(context)
        }
    }
}
