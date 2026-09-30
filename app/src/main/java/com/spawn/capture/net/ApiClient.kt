package com.spawn.capture.net

import com.spawn.capture.data.CapturedEvent
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    fun post(events: List<CapturedEvent>): Int {
        val array = JSONArray()
        for (event in events) {
            array.put(
                JSONObject()
                    .put("package", event.packageName)
                    .put("direction", event.direction)
                    .put("title", event.title ?: "")
                    .put("text", event.text ?: "")
                    .put("timestamp", event.timestamp)
            )
        }
        val connection = URL(ApiConfig.ENDPOINT).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Authorization", "Bearer ${ApiConfig.TOKEN}")
            connection.outputStream.use {
                it.write(array.toString().toByteArray(Charsets.UTF_8))
            }
            connection.responseCode
        } finally {
            connection.disconnect()
        }
    }
}
