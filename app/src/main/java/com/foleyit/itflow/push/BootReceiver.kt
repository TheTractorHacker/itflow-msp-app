package com.foleyit.itflow.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foleyit.itflow.ITFlowApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Restarts the real-time notification stream service after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = (context.applicationContext as ITFlowApplication).prefs
        val (enabled, token) = runBlocking {
            Pair(prefs.realtimeNotificationsEnabled.first(), prefs.authToken.first())
        }

        if (enabled && token != null) {
            NotificationStreamController.start(context.applicationContext)
        }
    }
}
