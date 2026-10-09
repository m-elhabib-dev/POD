package dev.melhabib.pod

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import dev.melhabib.pod.databinding.ActivityAodBinding
import kotlin.random.Random

/**
 * The AOD face itself. Shown over the lock screen on screen-off
 * (ScreenStateReceiver). Per aod-design.md:
 *  - suppressed (hidden) while in a pocket (proximity) or face-down
 *    (accelerometer), resumes when clear -- continuous, not one-shot
 *  - brightness mirrors the device's current system brightness, no
 *    independent ambient-light sampling
 *  - small periodic pixel drift for burn-in protection
 *  - any touch dismisses back to the real lock screen
 */
class AodOverlayActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityAodBinding
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null

    private var suppressedByProximity = false
    private var suppressedByOrientation = false

    private val tickReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ClockTickReceiver.ACTION_TICK) {
                applyBurnInDrift()
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

        sensorManager = getSystemService(SensorManager::class.java)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        applySystemBrightness()
        proximitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        ContextCompat.registerReceiver(
            this, tickReceiver, IntentFilter(ClockTickReceiver.ACTION_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ClockTickReceiver.schedule(this)
    }

    override fun onPause() {
        sensorManager.unregisterListener(this)
        unregisterReceiver(tickReceiver)
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

    private fun applyBurnInDrift() {
        // Small bounded random walk, well within the bezel margin, so the
        // shift is invisible in normal use but moves the lit pixels
        // enough over hours to avoid fixed burn-in. Re-centers via the
        // coerceIn bound rather than drifting unbounded.
        val maxOffsetPx = resources.displayMetrics.density * DRIFT_MAX_DP
        val newX = (binding.driftContainer.translationX + Random.nextInt(-3, 4))
            .coerceIn(-maxOffsetPx, maxOffsetPx)
        val newY = (binding.driftContainer.translationY + Random.nextInt(-3, 4))
            .coerceIn(-maxOffsetPx, maxOffsetPx)
        binding.driftContainer.translationX = newX
        binding.driftContainer.translationY = newY
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                // values[0] is distance in cm; binary sensors report
                // maximumRange for "far" and something lower (usually 0)
                // for "near" (in a pocket/bag).
                suppressedByProximity = event.values[0] < event.sensor.maximumRange
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // Face-down: z-axis gravity component points away from the
                // screen, i.e. strongly negative.
                suppressedByOrientation = event.values[2] < -7f
            }
        }
        binding.root.visibility =
            if (suppressedByProximity || suppressedByOrientation) {
                android.view.View.INVISIBLE
            } else {
                android.view.View.VISIBLE
            }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

    companion object {
        private const val DRIFT_MAX_DP = 10f

        fun show(context: Context) {
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
