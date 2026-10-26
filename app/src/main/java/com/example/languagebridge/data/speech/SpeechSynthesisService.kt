
package com.example.languagebridge.data.speech

import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class SpeechSynthesisService(
    private val speechKey: String,
    private val speechRegion: String,
) {
    private val speechMutex = Mutex()

    suspend fun speak(text: String, voiceName: String) {
        speakInternal(text, voiceName)
    }

    suspend fun speakSlow(
        text: String,
        voiceName: String,
        languageCode: String
    ) {
        speakInternal(text, voiceName, languageCode)
    }

    private suspend fun speakInternal(
        text: String,
        voiceName: String,
        languageCode: String? = null
    ) {
        if (text.isBlank() || !speechMutex.tryLock()) return

        try {
            withContext(Dispatchers.IO) {
                val config = SpeechConfig.fromSubscription(
                    speechKey,
                    speechRegion
                )

                var audioConfig: AudioConfig? = null
                var synthesizer: SpeechSynthesizer? = null

                try {
                    config.speechSynthesisVoiceName = voiceName

                    audioConfig = AudioConfig.fromDefaultSpeakerOutput()
                    synthesizer = SpeechSynthesizer(config, audioConfig)

                    if (languageCode == null) {
                        synthesizer.SpeakTextAsync(text).get()
                    } else {
                        val ssml = buildSlowSsml(
                            text,
                            voiceName,
                            languageCode
                        )
                        synthesizer.SpeakSsmlAsync(ssml).get()
                    }
                } finally {
                    synthesizer?.close()
                    audioConfig?.close()
                    config.close()
                }
            }
        } finally {
            speechMutex.unlock()
        }
    }

    private fun buildSlowSsml(
        text: String,
        voiceName: String,
        languageCode: String
    ): String {
        val escapedText = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val words = escapedText.trim().split(Regex("\\s+"))

        val spokenText = words.joinToString(" ") { word ->
            "$word<break time=\"700ms\"/>"
        }

        return """
            <speak version="1.0"
                   xmlns="http://www.w3.org/2001/10/synthesis"
                   xml:lang="$languageCode">
                <voice name="$voiceName">
                    <prosody rate="-30%">
                        $spokenText
                    </prosody>
                </voice>
            </speak>
        """.trimIndent()
    }
}
