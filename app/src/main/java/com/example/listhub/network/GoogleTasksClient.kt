package com.example.listhub.network

import com.example.listhub.BuildConfig
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GoogleTasksClient {

    private const val BASE_URL = "https://tasks.googleapis.com/tasks/v1/"

    /**
     * Appends our GOOGLE_TASKS_API_KEY (from local.properties -> BuildConfig)
     * to every outgoing request as a query parameter. Google associates API
     * usage/quota with this key. The actual per-user authorization still comes
     * from the OAuth Bearer token we attach separately per-request (see
     * GoogleTasksApiService) - the API key alone can't read anyone's private
     * task data.
     */
    private val apiKeyInterceptor = Interceptor { chain ->
        val original = chain.request()
        val originalUrl = original.url
        val newUrl: HttpUrl = originalUrl.newBuilder()
            .addQueryParameter("key", BuildConfig.GOOGLE_TASKS_API_KEY)
            .build()
        val newRequest = original.newBuilder().url(newUrl).build()
        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val api: GoogleTasksApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleTasksApiService::class.java)
    }

    fun bearerHeader(accessToken: String): Map<String, String> =
        mapOf("Authorization" to "Bearer $accessToken")
}
