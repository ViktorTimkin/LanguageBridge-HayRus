package com.example.languagebridge.data

import android.content.Context

object AppPreferences {
    private const val PREFS_NAME = "hayrus_prefs"
    private const val KEY_TOP_LANGUAGE = "top_language"

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
}