package com.example.alpha_ai.core.domain.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Limits each [CooldownAction] to once per [WINDOW] for the life of the process (FR-014). Time
 * is measured with a monotonic [TimeSource], so changing the phone's clock has no effect.
 */
@Singleton
class ActionCooldown @Inject constructor(
    private val timeSource: TimeSource.WithComparableMarks,
) {
    private val lastStarted = mutableMapOf<CooldownAction, ComparableTimeMark>()

    /** Records that [action] happened now. */
    @Synchronized
    fun start(action: CooldownAction) {
        lastStarted[action] = timeSource.markNow()
    }

    /** How long until [action] is allowed again; [Duration.ZERO] when it is allowed now. */
    @Synchronized
    fun remaining(action: CooldownAction): Duration {
        val started = lastStarted[action] ?: return Duration.ZERO
        val left = WINDOW - started.elapsedNow()
        return if (left.isPositive()) left else Duration.ZERO
    }

    companion object {
        val WINDOW: Duration = 60.seconds
    }
}
