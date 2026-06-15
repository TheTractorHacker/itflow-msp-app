package com.foleyit.itflow.push

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Starts/stops the real-time notification stream service + its watchdog. */
object NotificationStreamController {

    fun start(context: Context) {
        ContextCompat.startForegroundService(
            context, Intent(context, NotificationStreamService::class.java)
        )
        NotificationWatchdogWorker.schedule(context)
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, NotificationStreamService::class.java))
        NotificationWatchdogWorker.cancel(context)
    }
}
