package com.example.alpha_ai.core.ui.ai

import androidx.annotation.StringRes
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import com.example.alpha_ai.core.ui.R

/**
 * The message to show for a failed AI request, in the user's language.
 *
 * Both `when` expressions have no `else` branch on purpose: a new failure or reason does not
 * compile until it has a message.
 */
@StringRes
fun AiFailure.messageRes(): Int = when (this) {
    AiFailure.NoConnection -> R.string.ai_failure_no_connection
    AiFailure.TimedOut -> R.string.ai_failure_timed_out
    is AiFailure.ServiceBusy -> R.string.ai_failure_service_busy
    AiFailure.ServiceUnavailable -> R.string.ai_failure_service_unavailable
    AiFailure.InvalidResponse -> R.string.ai_failure_invalid_response
    AiFailure.Unknown -> R.string.ai_failure_unknown
    is AiFailure.InvalidInput -> when (reason) {
        InvalidInputReason.EMPTY -> R.string.ai_input_empty
        InvalidInputReason.TOO_SHORT -> R.string.ai_input_too_short
        InvalidInputReason.TOO_LONG -> R.string.ai_input_too_long
        InvalidInputReason.TOO_LARGE -> R.string.ai_input_too_large
        InvalidInputReason.UNSUPPORTED_LANGUAGE -> R.string.ai_input_unsupported_language
        InvalidInputReason.UNSUPPORTED_FORMAT -> R.string.ai_input_unsupported_format
    }
}
