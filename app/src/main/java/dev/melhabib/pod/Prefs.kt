package dev.melhabib.pod

import android.content.Context

/**
 * Single source of truth for the one user-facing setting this app has: the
 * manual on/off toggle (see aod-design.md "Manual control"). Pasha turns
 * this off by hand instead of relying on a scheduled window -- there is no
 * time-of-day logic anywhere in this app.
 */
object Prefs {
    private const val FILE = "pod_prefs"
    private const val KEY_ENABLED = "aod_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
