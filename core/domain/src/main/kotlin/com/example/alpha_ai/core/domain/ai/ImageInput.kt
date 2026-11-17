package com.example.alpha_ai.core.domain.ai

/**
 * An image held in memory. The caller downscales it and owns any file it was read from.
 *
 * @param mimeType one of [AiLimits.IMAGE_MIME_TYPES]
 */
class ImageInput(
    val bytes: ByteArray,
    val mimeType: String,
)
