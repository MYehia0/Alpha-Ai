package com.example.alpha_ai.core.data.auth

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyAuthCleanupTest {

    private class FakeStorage : LegacyAuthStorage {
        var prefsDeletes = 0
        var keyDeletes = 0
        var failPrefs = false

        override fun deleteSecurePreferences() {
            prefsDeletes++
            if (failPrefs) throw IllegalStateException("disk")
        }

        override fun deleteMasterKey() {
            keyDeletes++
        }
    }

    private val preferences = InMemoryAppPreferencesStore()
    private val storage = FakeStorage()
    private val cleanup = LegacyAuthCleanup(preferences, storage)

    @Test
    fun `deletes both and sets the flag`() = runTest {
        cleanup.runOnce()
        assertEquals(1, storage.prefsDeletes)
        assertEquals(1, storage.keyDeletes)
        assertTrue(preferences.legacyCleanupDone.first())
    }

    @Test
    fun `runs only once`() = runTest {
        cleanup.runOnce()
        cleanup.runOnce()
        assertEquals(1, storage.prefsDeletes)
    }

    @Test
    fun `a failing step still sets the flag and does not throw`() = runTest {
        storage.failPrefs = true
        cleanup.runOnce()
        assertEquals(1, storage.keyDeletes)
        assertTrue(preferences.legacyCleanupDone.first())
    }

    @Test
    fun `a failing store does not throw`() = runTest {
        preferences.failure = IllegalStateException("disk")
        cleanup.runOnce()
    }
}
