package dev.melhabib.pod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts the foreground service after a reboot or an app update (which
 * kills it), if it was left enabled -- otherwise plugging in does nothing
 * until the app is opened by hand.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if ((action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) &&
            Prefs.isEnabled(context)
        ) {
            AodForegroundService.start(context)
        }
    }
}
