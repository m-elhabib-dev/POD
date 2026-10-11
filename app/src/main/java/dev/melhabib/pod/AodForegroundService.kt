package dev.melhabib.pod

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder

/**
 * Keeps ScreenStateReceiver and ChargeReceiver registered and the service
 * alive in the background. Neither SCREEN_OFF nor POWER_CONNECTED reaches
 * a manifest-declared receiver on Android 8+, so this service is what
 * makes the overlay appear on its own. This does NOT keep the CPU awake
 * by itself -- it is a foreground service so Android doesn't kill it,
 * nothing more. The screen-off / plug-in -> show overlay logic lives in
 * those receivers, and the per-minute redraw lives in ClockTickReceiver,
 * all driven by broadcasts / alarms rather than a busy loop.
 *
 * This runs continuously (no time-of-day window) so it can catch
 * screen-off at any hour; whether the overlay actually shows is gated on
 * Prefs.isEnabled() (the manual toggle) and on charging state (see
 * ScreenStateReceiver / ChargeReceiver -- "working in charge only").
 */
class AodForegroundService : Service() {

    private val screenStateReceiver = ScreenStateReceiver()
    private val chargeReceiver = ChargeReceiver()
    private var receiverRegistered = false

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!receiverRegistered) {
            registerReceiver(screenStateReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
            registerReceiver(chargeReceiver, IntentFilter(Intent.ACTION_POWER_CONNECTED))
            receiverRegistered = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sticky: if the system kills us under memory pressure, come back.
        return START_STICKY
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(screenStateReceiver)
            unregisterReceiver(chargeReceiver)
            receiverRegistered = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "pod_aod_service"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            val intent = Intent(context, AodForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AodForegroundService::class.java))
        }
    }
}
