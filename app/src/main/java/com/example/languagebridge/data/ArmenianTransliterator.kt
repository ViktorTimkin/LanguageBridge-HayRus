
package com.example.languagebridge.data

import java.util.Locale

object ArmenianTransliterator {

    private const val ACUTE = '\u0301'

    private val letterMap = mapOf(
        'ա' to "а",
        'բ' to "б",
        'գ' to "г",
        'դ' to "д",
        'ե' to "е",
        'զ' to "з",
        'է' to "э",
        'ը' to "э",
        'թ' to "т",
        'ժ' to "ж",
        'ի' to "и",
        'լ' to "л",
        'խ' to "х",
        'ծ' to "ц",
        'կ' to "к",
        'հ' to "х",
        'ձ' to "дз",
        'ղ' to "х",
        'ճ' to "ч",
        'մ' to "м",
        'յ' to "й",
        'ն' to "н",
        'շ' to "ш",
        'չ' to "ч",
        'պ' to "п",
        'ջ' to "дж",
        'ռ' to "р",
        'ս' to "с",
        'վ' to "в",
        'տ' to "т",
        'ր' to "р",
        'ց' to "ц",
        'փ' to "п",
        'ք' to "к",
        'օ' to "о",
        'ֆ' to "ф",
        'և' to "ев"
    )

    private const val VOWELS = "աեէըիոօւ"

    private val exceptions = mapOf(
        "ողջույն" to "вохчу\u0301йн"
    )

    private val wordRegex =
        Regex("[\\u0531-\\u0556\\u0561-\\u0587]+")

    /**
     * Главный метод.
     * Сохраняет пробелы и большую часть пунктуации.
     */
    fun transliterate(text: String): String {
        if (text.isEmpty()) return text

        val cleanText = text
            .replace("՞", "")
            .replace("՜", "")
            .replace("՛", "")
            .replace("՝", ",")
            .replace("։", ".")

        return wordRegex.replace(cleanText) { match ->
            transliterateWord(match.value)
        }
    }

    /**
     * Преобразует одно армянское слово.
     */
    private fun transliterateWord(word: String): String {
        if (word.isEmpty()) return word

        val lower = word.lowercase(Locale.ROOT)

        exceptions[lower]?.let { exception ->
            return restoreCase(exception, word)
        }

        val result = StringBuilder()

        val vowelPositions = mutableListOf<Pair<Int, Boolean>>()

        fun appendSound(
            sound: String,
            vowel: Boolean = false,
            schwa: Boolean = false
        ) {
            result.append(sound)

            if (vowel) {
                vowelPositions.add(result.length to schwa)
            }
        }

        var i = 0

        while (i < lower.length) {
            val current = lower[i]
            val next = lower.getOrNull(i + 1)
            val third = lower.getOrNull(i + 2)

            if (current == 'ղ' && next == 'ջ') {
                appendSound("хч")
                i += 2
                continue
            }

            if (current == 'ո' && next == 'ւ') {
                appendSound("у", vowel = true)
                i += 2
                continue
            }

            val afterConsonant =
                i > 0 && lower[i - 1] !in VOWELS &&
                        lower[i - 1] != 'և'

            if (current == 'յ' && afterConsonant) {

                if (next == 'ա') {
                    appendSound("я", vowel = true)
                    i += 2
                    continue
                }

                if (next == 'ո' && third == 'ւ') {
                    appendSound("ю", vowel = true)
                    i += 3
                    continue
                }
            }

            when (current) {

                'ե' -> {
                    appendSound("е", vowel = true)
                }

                'ո' -> {
                    appendSound(
                        if (i == 0) "во" else "о",
                        vowel = true
                    )
                }

                'և' -> {
                    appendSound("ев")
                    vowelPositions.add(
                        (result.length - 1) to false
                    )
                }

                'ա', 'է', 'ը', 'ի', 'օ' -> {
                    appendSound(
                        letterMap.getValue(current),
                        vowel = true,
                        schwa = current == 'ը'
                    )
                }

                'ւ' -> {
                    appendSound("в")
                }

                else -> {
                    appendSound(
                        letterMap[current] ?: current.toString()
                    )
                }
            }

            i++
        }

        if (vowelPositions.size >= 2) {

            val stressedVowel = vowelPositions
                .lastOrNull { !it.second }

            if (stressedVowel != null) {
                result.insert(stressedVowel.first, ACUTE)
            }
        }

        return restoreCase(result.toString(), word)
    }

    private fun restoreCase(
        transcription: String,
        original: String
    ): String {
        if (original.isEmpty()) return transcription

        return when {
            original.all { it.isUpperCase() } ->
                transcription.uppercase(Locale.ROOT)

            original.first().isUpperCase() ->
                transcription.replaceFirstChar {
                    it.titlecase(Locale.ROOT)
                }

            else -> transcription
        }
    }
}
