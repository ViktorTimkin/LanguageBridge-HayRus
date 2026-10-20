package com.example.languagebridge.data.speech

import com.example.languagebridge.data.TranslationOutcome
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Распознавание речи с одновременным переводом через Azure Speech. */
class SpeechRecognitionService(
    private val speechKey: String,
    private val speechRegion: String,
) {
    private var recognizer: TranslationRecognizer? = null
    private var activeConfig: SpeechTranslationConfig? = null
    private var targetLangShort: String = ""

    private val recognizedBuilder = StringBuilder()
    private val translatedBuilder = StringBuilder()

    fun startListening(sourceLang: String, targetLangShort: String) {
        this.targetLangShort = targetLangShort
        recognizedBuilder.clear()
        translatedBuilder.clear()

        val config = SpeechTranslationConfig.fromSubscription(speechKey, speechRegion)
        config.speechRecognitionLanguage = sourceLang
        config.addTargetLanguage(targetLangShort)
        config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "700")

        val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        val newRecognizer = TranslationRecognizer(config, audioConfig)

        newRecognizer.recognized.addEventListener { _, event ->
            if (event.result.reason == ResultReason.TranslatedSpeech) {
                val text = event.result.text
                val translated = event.result.translations[targetLangShort] ?: ""
                if (text.isNotBlank()) {
                    if (recognizedBuilder.isNotEmpty()) recognizedBuilder.append(" ")
                    recognizedBuilder.append(text)
                }
                if (translated.isNotBlank()) {
                    if (translatedBuilder.isNotEmpty()) translatedBuilder.append(" ")
                    translatedBuilder.append(translated)
                }
            }
        }

        recognizer = newRecognizer
        activeConfig = config
        newRecognizer.startContinuousRecognitionAsync().get()
    }

    suspend fun stopListening(): TranslationOutcome? = withContext(Dispatchers.IO) {
        val activeRecognizer = recognizer ?: return@withContext null

        try {
            activeRecognizer.stopContinuousRecognitionAsync().get()
        } catch (e: Exception) {
            return@withContext TranslationOutcome.Error(e.message ?: "Unknown error")
        } finally {
            activeRecognizer.close()
            activeConfig?.close()
            recognizer = null
        }

        if (recognizedBuilder.isEmpty()) {
            TranslationOutcome.Error("Речь не распознана. Попробуйте ещё раз.")
        } else {
            TranslationOutcome.Success(
                recognizedText = recognizedBuilder.toString(),
                translatedText = translatedBuilder.toString(),
            )
        }
    }

}
