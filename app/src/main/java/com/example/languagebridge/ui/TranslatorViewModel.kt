
package com.example.languagebridge.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.languagebridge.data.ArmenianTransliterator
import com.example.languagebridge.data.ConversationTurn
import com.example.languagebridge.data.Language
import com.example.languagebridge.data.TranslationOutcome
import com.example.languagebridge.data.speech.SpeechRecognitionService
import com.example.languagebridge.data.speech.SpeechSynthesisService
import com.example.languagebridge.data.translation.TextTranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TranslatorViewModel(
    private val speechRecognition: SpeechRecognitionService,
    private val speechSynthesis: SpeechSynthesisService,
    private val textTranslation: TextTranslationService,
) : ViewModel() {

    val conversation = mutableStateListOf<ConversationTurn>()

    var isBusy by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    private var startJob: Job? = null
    private var activeLanguage: Language? = null

    fun clearConversation() {
        conversation.clear()
    }

    fun startListening(sourceLanguage: Language) {
        if (isBusy) return

        errorMessage = null
        isBusy = true
        activeLanguage = sourceLanguage

        startJob = viewModelScope.launch {
            try {
                speechRecognition.startListening(
                    sourceLang = sourceLanguage.speechLocale,
                    targetLangShort = sourceLanguage.other().translationCode,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Ошибка запуска микрофона: ${e.message}"
                activeLanguage = null
                isBusy = false
            }
        }
    }

    fun stopListening(sourceLanguage: Language) {
        if (activeLanguage != sourceLanguage) return

        activeLanguage = null
        val targetLanguage = sourceLanguage.other()

        viewModelScope.launch {
            try {
                startJob?.join()

                val outcome = speechRecognition.stopListening()
                    ?: return@launch

                when (outcome) {
                    is TranslationOutcome.Success -> {
                        conversation.add(
                            createTurn(sourceLanguage, outcome)
                        )

                        try {
                            speechSynthesis.speak(
                                outcome.translatedText,
                                targetLanguage.voiceName
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMessage = "Ошибка озвучки: ${e.message}"
                        }
                    }

                    is TranslationOutcome.Error -> {
                        errorMessage = outcome.message
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Ошибка обработки речи: ${e.message}"
            } finally {
                startJob = null
                isBusy = false
            }
        }
    }

    fun translateTyped(sourceLanguage: Language, text: String) {
        if (text.isBlank() || isBusy) return

        errorMessage = null
        isBusy = true

        val targetLanguage = sourceLanguage.other()

        viewModelScope.launch {
            try {
                when (
                    val outcome = textTranslation.translateText(
                        text = text,
                        sourceLangShort = sourceLanguage.translationCode,
                        targetLangShort = targetLanguage.translationCode,
                    )
                ) {
                    is TranslationOutcome.Success -> {
                        conversation.add(
                            createTurn(sourceLanguage, outcome)
                        )

                        try {
                            speechSynthesis.speak(
                                outcome.translatedText,
                                targetLanguage.voiceName
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMessage = "Ошибка озвучки: ${e.message}"
                        }
                    }

                    is TranslationOutcome.Error -> {
                        errorMessage = outcome.message
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Ошибка перевода: ${e.message}"
            } finally {
                isBusy = false
            }
        }
    }

    fun speakTurn(
        turn: ConversationTurn,
        language: Language,
        slow: Boolean
    ) {
        val text = if (language == Language.ARMENIAN) {
            turn.armenianText
        } else {
            turn.russianText
        }

        viewModelScope.launch {
            try {
                if (slow) {
                    speechSynthesis.speakSlow(
                        text,
                        language.voiceName,
                        language.speechLocale
                    )
                } else {
                    speechSynthesis.speak(
                        text,
                        language.voiceName
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage = "Ошибка озвучки: ${e.message}"
            }
        }
    }

    private fun createTurn(
        sourceLanguage: Language,
        outcome: TranslationOutcome.Success
    ): ConversationTurn {
        val russianText =
            if (sourceLanguage == Language.RUSSIAN) {
                outcome.recognizedText
            } else {
                outcome.translatedText
            }

        val armenianText =
            if (sourceLanguage == Language.ARMENIAN) {
                outcome.recognizedText
            } else {
                outcome.translatedText
            }

        return ConversationTurn(
            russianText = russianText,
            armenianText = armenianText,
            armenianTranscription =
                ArmenianTransliterator.transliterate(armenianText),
        )
    }
}
