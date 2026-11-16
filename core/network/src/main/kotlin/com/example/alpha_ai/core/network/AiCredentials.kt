package com.example.alpha_ai.core.network

/**
 * Supplies the access token for a provider. The app module implements it, so a later feature
 * can supply a per-user key without changing this module.
 */
interface AiCredentials {
    /** The token for [provider], or `null` when none is configured. */
    fun tokenFor(provider: AiProvider): String?
}
