package com.foleyit.itflow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.foleyit.itflow.data.api.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itflow_prefs")

class AppPreferences(context: Context) {
    private val ctx = context.applicationContext

    companion object {
        val SERVER_URL       = stringPreferencesKey("server_url")
        val USER_NAME        = stringPreferencesKey("user_name")
        val USER_ID          = intPreferencesKey("user_id")
        val USER_TYPE        = intPreferencesKey("user_type")
        val TRUSTED_CERT_SHA      = stringPreferencesKey("trusted_cert_sha")
        val BIOMETRIC_LOCK        = booleanPreferencesKey("biometric_lock")
        val REALTIME_NOTIFICATIONS_ENABLED = booleanPreferencesKey("realtime_notifications_enabled")
    }

    // AES256-GCM master key stored in Android Keystore (hardware-backed on Android 9+)
    private val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Auth token encrypted with AES256-GCM; key names encrypted with AES256-SIV
    private val securePrefs = EncryptedSharedPreferences.create(
        ctx,
        "itflow_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Non-sensitive: server URL, display name, trusted cert fingerprint — plain DataStore
    val serverUrl: Flow<String>   = ctx.dataStore.data.map { it[SERVER_URL] ?: "" }
    val userName: Flow<String?>   = ctx.dataStore.data.map { it[USER_NAME] }
    val trustedCertSha: Flow<String?>  = ctx.dataStore.data.map { it[TRUSTED_CERT_SHA] }
    val biometricLock: Flow<Boolean>   = ctx.dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }
    val realtimeNotificationsEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[REALTIME_NOTIFICATIONS_ENABLED] ?: false }

    // Auth token from encrypted storage — one-shot Flow (callers use .first())
    val authToken: Flow<String?> = flow {
        emit(securePrefs.getString("auth_token", null))
    }

    suspend fun saveServerUrl(url: String) {
        ctx.dataStore.edit { it[SERVER_URL] = url.trimEnd('/') }
    }

    suspend fun saveAuthData(token: String, user: UserInfo) {
        securePrefs.edit().putString("auth_token", token).apply()
        ctx.dataStore.edit {
            it[USER_NAME] = user.name
            it[USER_ID]   = user.id
            it[USER_TYPE] = user.type
        }
    }

    suspend fun clearAuth() {
        securePrefs.edit().remove("auth_token").apply()
        ctx.dataStore.edit {
            it.remove(USER_NAME)
            it.remove(USER_ID)
            it.remove(USER_TYPE)
        }
    }

    suspend fun saveTrustedCert(sha256: String) {
        ctx.dataStore.edit { it[TRUSTED_CERT_SHA] = sha256 }
    }

    suspend fun clearTrustedCert() {
        ctx.dataStore.edit { it.remove(TRUSTED_CERT_SHA) }
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        ctx.dataStore.edit { it[BIOMETRIC_LOCK] = enabled }
    }

    suspend fun setRealtimeNotificationsEnabled(enabled: Boolean) {
        ctx.dataStore.edit { it[REALTIME_NOTIFICATIONS_ENABLED] = enabled }
    }
}
