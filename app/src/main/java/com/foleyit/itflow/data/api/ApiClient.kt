package com.foleyit.itflow.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var _serverUrl: String = ""
    private var _token: String? = null
    private var _service: ApiService? = null

    val serverUrl get() = _serverUrl

    fun init(serverUrl: String, token: String?) {
        _serverUrl = serverUrl.trimEnd('/')
        _token = token
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

    fun service(): ApiService = _service ?: error("ApiClient not initialized. Call init() first.")

    private fun buildService(): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    _token?.let { addHeader("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        return Retrofit.Builder()
            .baseUrl("$_serverUrl/api/v1/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
