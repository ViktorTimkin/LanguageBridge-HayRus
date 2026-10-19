package com.example.languagebridge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.languagebridge.data.speech.SpeechRecognitionService
import com.example.languagebridge.data.speech.SpeechSynthesisService
import com.example.languagebridge.data.translation.TextTranslationService

class TranslatorViewModelFactory(
    private val speechRecognition: SpeechRecognitionService,
    private val speechSynthesis: SpeechSynthesisService,
    private val textTranslation: TextTranslationService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TranslatorViewModel(speechRecognition, speechSynthesis, textTranslation) as T
    }
}
