package com.example.languagebridge.data.speech

import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Озвучивание готового текста (обычное и замедленное). */
class SpeechSynthesisService(
    private val speechKey: String,
    private val speechRegion: String,
) {
    suspend fun speak(text: String, voiceName: String): Unit = withContext(Dispatchers.IO) {
        val config = SpeechConfig.fromSubscription(speechKey, speechRegion)
        config.speechSynthesisVoiceName = voiceName
        val audioOutputConfig = AudioConfig.fromDefaultSpeakerOutput()
        val synthesizer = SpeechSynthesizer(config, audioOutputConfig)
        try {
            synthesizer.SpeakTextAsync(text).get()
        } finally {
            synthesizer.close()
            config.close()
        }
    }
    suspend fun speakSlow(
        text: String,
        voiceName: String,
        languageCode: String
    ): Unit = withContext(Dispatchers.IO) {
        val config = SpeechConfig.fromSubscription(speechKey, speechRegion)
        val audioOutputConfig = AudioConfig.fromDefaultSpeakerOutput()
        val synthesizer = SpeechSynthesizer(config, audioOutputConfig)

        try {
            val escapedText = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;")

            val words = escapedText.split(Regex("\\s+"))

            val spokenText = words.joinToString(" ") { word ->
                "$word<break time=\"700ms\"/>"
            }

            val ssml = """
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

            synthesizer.SpeakSsmlAsync(ssml).get()
        } finally {
            synthesizer.close()
            config.close()
        }
    }
}
