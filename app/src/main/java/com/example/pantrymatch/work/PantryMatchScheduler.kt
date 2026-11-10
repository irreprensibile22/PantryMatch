package com.example.pantrymatch.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Schedules/cancels the periodic worker with WorkManager.
object PantryMatchScheduler {

    private const val WORK_NAME = "pantry_match_periodic"
    private const val INTERVAL_MINUTES = 30L

    fun schedule(context: Context) {
        // the worker makes network calls: run it only when connected
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<PantryMatchWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        // KEEP: if already scheduled, do not recreate it (avoids duplicates on every launch)
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
