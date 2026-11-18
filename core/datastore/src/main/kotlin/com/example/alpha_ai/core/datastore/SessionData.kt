package com.example.alpha_ai.core.datastore

import kotlinx.serialization.Serializable

/**
 * What the app itself keeps about the signed-in user, stored encrypted in `session.enc`. It never
 * holds a password or a token (FR-022). [userId] ties the cache to one account; a cache for
 * another account is ignored.
 */
@Serializable
data class SessionData(
    val userId: String? = null,
    val name: String? = null,
    val email: String? = null,
    val emailConfirmed: Boolean = false,
    /** The name is not yet saved to the user's profile document. */
    val profileSyncPending: Boolean = false,
    /** The one-time "confirmation link sent" message is still to be shown. */
    val confirmationNoticePending: Boolean = false,
) {
    companion object {
        val EMPTY = SessionData()
    }
}
