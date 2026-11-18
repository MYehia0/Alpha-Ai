package com.example.alpha_ai.core.data.auth

/**
 * The Firestore document `users/{uid}`, in the shape v1 already writes, so old and new accounts
 * read the same way. Never leaves the data layer. The no-argument constructor (all defaults) is
 * what Firestore needs to read it back.
 */
internal data class UserProfileDto(
    val uid: String = "",
    val uName: String? = null,
    val uEmail: String? = null,
)
