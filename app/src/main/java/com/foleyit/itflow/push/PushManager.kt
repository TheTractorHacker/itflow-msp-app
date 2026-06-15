package com.foleyit.itflow.push

import android.content.Context
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Thin wrapper around the UnifiedPush connector. ITFlow MSP uses a single
 * fixed instance per device — one push endpoint registration per app install.
 */
object PushManager {
    const val INSTANCE = "itflow_msp"

    fun register(context: Context) {
        UnifiedPush.register(context, INSTANCE)
    }

    fun unregister(context: Context) {
        UnifiedPush.unregister(context, INSTANCE)
    }

    fun hasDistributor(context: Context): Boolean =
        UnifiedPush.getDistributors(context).isNotEmpty()
}
