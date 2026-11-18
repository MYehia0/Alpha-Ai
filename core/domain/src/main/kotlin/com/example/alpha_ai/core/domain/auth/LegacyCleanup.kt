package com.example.alpha_ai.core.domain.auth

/** Removes what the v1 app stored on the phone, including its saved password (FR-022). */
interface LegacyCleanup {
    /** Runs the clean-up the first time it is called on this phone; never throws. */
    suspend fun runOnce()
}
