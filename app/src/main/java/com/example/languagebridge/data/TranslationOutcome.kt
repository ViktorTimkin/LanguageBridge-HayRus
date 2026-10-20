package com.example.languagebridge.data

sealed class TranslationOutcome {
    data class Success(val recognizedText: String, val translatedText: String) : TranslationOutcome()
    data class Error(val message: String) : TranslationOutcome()
}
