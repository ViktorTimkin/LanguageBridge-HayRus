
package com.example.languagebridge.data.translation

import com.example.languagebridge.data.TranslationOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class TextTranslationService(
    private val translatorKey: String
) {
    suspend fun translateText(
        text: String,
        sourceLangShort: String,
        targetLangShort: String,
    ): TranslationOutcome = withContext(Dispatchers.IO) {

        var connection: HttpURLConnection? = null

        try {
            val url = URL(
                "https://api.cognitive.microsofttranslator.com/translate" +
                        "?api-version=3.0&from=$sourceLangShort&to=$targetLangShort"
            )

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty(
                    "Ocp-Apim-Subscription-Key",
                    translatorKey
                )
                setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )
                doOutput = true
            }

            val requestBody = JSONArray().apply {
                put(JSONObject().apply {
                    put("Text", text)
                })
            }.toString()

            connection.outputStream.use { stream ->
                stream.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

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
                val error = connection.errorStream
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    ?: "HTTP $responseCode"

                TranslationOutcome.Error(error)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            TranslationOutcome.Error(
                "Превышено время ожидания ответа сервера"
            )
        } catch (e: Exception) {
            TranslationOutcome.Error(
                e.message ?: "Ошибка подключения к серверу"
            )
        } finally {
            connection?.disconnect()
        }
    }
}
