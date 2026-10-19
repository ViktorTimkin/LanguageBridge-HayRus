package com.example.languagebridge.data

enum class Language(
    val displayName: String,
    val speechLocale: String,
    val translationCode: String,
    val voiceName: String,
    val holdToSpeakPrompt: String
) {
    RUSSIAN(
        displayName = "Русский",
        speechLocale = "ru-RU",
        translationCode = "ru",
        voiceName = "ru-RU-SvetlanaNeural",
        holdToSpeakPrompt = "Зажмите, чтобы сказать"
    ),
    ARMENIAN(
        displayName = "Հայերեն",
        speechLocale = "hy-AM",
        translationCode = "hy",
        voiceName = "hy-AM-AnahitNeural",
        holdToSpeakPrompt = "Սեղմեք և ասեք"
    );

    fun other(): Language = if (this == RUSSIAN) ARMENIAN else RUSSIAN
}
