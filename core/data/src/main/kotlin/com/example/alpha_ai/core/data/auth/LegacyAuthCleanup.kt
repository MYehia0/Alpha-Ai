package com.example.alpha_ai.core.data.auth

import android.content.Context
import com.example.alpha_ai.core.datastore.AppPreferencesStore
import com.example.alpha_ai.core.domain.auth.LegacyCleanup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** What v1 left on the phone. Separate from [LegacyAuthCleanup] so it can be faked in tests. */
internal interface LegacyAuthStorage {
    /** Deletes v1's encrypted preferences, which hold the saved email and password. */
    fun deleteSecurePreferences()

    /** Deletes the Keystore key that encrypted them. */
    fun deleteMasterKey()
}

internal class AndroidLegacyAuthStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : LegacyAuthStorage {

    override fun deleteSecurePreferences() {
        context.deleteSharedPreferences(SECURE_PREFS)
    }

    override fun deleteMasterKey() {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) keyStore.deleteEntry(MASTER_KEY_ALIAS)
    }

    private companion object {
        const val SECURE_PREFS = "alpha_ai_secure_prefs"
        const val KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    }
}

/**
 * Erases v1's stored password once per phone (FR-022). The Firebase session is not touched, so a
 * v1 user stays signed in. A failure is logged and still counts as done: retrying on every start
 * would only repeat it.
 */
@Singleton
internal class LegacyAuthCleanup @Inject constructor(
    private val preferences: AppPreferencesStore,
    private val storage: LegacyAuthStorage,
) : LegacyCleanup {

    private val mutex = Mutex()

    override suspend fun runOnce() = mutex.withLock {
        try {
            if (preferences.legacyCleanupDone.first()) return@withLock
            step("preferences") { storage.deleteSecurePreferences() }
            step("master key") { storage.deleteMasterKey() }
            preferences.setLegacyCleanupDone()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w("Legacy clean-up bookkeeping failed (%s)", e.javaClass.simpleName)
        }
    }

    private inline fun step(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Timber.w("Legacy clean-up of %s failed (%s)", name, e.javaClass.simpleName)
        }
    }
}
