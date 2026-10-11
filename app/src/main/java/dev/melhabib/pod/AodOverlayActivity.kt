package dev.melhabib.pod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dev.melhabib.pod.databinding.ActivityAodBinding
import kotlin.random.Random

/**
 * The AOD face itself. Shown over the lock screen on screen-off while
 * charging (ScreenStateReceiver / ChargeReceiver). Per aod-design.md:
 *  - requires charging -- no pocket/face-down sensor suppression anymore,
 *    a phone on a charger is assumed to be sitting on a dock, not in a
 *    pocket
 *  - dismisses itself the moment charging stops (ACTION_POWER_DISCONNECTED)
 *  - brightness mirrors the device's current system brightness, no
 *    independent ambient-light sampling
 *  - small periodic drift of the whole scene (background + clock) for
 *    burn-in protection
 *  - any touch dismisses back to the real lock screen
 */
class AodOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAodBinding

    private val tickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ClockTickReceiver.ACTION_TICK) {
                applyBurnInDrift()
            }
        }
    }

    private val powerDisconnectedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_POWER_DISCONNECTED) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setShowWhenLockedCompat()
        applySystemBrightness()

        binding.root.setOnTouchListener { _, _ ->
            finish()
            true
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBrightness()
        ContextCompat.registerReceiver(
            this, tickReceiver, IntentFilter(ClockTickReceiver.ACTION_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        registerReceiver(powerDisconnectedReceiver, IntentFilter(Intent.ACTION_POWER_DISCONNECTED))
        ClockTickReceiver.schedule(this)
    }

    override fun onPause() {
        unregisterReceiver(tickReceiver)
        unregisterReceiver(powerDisconnectedReceiver)
        // The tick only exists to drift this face, so it runs exactly while
        // the overlay is resumed (no point redrawing a clock nobody can see).
        ClockTickReceiver.cancel(this)
        super.onPause()
    }

    private fun setShowWhenLockedCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Mirrors the device's current system brightness instead of reading
     * our own ambient-light sensor -- see aod-design.md "Brightness".
     */
    private fun applySystemBrightness() {
        val systemBrightness = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            128 // mid brightness fallback; SCREEN_BRIGHTNESS is unreadable on some OEM builds.
        }
        val attrs = window.attributes
        attrs.screenBrightness = (systemBrightness / 255f).coerceIn(0.01f, 1f)
        window.attributes = attrs
    }

    /**
     * Shifts the WHOLE scene (background + clock, see drift_container in
     * activity_aod.xml), not just the clock text -- the mountains
     * background is now the dominant lit area, so drifting only the
     * clock would leave most pixels static. drift_container is scaled up
     * 1.1x in the layout so this never exposes a blank edge.
     */
    private fun applyBurnInDrift() {
        val maxOffsetPx = resources.displayMetrics.density * DRIFT_MAX_DP
        val newX = (binding.driftContainer.translationX + Random.nextInt(-3, 4))
            .coerceIn(-maxOffsetPx, maxOffsetPx)
        val newY = (binding.driftContainer.translationY + Random.nextInt(-3, 4))
            .coerceIn(-maxOffsetPx, maxOffsetPx)
        binding.driftContainer.translationX = newX
        binding.driftContainer.translationY = newY
    }

    companion object {
        private const val TAG = "AodOverlayActivity"
        private const val DRIFT_MAX_DP = 10f

        fun show(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                // Without this, Android's background-activity-start restriction
                // blocks us from launching from a receiver/service (see the
                // A-vs-B decision in the project thread). MainActivity is
                // responsible for asking the user to grant it; if they haven't,
                // there's nothing to show.
                Log.w(TAG, "Skipping AOD overlay: SYSTEM_ALERT_WINDOW not granted")
                return
            }
            val intent = Intent(context, AodOverlayActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
                )
            context.startActivity(intent)
        }
    }
}
