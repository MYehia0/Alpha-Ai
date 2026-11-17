package com.example.alpha_ai.core.domain.ai

/**
 * One step of a streamed text result.
 *
 * A stream emits any number of [Delta] and [Restarted] events and then exactly one terminal
 * event: [Completed] or [Failed].
 */
sealed interface TextStreamEvent {
    /** The next piece of text. Append it to what is shown. */
    data class Delta(val text: String) : TextStreamEvent

    /** Another model took over after a failure. Discard the text shown so far. */
    data object Restarted : TextStreamEvent

    /** The full result, cleaned and checked. Replace the text shown with [text]. */
    data class Completed(val text: String) : TextStreamEvent

    data class Failed(val failure: AiFailure) : TextStreamEvent
}
