package com.example.esamemobile.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Pianifica (e annulla) il task periodico [PantryMatchWorker] tramite WorkManager.
 */
object PantryMatchScheduler {

    private const val WORK_NAME = "pantry_match_periodic"
    private const val INTERVAL_HOURS = 12L

    /**
     * Registra il controllo periodico. Usa una "unique work" con policy KEEP così
     * da non duplicare il lavoro a ogni avvio dell'app: se è già pianificato non
     * viene riprogrammato.
     */
    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<PantryMatchWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

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
