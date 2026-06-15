package com.foleyit.itflow.push

import android.content.Context
import com.foleyit.itflow.ITFlowApplication
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.api.PushEndpointRequest
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.MessagingReceiver
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage

class PushReceiver : MessagingReceiver() {

    override fun onNewEndpoint(context: Context, endpoint: PushEndpoint, instance: String) {
        val app = context.applicationContext as ITFlowApplication
        CoroutineScope(Dispatchers.IO).launch {
            val token = app.prefs.authToken.first()
            if (token.isNullOrBlank()) return@launch
            runCatching {
                ApiClient.service().registerPushEndpoint(PushEndpointRequest(endpoint.url))
            }
        }
    }

    override fun onUnregistered(context: Context, instance: String) {
        val app = context.applicationContext as ITFlowApplication
        CoroutineScope(Dispatchers.IO).launch {
            val token = app.prefs.authToken.first()
            if (token.isNullOrBlank()) return@launch
            runCatching { ApiClient.service().unregisterPushEndpoint() }
        }
    }

    override fun onRegistrationFailed(context: Context, reason: FailedReason, instance: String) {
        // Leave push disabled — user can retry from Profile
    }

    override fun onMessage(context: Context, message: PushMessage, instance: String) {
        NotificationHelper.createChannel(context)

        val raw = String(message.content, Charsets.UTF_8)
        val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return

        val title = json.get("title")?.asString ?: "ITFlow MSP"
        val body  = json.get("body")?.asString ?: ""
        val data  = json.getAsJsonObject("data")

        val deepLinkRoute = data?.let { d ->
            val ticketId = d.get("entity_id")?.asInt ?: d.get("ticket_id")?.asInt
            when {
                ticketId != null && ticketId > 0 -> "tickets/$ticketId"
                else -> null
            }
        }

        NotificationHelper.show(context, title, body, deepLinkRoute)
    }
}
