
package com.example.languagebridge.data.speech

import com.example.languagebridge.data.TranslationOutcome
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpeechRecognitionService(
    private val speechKey: String,
    private val speechRegion: String,
) {
    private var recognizer: TranslationRecognizer? = null
    private var activeConfig: SpeechTranslationConfig? = null

    private val recognizedBuilder = StringBuilder()
    private val translatedBuilder = StringBuilder()

    private val resultLock = Any()
    private var recognitionError: String? = null

    suspend fun startListening(
        sourceLang: String,
        targetLangShort: String
    ) = withContext(Dispatchers.IO) {
        check(recognizer == null) {
            "Распознавание уже запущено"
        }

        synchronized(resultLock) {
            recognizedBuilder.clear()
            translatedBuilder.clear()
            recognitionError = null
        }

        val config = SpeechTranslationConfig.fromSubscription(
            speechKey,
            speechRegion
        )

        try {
            config.speechRecognitionLanguage = sourceLang
            config.addTargetLanguage(targetLangShort)

            config.setProperty(
                PropertyId.Speech_SegmentationSilenceTimeoutMs,
                "700"
            )

            val audioConfig = AudioConfig.fromDefaultMicrophoneInput()

            val newRecognizer = try {
                TranslationRecognizer(config, audioConfig)
            } finally {
                audioConfig.close()
            }

            try {
                newRecognizer.recognized.addEventListener { _, event ->
                    if (event.result.reason == ResultReason.TranslatedSpeech) {
                        val text = event.result.text
                        val translated =
                            event.result.translations[targetLangShort] ?: ""

                        synchronized(resultLock) {
                            if (text.isNotBlank()) {
                                if (recognizedBuilder.isNotEmpty()) {
                                    recognizedBuilder.append(" ")
                                }
                                recognizedBuilder.append(text)
                            }

                            if (translated.isNotBlank()) {
                                if (translatedBuilder.isNotEmpty()) {
                                    translatedBuilder.append(" ")
                                }
                                translatedBuilder.append(translated)
                            }
                        }
                    }
                }

                newRecognizer.canceled.addEventListener { _, event ->
                    if (event.reason == CancellationReason.Error) {
                        synchronized(resultLock) {
                            recognitionError = "Ошибка Azure Speech. " +
                                    "Проверьте интернет и попробуйте ещё раз."
                        }
                    }
                }

                recognizer = newRecognizer
                activeConfig = config

                newRecognizer.startContinuousRecognitionAsync().get()
            } catch (e: Exception) {
                recognizer = null
                activeConfig = null
                newRecognizer.close()
                throw e
            }
        } catch (e: Exception) {
            config.close()
            throw e
        }
    }

    suspend fun stopListening(): TranslationOutcome? =
        withContext(Dispatchers.IO) {
            val activeRecognizer = recognizer
                ?: return@withContext null

            try {
                activeRecognizer.stopContinuousRecognitionAsync().get()

                synchronized(resultLock) {
                    val error = recognitionError

                    when {
                        error != null -> {
                            TranslationOutcome.Error(error)
                        }

                        recognizedBuilder.isEmpty() -> {
                            TranslationOutcome.Error(
                                "Речь не распознана. Попробуйте ещё раз."
                            )
                        }

                        else -> {
                            TranslationOutcome.Success(
                                recognizedText = recognizedBuilder.toString(),
                                translatedText = translatedBuilder.toString(),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                TranslationOutcome.Error(
                    "Ошибка остановки распознавания: " +
                            (e.message ?: "Неизвестная ошибка")
                )
            } finally {
                activeRecognizer.close()
                activeConfig?.close()
                recognizer = null
                activeConfig = null
            }
        }
}
