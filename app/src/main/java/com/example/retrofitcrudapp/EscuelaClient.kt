package com.example.retrofitcrudapp

import android.content.Context
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object EscuelaClient {
    const val ALUMNOS_URL = "https://66240d4504457d4aaf9b8530.mockapi.io/api/escuela/alumno"
    const val PROFESORES_URL = "https://6ad813a395e98f36aef2ea16.mockapi.io/profesor"
    const val PREFS = "escuela_api"

    fun resourceUrl(context: Context, entidad: Entidad): String {
        val fallback = if (entidad == Entidad.ALUMNO) ALUMNOS_URL else PROFESORES_URL
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(entidad.recurso + "_url", fallback) ?: fallback
    }

    fun localUrl(entidad: Entidad) = "http://10.0.2.2:8765/api/escuela/${entidad.recurso}"

    fun recordUrl(collection: String, id: String): String = collection.toHttpUrlOrNull()!!
        .newBuilder().addPathSegment(id).build().toString()

    fun normalizarUrl(value: String): String? {
        val url = (value.trim().trimEnd('/') + "/").toHttpUrlOrNull() ?: return null
        if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) return null
        if (url.encodedPath == "/") return null
        if (url.scheme != "https" && url.host !in setOf("10.0.2.2", "127.0.0.1", "localhost")) return null
        return url.toString().trimEnd('/')
    }

    fun create(context: Context): EscuelaApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder().addInterceptor(logging)
            // No repetir automáticamente un POST si la respuesta se pierde.
            .retryOnConnectionFailure(false)
            .callTimeout(25, TimeUnit.SECONDS).build()
        // @Url usa la URL completa de cada colección, aunque estén en hosts distintos.
        return Retrofit.Builder().baseUrl("https://localhost/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(EscuelaApi::class.java)
    }
}
