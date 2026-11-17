package com.example.alpha_ai.core.ui.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * What a screen, or one part of it, is showing: loading, nothing yet, a failure, or content.
 * Feature `UiState`s use it for anything that loads (constitution VI).
 */
@Immutable
sealed interface ScreenState<out T> {
    /** Work is in progress. [cancellable] adds a Cancel action. */
    data class Loading(val cancellable: Boolean = false) : ScreenState<Nothing>

    /** There is nothing to show yet; [actionLabelRes] names the one action to get started. */
    data class Empty(
        @StringRes val messageRes: Int,
        @StringRes val actionLabelRes: Int? = null,
    ) : ScreenState<Nothing>

    /** Something failed. Retry is offered only when [retryable] is true. */
    data class Error(@StringRes val messageRes: Int, val retryable: Boolean) : ScreenState<Nothing>

    data class Content<out T>(val value: T) : ScreenState<T>
}
