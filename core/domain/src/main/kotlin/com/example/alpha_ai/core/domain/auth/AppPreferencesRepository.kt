package com.example.alpha_ai.core.domain.auth

/** App-wide, per-phone preferences that sign-out does not clear. */
interface AppPreferencesRepository {
    /** A read failure returns `false`; never throws. */
    suspend fun isIntroductionCompleted(): Boolean

    /** A write failure is logged; never throws. */
    suspend fun setIntroductionCompleted()
}
