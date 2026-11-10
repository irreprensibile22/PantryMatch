package com.example.pantrymatch.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.pantrymatch.R

object Notifications {

    const val CHANNEL_PANTRY = "pantry_match"
    const val NOTIF_ID_PANTRY = 1001

    // the channel must be created before notifying; required since Android 8, no-op below
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
