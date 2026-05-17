package com.foleyit.itflow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itflow_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val SERVER_URL  = stringPreferencesKey("server_url")
        val AUTH_TOKEN  = stringPreferencesKey("auth_token")
        val USER_NAME   = stringPreferencesKey("user_name")
        val USER_ID     = intPreferencesKey("user_id")
        val USER_TYPE   = intPreferencesKey("user_type")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: "" }
    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val userName: Flow<String?> = context.dataStore.data.map { it[USER_NAME] }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun saveAuthData(token: String, user: com.foleyit.itflow.data.api.UserInfo) {
        context.dataStore.edit {
            it[AUTH_TOKEN] = token
            it[USER_NAME]  = user.name
            it[USER_ID]    = user.id
            it[USER_TYPE]  = user.type
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit {
            it.remove(AUTH_TOKEN)
            it.remove(USER_NAME)
            it.remove(USER_ID)
            it.remove(USER_TYPE)
        }
    }
}
