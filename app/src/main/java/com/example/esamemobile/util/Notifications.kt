package com.example.esamemobile.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.esamemobile.R

/**
 * Centralizza canale e identificatori delle notifiche locali dell'app.
 * Il canale è richiesto da Android 8.0 (API 26) in su.
 */
object Notifications {

    const val CHANNEL_PANTRY = "pantry_match"
    const val NOTIF_ID_PANTRY = 1001

    /** Crea (idempotente) il canale di notifica usato dal task in background. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_PANTRY,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_desc) }

            context.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}
