package com.soundly.cloud

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response as NPResponse
import java.util.concurrent.TimeUnit

class NewPipeDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    override fun execute(request: NPRequest): NPResponse {
        android.util.Log.d("NewPipeDownloader", "Ejecutando petición: ${request.url()}")
        val okRequest = Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), getRequestBody(request))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .apply {
                request.headers().forEach { (key, values) ->
                    values.forEach { addHeader(key, it) }
                }
            }
            .build()

        return try {
            client.newCall(okRequest).execute().use { response ->
                val body = response.body?.string() ?: ""
                android.util.Log.d("NewPipeDownloader", "Respuesta recibida: ${response.code} para ${request.url()} (Tamaño: ${body.length})")
                NPResponse(
                    response.code, response.message, response.headers.toMultimap(),
                    body, response.request.url.toString()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("NewPipeDownloader", "Error en petición: ${request.url()}", e)
            throw e
        }
    }

    private fun getRequestBody(request: NPRequest) = when {
        request.dataToSend() != null && request.dataToSend()!!.isNotEmpty() -> request.dataToSend()!!.toRequestBody(null)
        request.httpMethod() == "POST" -> "".toRequestBody(null)
        else -> null
    }
}

val sharedHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(10, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
    .build()
