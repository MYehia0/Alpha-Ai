package com.example.alpha_ai.core.domain.ai

/**
 * Recorded speech held in memory. The caller owns, and deletes, any file it was read from.
 *
 * @param mimeType one of [AiLimits.SPEECH_MIME_TYPES]
 */
class AudioClip(
    val bytes: ByteArray,
    val mimeType: String,
    val durationMillis: Long,
)
