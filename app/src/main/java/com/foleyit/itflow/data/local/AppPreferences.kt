package com.foleyit.itflow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single DataStore instance keyed on applicationContext
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itflow_prefs")

class AppPreferences(context: Context) {
    // Always use applicationContext — prevents multiple DataStore instances for the same file
    private val ctx = context.applicationContext

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val USER_NAME  = stringPreferencesKey("user_name")
        val USER_ID    = intPreferencesKey("user_id")
        val USER_TYPE  = intPreferencesKey("user_type")
    }

    val serverUrl: Flow<String>  = ctx.dataStore.data.map { it[SERVER_URL] ?: "" }
    val authToken: Flow<String?> = ctx.dataStore.data.map { it[AUTH_TOKEN] }
    val userName: Flow<String?>  = ctx.dataStore.data.map { it[USER_NAME] }

    suspend fun saveServerUrl(url: String) {
        ctx.dataStore.edit { it[SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun saveAuthData(token: String, user: com.foleyit.itflow.data.api.UserInfo) {
        ctx.dataStore.edit {
            it[AUTH_TOKEN] = token
            it[USER_NAME]  = user.name
            it[USER_ID]    = user.id
            it[USER_TYPE]  = user.type
        }
    }

    suspend fun clearAuth() {
        ctx.dataStore.edit {
            it.remove(AUTH_TOKEN)
            it.remove(USER_NAME)
            it.remove(USER_ID)
            it.remove(USER_TYPE)
        }
    }
}
