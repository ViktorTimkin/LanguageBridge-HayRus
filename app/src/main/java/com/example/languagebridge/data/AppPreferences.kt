package com.example.languagebridge.data

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "hayrus_prefs"
    private const val KEY_TOP_LANGUAGE = "top_language"
    private const val KEY_TEXT_SCALE = "text_scale_index"

    val TEXT_SCALES = listOf(1.0f, 1.3f, 1.6f)
    val TEXT_SCALE_LABELS = listOf("Обычный", "Крупный", "Очень крупный")

    fun saveTopLanguage(context: Context, language: Language) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOP_LANGUAGE, language.name)
            .apply()
    }

    fun loadTopLanguage(context: Context): Language {
        val saved = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TOP_LANGUAGE, null)
        return saved
            ?.let { runCatching { Language.valueOf(it) }.getOrNull() }
            ?: Language.ARMENIAN
    }

    fun saveTextScaleIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TEXT_SCALE, index)
            .apply()
    }

    fun loadTextScaleIndex(context: Context): Int {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TEXT_SCALE, 0)
            .coerceIn(0, TEXT_SCALES.lastIndex)
    }
}
