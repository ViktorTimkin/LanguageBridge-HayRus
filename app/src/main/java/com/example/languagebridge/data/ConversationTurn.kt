package com.example.languagebridge.data

data class ConversationTurn(
    val russianText: String,
    val armenianText: String,
    val armenianTranscription: String? = null
) {
    fun textFor(language: Language): String {
        return if (language == Language.RUSSIAN) {
            russianText
        } else {
            if (armenianTranscription != null) {
                "$armenianText ($armenianTranscription)"
            } else {
                armenianText
            }
        }
    }
}
