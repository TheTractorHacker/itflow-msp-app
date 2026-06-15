package com.foleyit.itflow.push

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foleyit.itflow.ITFlowApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodically ensures the real-time notification stream foreground service
 * is running while the user has it enabled — guards against the OS killing
 * the service or the device rebooting without the boot receiver firing.
 */
class NotificationWatchdogWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = (applicationContext as ITFlowApplication).prefs
        val enabled = prefs.realtimeNotificationsEnabled.first()
        val token = prefs.authToken.first()

        if (enabled && token != null) {
            val intent = Intent(applicationContext, NotificationStreamService::class.java)
            ContextCompat.startForegroundService(applicationContext, intent)
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "notification_stream_watchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
