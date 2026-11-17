package com.example.alpha_ai.core.designsystem.component

/** Loading shows nothing for this long, so quick operations never flash an indicator. */
const val LOADING_SHOW_DELAY_MS: Long = 400

/** Once the indicator has appeared, it stays at least this long. */
const val LOADING_MIN_VISIBLE_MS: Long = 500

/**
 * Whether the loading indicator is visible [elapsedMs] after the work started.
 * [finishedAtMs] is when the work ended, measured from the same start, or null while it runs.
 */
fun loadingVisible(elapsedMs: Long, finishedAtMs: Long?): Boolean {
    if (elapsedMs < LOADING_SHOW_DELAY_MS) return false
    if (finishedAtMs == null) return true
    if (finishedAtMs < LOADING_SHOW_DELAY_MS) return false
    return elapsedMs < maxOf(finishedAtMs, LOADING_SHOW_DELAY_MS + LOADING_MIN_VISIBLE_MS)
}

/**
 * How long to keep the indicator on screen after work that started at 0 ended at
 * [finishedAtMs]: zero if it never appeared, otherwise whatever is left of the minimum.
 */
fun loadingHoldMs(finishedAtMs: Long): Long =
    if (finishedAtMs < LOADING_SHOW_DELAY_MS) {
        0
    } else {
        (LOADING_SHOW_DELAY_MS + LOADING_MIN_VISIBLE_MS - finishedAtMs).coerceAtLeast(0)
    }
