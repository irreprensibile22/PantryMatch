package com.example.pantrymatch.util

import android.content.Context

// Local user preferences (SharedPreferences).
object SettingsPrefs {
    private const val PREFS = "settings"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    // notifications are enabled by default
    fun notificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
    }
}
