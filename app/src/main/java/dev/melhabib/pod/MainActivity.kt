package dev.melhabib.pod

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import dev.melhabib.pod.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toggle: MaterialSwitch = binding.aodToggle
        toggle.isChecked = Prefs.isEnabled(this)
        updateSummary(toggle.isChecked)

        toggle.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setEnabled(this, isChecked)
            updateSummary(isChecked)
            if (isChecked) {
                maybeRequestNotificationPermission()
                AodForegroundService.start(this)
            } else {
                AodForegroundService.stop(this)
            }
        }

        if (toggle.isChecked) {
            maybeRequestNotificationPermission()
            AodForegroundService.start(this)
        }
    }

    private fun updateSummary(enabled: Boolean) {
        binding.toggleSummary.setText(
            if (enabled) R.string.toggle_summary_on else R.string.toggle_summary_off
        )
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
