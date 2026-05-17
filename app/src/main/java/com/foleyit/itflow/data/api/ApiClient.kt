package com.foleyit.itflow.data.api

import com.foleyit.itflow.BuildConfig
import com.foleyit.itflow.data.ssl.FingerprintTrustManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

object ApiClient {
    private var _serverUrl: String = ""
    private var _token: String? = null
    private var _trustedCertSha: String? = null
    private var _service: ApiService? = null

    val serverUrl get() = _serverUrl

    // Set in MainActivity to auto-navigate to login on 401
    var onUnauthorized: (() -> Unit)? = null

    fun init(serverUrl: String, token: String?, trustedCertSha: String? = null) {
        _serverUrl = serverUrl.trimEnd('/')
        _token = token
        _trustedCertSha = trustedCertSha
        _service = buildService()
    }

    fun setToken(token: String) {
        _token = token
        _service = buildService()
    }

    fun clearToken() {
        _token = null
        _service = buildService()
    }

    fun setTrustedCert(sha: String?) {
        _trustedCertSha = sha
        _service = buildService()
    }

    fun service(): ApiService = _service ?: error("ApiClient not initialized")

    private fun buildService(): ApiService {
        val trustManager = FingerprintTrustManager(_trustedCertSha)
        val sslContext = SSLContext.getInstance("TLS").also {
            it.init(null, arrayOf(trustManager), null)
        }

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // Attach Bearer token
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    _token?.let { addHeader("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(req)
            }
            // 401 → clear credentials + navigate to login
            .addInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (response.code == 401 && _token != null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onUnauthorized?.invoke()
                    }
                }
                response
            }
            // Logging debug-only — never expose request URLs in release builds
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("$_serverUrl/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
