package com.soundly.player

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.net.URLEncoder

class LocalAudioServer(private val context: Context, port: Int = 8080) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val params = session.parameters
        
        if (uri == "/stream") {
            val filePath = params["path"]?.firstOrNull()
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists() && file.isFile) {
                    try {
                        val mimeType = "audio/*"
                        // NanoHTTPD handles Range headers automatically if we provide a stream and length
                        return newFixedLengthResponse(
                            Response.Status.OK,
                            mimeType,
                            FileInputStream(file),
                            file.length()
                        ).apply {
                            addHeader("Accept-Ranges", "bytes")
                            addHeader("Access-Control-Allow-Origin", "*")
                        }
                    } catch (e: Exception) {
                        Log.e("LocalAudioServer", "Error serving file", e)
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
                    }
                }
            }
        }
        
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }

    fun getLocalIpAddress(): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            if (ipAddress == 0) return null
            val formattedIp = String.format(
                Locale.US,
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
            formattedIp
        } catch (e: Exception) {
            Log.e("LocalAudioServer", "Error getting IP", e)
            null
        }
    }

    fun getStreamUrl(filePath: String): String? {
        val ip = getLocalIpAddress() ?: return null
        return "http://$ip:$listeningPort/stream?path=${URLEncoder.encode(filePath, "UTF-8")}"
    }
}
