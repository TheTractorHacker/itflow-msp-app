package com.foleyit.itflow.push

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.foleyit.itflow.ITFlowApplication
import com.foleyit.itflow.MainActivity
import com.foleyit.itflow.R
import com.foleyit.itflow.data.ssl.FingerprintTrustManager
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

/**
 * Persistent foreground service that holds an SSE connection to
 * /api/v1/notifications/stream and shows a system notification for each
 * event received. Reconnects with backoff on failure/close.
 */
class NotificationStreamService : Service() {

    private var eventSource: EventSource? = null
    private val handler = Handler(Looper.getMainLooper())
    private var reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
    private var stopped = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopped = false
        androidx.core.app.ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildForegroundNotification(), foregroundServiceType()
        )
        connect()
        return START_STICKY
    }

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        eventSource?.cancel()
        eventSource = null
        super.onDestroy()
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("ITFlow")
            .setContentText("Listening for real-time updates")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun connect() {
        if (stopped) return

        val prefs = (application as ITFlowApplication).prefs
        val (serverUrl, token, certSha) = runBlocking {
            Triple(prefs.serverUrl.first(), prefs.authToken.first(), prefs.trustedCertSha.first())
        }

        if (serverUrl.isBlank() || token == null) {
            stopSelf()
            return
        }

        val trustManager = FingerprintTrustManager(certSha)
        val sslContext = SSLContext.getInstance("TLS").also {
            it.init(null, arrayOf(trustManager), null)
        }

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("$serverUrl/api/v1/notifications/stream")
            .header("Authorization", "Bearer $token")
            .build()

        eventSource = EventSources.createFactory(client).newEventSource(request, listener)
    }

    private fun scheduleReconnect() {
        if (stopped) return
        handler.postDelayed({
            connect()
        }, reconnectDelayMs)
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
    }

    private val listener = object : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) {
            reconnectDelayMs = INITIAL_RECONNECT_DELAY_MS
        }

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            if (type != "notification") return
            try {
                val json = JsonParser.parseString(data).asJsonObject
                // Server sets push=false for low-priority background events (e.g. ticket edits).
                // Default true so older server versions still show notifications.
                val push = json.get("push")?.asBoolean ?: true
                if (!push) return
                val title = json.get("title")?.asString ?: "ITFlow"
                val body = json.get("body")?.asString ?: ""
                val action = json.get("action")?.asString
                NotificationHelper.show(this@NotificationStreamService, title, body, action)
            } catch (_: Exception) {
                // Ignore malformed events
            }
        }

        override fun onClosed(eventSource: EventSource) {
            scheduleReconnect()
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            scheduleReconnect()
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val INITIAL_RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 60_000L

        fun foregroundServiceType(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
    }
}
