package com.example.alpha_ai.core.data.ai.validate

import com.example.alpha_ai.core.domain.ai.DetectedLanguage
import com.example.alpha_ai.core.domain.ai.Language

/**
 * Tells Arabic from English by counting letters per Unicode script. The two languages use
 * different scripts, so this is exact for them, instant, and needs no network.
 *
 * Known limit: another Latin-script language is reported as English, and Persian or Urdu as
 * Arabic.
 */
internal object ScriptAnalyzer {

    /** A script must hold at least this share of the letters to be the detected language. */
    const val DETECTION_SHARE = 0.60

    /**
     * When the caller states the language, text is accepted from this share upwards, so Arabic
     * writing with many English terms (and the reverse) is not rejected.
     */
    const val STATED_LANGUAGE_MIN_SHARE = 0.30

    fun detect(text: String): DetectedLanguage {
        val counts = count(text)
        if (counts.letters == 0) return DetectedLanguage.UNSUPPORTED
        return when {
            counts.arabic >= DETECTION_SHARE * counts.letters -> DetectedLanguage.ARABIC
            counts.latin >= DETECTION_SHARE * counts.letters -> DetectedLanguage.ENGLISH
            else -> DetectedLanguage.UNSUPPORTED
        }
    }

    /** The fraction of the letters in [text] that are in [language]'s script; 0 with no letters. */
    fun share(text: String, language: Language): Double {
        val counts = count(text)
        if (counts.letters == 0) return 0.0
        val matching = when (language) {
            Language.ARABIC -> counts.arabic
            Language.ENGLISH -> counts.latin
        }
        return matching.toDouble() / counts.letters
    }

    fun isAcceptedAs(text: String, language: Language): Boolean =
        share(text, language) >= STATED_LANGUAGE_MIN_SHARE

    private class Counts(val letters: Int, val arabic: Int, val latin: Int)

    private fun count(text: String): Counts {
        var letters = 0
        var arabic = 0
        var latin = 0
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            index += Character.charCount(codePoint)
            if (!Character.isLetter(codePoint)) continue
            letters++
            when (Character.UnicodeScript.of(codePoint)) {
                Character.UnicodeScript.ARABIC -> arabic++
                Character.UnicodeScript.LATIN -> latin++
                else -> Unit
            }
        }
        return Counts(letters, arabic, latin)
    }
}

internal fun DetectedLanguage.toLanguageOrNull(): Language? = when (this) {
    DetectedLanguage.ARABIC -> Language.ARABIC
    DetectedLanguage.ENGLISH -> Language.ENGLISH
    DetectedLanguage.UNSUPPORTED -> null
}
