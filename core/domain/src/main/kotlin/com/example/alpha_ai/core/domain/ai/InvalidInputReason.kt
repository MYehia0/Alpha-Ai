package com.example.alpha_ai.core.domain.ai

/** Why an input was rejected before any model was called. */
enum class InvalidInputReason {
    EMPTY,
    TOO_SHORT,
    TOO_LONG,
    TOO_LARGE,
    UNSUPPORTED_LANGUAGE,
    UNSUPPORTED_FORMAT,
}
