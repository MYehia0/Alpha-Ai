package com.example.alpha_ai.core.ui.state

import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.ui.ai.messageRes

/**
 * Whether the error state should offer Retry for this failure (FR-015). The decision belongs to
 * the AI layer ([AiFailure.canRetry], spec 002); this is the one place the UI reads it.
 */
fun AiFailure.isRetryable(): Boolean = canRetry

/** The error state for a failed AI request: its localized message and, if it helps, Retry. */
fun AiFailure.toErrorState(): ScreenState.Error =
    ScreenState.Error(messageRes = messageRes(), retryable = isRetryable())
