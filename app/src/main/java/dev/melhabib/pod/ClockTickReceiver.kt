package dev.melhabib.pod

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Drives the once-a-minute redraw via AlarmManager rather than a
 * Handler/Thread loop (see aod-design.md "Update rate" / architecture
 * sketch) -- an exact alarm wakes us briefly once a minute instead of
 * keeping anything spinning continuously.
 *
 * The TextClock views in activity_aod.xml already redraw themselves on
 * the minute; this alarm's job is purely to drive the burn-in pixel
 * drift on the same cadence. AodOverlayActivity listens for ACTION_TICK
 * while visible to apply that drift.
 */
class ClockTickReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TICK) {
            // Exact alarms are one-shot; re-arm for the next minute boundary.
            schedule(context)
        }
    }

    companion object {
        const val ACTION_TICK = "dev.melhabib.pod.ACTION_TICK"
        private const val REQUEST_CODE = 100

        private fun pendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ClockTickReceiver::class.java).setAction(ACTION_TICK)
            return PendingIntent.getBroadcast(
                context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val next = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 1)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC, next.timeInMillis, pendingIntent(context)
            )
        }

        fun cancel(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(pendingIntent(context))
        }
    }
}
