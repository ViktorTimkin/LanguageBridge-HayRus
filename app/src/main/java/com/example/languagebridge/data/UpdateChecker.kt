package com.example.languagebridge.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val version: String,
    val apkUrl: String?,
    val pageUrl: String,
    val notes: String,
)

object UpdateChecker {
    private const val LATEST_URL =
        "https://api.github.com/repos/ViktorTimkin/LanguageBridge-HayRus/releases/latest"

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(LATEST_URL).openConnection() as HttpURLConnection
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "HayRus-App")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != 200) return@withContext null

            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val latest = json.getString("tag_name").removePrefix("v")
            if (!isNewer(latest, currentVersion)) return@withContext null

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            }

            UpdateInfo(
                version = latest,
                apkUrl = apkUrl,
                pageUrl = json.getString("html_url"),
                notes = json.optString("body", ""),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val a = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val b = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
