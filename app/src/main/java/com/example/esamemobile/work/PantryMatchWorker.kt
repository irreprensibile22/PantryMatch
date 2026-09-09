package com.example.esamemobile.work

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
import com.example.esamemobile.MainActivity
import com.example.esamemobile.R
import com.example.esamemobile.data.repository.AuthRepository
import com.example.esamemobile.data.repository.PantryRepository
import com.example.esamemobile.data.repository.RecipeRepository
import com.example.esamemobile.util.Notifications

// Lavoro periodico in background: conta le ricette preparabili con la dispensa e notifica l'utente.
class PantryMatchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // se nessuno e loggato non c'e niente da fare (ma non e un errore)
        val loggedIn = runCatching { AuthRepository().isLoggedIn }.getOrDefault(false)
        if (!loggedIn) return Result.success()

        val count = runCatching {
            val pantry = PantryRepository().getPantry()
            // maxMissing = 0: solo le ricette preparabili subito, senza ingredienti mancanti
            if (pantry.isEmpty()) 0
            else RecipeRepository().searchByPantry(pantry, maxMissing = 0).size
        }.getOrElse {
            return Result.retry() // errore di rete/Firestore: riprovo piu tardi
        }

        if (count > 0) notifyPreparable(count)
        return Result.success()
    }

    private fun notifyPreparable(count: Int) {
        Notifications.ensureChannel(applicationContext)

        // su Android 13+ senza il permesso runtime la notifica verrebbe scartata: esco prima
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
