package com.example.esamemobile.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Pianifica/annulla il worker periodico con WorkManager.
object PantryMatchScheduler {

    private const val WORK_NAME = "pantry_match_periodic"
    private const val INTERVAL_MINUTES = 30L

    fun schedule(context: Context) {
        // il worker fa chiamate di rete: lo eseguo solo quando c'e connessione
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<PantryMatchWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // KEEP: se e gia pianificato non lo ricreo (evito doppioni ad ogni avvio)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
