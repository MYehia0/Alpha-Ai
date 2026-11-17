package com.example.alpha_ai.core.data.ai.validate

import com.example.alpha_ai.core.domain.ai.AiLimits
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.domain.ai.ImageInput
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import com.example.alpha_ai.core.domain.ai.Language

/** The outcome of checking an input: accepted with a resolved value, or rejected with a reason. */
internal sealed interface InputCheck<out T> {
    data class Accepted<out T>(val value: T) : InputCheck<T>

    data class Rejected(val reason: InvalidInputReason) : InputCheck<Nothing>
}

/**
 * Enforces [AiLimits] before any model is called.
 * The rules are listed in specs/002-ai-provider-layer/data-model.md, section 3.
 */
internal object InputValidator {

    /** Accepts with the language to correct in: the stated one, or the detected one. */
    fun grammar(text: String, language: Language?): InputCheck<Language> =
        text(text, minChars = 1, maxChars = AiLimits.GRAMMAR_MAX_CHARS, language = language)

    /** Accepts with the language to summarize in: the stated one, or the detected one. */
    fun summary(text: String, language: Language?): InputCheck<Language> =
        text(
            text,
            minChars = AiLimits.SUMMARY_MIN_CHARS,
            maxChars = AiLimits.SUMMARY_MAX_CHARS,
            language = language,
        )

    fun translation(text: String, from: Language, to: Language): InputCheck<Unit> {
        if (from == to) return InputCheck.Rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE)
        return when (
            val check = text(text, minChars = 1, maxChars = AiLimits.TRANSLATION_MAX_CHARS, language = from)
        ) {
            is InputCheck.Accepted -> InputCheck.Accepted(Unit)
            is InputCheck.Rejected -> check
        }
    }

    fun detection(text: String): InputCheck<Unit> =
        if (text.isBlank()) InputCheck.Rejected(InvalidInputReason.EMPTY) else InputCheck.Accepted(Unit)

    fun speech(audio: AudioClip): InputCheck<Unit> = when {
        audio.bytes.isEmpty() -> InputCheck.Rejected(InvalidInputReason.EMPTY)
        audio.mimeType !in AiLimits.SPEECH_MIME_TYPES ->
            InputCheck.Rejected(InvalidInputReason.UNSUPPORTED_FORMAT)
        audio.durationMillis > AiLimits.SPEECH_MAX_MILLIS ->
            InputCheck.Rejected(InvalidInputReason.TOO_LONG)
        audio.bytes.size > AiLimits.SPEECH_MAX_BYTES -> InputCheck.Rejected(InvalidInputReason.TOO_LARGE)
        else -> InputCheck.Accepted(Unit)
    }

    fun image(image: ImageInput): InputCheck<Unit> = when {
        image.bytes.isEmpty() -> InputCheck.Rejected(InvalidInputReason.EMPTY)
        image.mimeType !in AiLimits.IMAGE_MIME_TYPES ->
            InputCheck.Rejected(InvalidInputReason.UNSUPPORTED_FORMAT)
        image.bytes.size > AiLimits.IMAGE_MAX_BYTES -> InputCheck.Rejected(InvalidInputReason.TOO_LARGE)
        else -> InputCheck.Accepted(Unit)
    }

    private fun text(text: String, minChars: Int, maxChars: Int, language: Language?): InputCheck<Language> {
        val length = text.trim().length
        if (length == 0) return InputCheck.Rejected(InvalidInputReason.EMPTY)
        if (length < minChars) return InputCheck.Rejected(InvalidInputReason.TOO_SHORT)
        if (length > maxChars) return InputCheck.Rejected(InvalidInputReason.TOO_LONG)

        val resolved = if (language != null) {
            language.takeIf { ScriptAnalyzer.isAcceptedAs(text, it) }
        } else {
            ScriptAnalyzer.detect(text).toLanguageOrNull()
        }
        return resolved?.let { InputCheck.Accepted(it) }
            ?: InputCheck.Rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE)
    }
}
