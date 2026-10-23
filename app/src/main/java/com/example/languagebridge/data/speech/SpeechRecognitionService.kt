
package com.example.languagebridge.data.speech

import com.example.languagebridge.data.TranslationOutcome
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
    private var targetLangShort: String = ""

    private val recognizedBuilder = StringBuilder()
    private val translatedBuilder = StringBuilder()

    suspend fun startListening(
        sourceLang: String,
        targetLangShort: String
    ) = withContext(Dispatchers.IO) {
        check(recognizer == null) {
            "Распознавание уже запущено"
        }

        this@SpeechRecognitionService.targetLangShort = targetLangShort
        recognizedBuilder.clear()
        translatedBuilder.clear()

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

                        synchronized(recognizedBuilder) {
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

                synchronized(recognizedBuilder) {
                    if (recognizedBuilder.isEmpty()) {
                        TranslationOutcome.Error(
                            "Речь не распознана. Попробуйте ещё раз."
                        )
                    } else {
                        TranslationOutcome.Success(
                            recognizedText = recognizedBuilder.toString(),
                            translatedText = translatedBuilder.toString(),
                        )
                    }
                }
            } catch (e: Exception) {
                TranslationOutcome.Error(
                    e.message ?: "Ошибка распознавания речи"
                )
            } finally {
                activeRecognizer.close()
                activeConfig?.close()
                recognizer = null
                activeConfig = null
            }
        }
}
