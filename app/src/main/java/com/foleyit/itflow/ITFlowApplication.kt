package com.foleyit.itflow

import android.app.Application
import com.foleyit.itflow.data.api.ApiClient
import com.foleyit.itflow.data.local.AppPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ITFlowApplication : Application() {

    // Single shared instance — use (application as ITFlowApplication).prefs everywhere
    lateinit var prefs: AppPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        runBlocking {
            val url   = prefs.serverUrl.first()
            val token = prefs.authToken.first()
            if (url.isNotBlank()) {
                ApiClient.init(url, token)
            }
        }
    }
}
