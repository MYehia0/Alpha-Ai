package com.example.alpha_ai.core.domain.ai

/**
 * Input limits for every capability. Input outside them is rejected before any model is
 * called. A later feature may change a value here; nothing else needs to change.
 */
object AiLimits {
    const val GRAMMAR_MAX_CHARS: Int = 5_000
    const val TRANSLATION_MAX_CHARS: Int = 5_000
    const val SUMMARY_MIN_CHARS: Int = 200
    const val SUMMARY_MAX_CHARS: Int = 20_000

    const val SPEECH_MAX_MILLIS: Long = 60_000
    const val SPEECH_MAX_BYTES: Int = 5 * 1024 * 1024
    val SPEECH_MIME_TYPES: Set<String> =
        setOf("audio/wav", "audio/mpeg", "audio/mp4", "audio/ogg", "audio/flac")

    const val IMAGE_MAX_BYTES: Int = 4 * 1024 * 1024
    val IMAGE_MIME_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")
}
