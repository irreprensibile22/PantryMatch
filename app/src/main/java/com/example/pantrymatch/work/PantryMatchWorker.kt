package com.example.pantrymatch.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantrymatch.MainActivity
import com.example.pantrymatch.R
import com.example.pantrymatch.data.repository.AuthRepository
import com.example.pantrymatch.data.repository.PantryRepository
import com.example.pantrymatch.data.repository.RecipeRepository
import com.example.pantrymatch.util.Notifications

// Periodic background work: counts recipes cookable with the pantry and notifies the user.
class PantryMatchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // if nobody is signed in there is nothing to do (but it is not an error)
        val loggedIn = runCatching { AuthRepository().isLoggedIn }.getOrDefault(false)
        if (!loggedIn) return Result.success()

        val count = runCatching {
            val pantry = PantryRepository().getPantry()
            // maxMissing = 0: only recipes ready to cook, with no missing ingredients
            if (pantry.isEmpty()) 0
            else RecipeRepository().searchByPantry(pantry, maxMissing = 0).size
        }.getOrElse {
            return Result.retry() // network/Firestore error: retry later
        }

        if (count > 0) notifyPreparable(count)
        return Result.success()
    }

    private fun notifyPreparable(count: Int) {
        Notifications.ensureChannel(applicationContext)

        // on Android 13+ without the runtime permission the notification would be dropped: exit early
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openApp = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val text = applicationContext.resources
            .getQuantityString(R.plurals.notif_pantry_text, count, count)

        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_PANTRY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.notif_pantry_title))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(Notifications.NOTIF_ID_PANTRY, notification)
    }
}
