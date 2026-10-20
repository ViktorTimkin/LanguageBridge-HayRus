package com.example.languagebridge.data.translation

import com.example.languagebridge.data.TranslationOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Перевод введённого текста через Azure Translator REST API. */
class TextTranslationService(private val translatorKey: String) {
    suspend fun translateText(
        text: String,
        sourceLangShort: String,
        targetLangShort: String,
    ): TranslationOutcome = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.cognitive.microsofttranslator.com/translate" +
                        "?api-version=3.0&from=$sourceLangShort&to=$targetLangShort"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", translatorKey)
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.doOutput = true

            val requestBody = JSONArray().apply {
                put(JSONObject().apply { put("Text", text) })
            }.toString()

            connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val translatedText = JSONArray(response)
                    .getJSONObject(0)
                    .getJSONArray("translations")
                    .getJSONObject(0)
                    .getString("text")
                TranslationOutcome.Success(
                    recognizedText = text,
                    translatedText = translatedText,
                )
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: "HTTP ${connection.responseCode}"
                TranslationOutcome.Error(error)
            }
        } catch (e: Exception) {
            TranslationOutcome.Error(e.message ?: "Unknown error")
        }
    }
}
