package com.example.alpha_ai.core.data.auth

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DataStoreAppPreferencesRepositoryTest {

    private val store = InMemoryAppPreferencesStore()
    private val repository = DataStoreAppPreferencesRepository(store)

    @Test
    fun `introduction is not completed by default`() = runTest {
        assertFalse(repository.isIntroductionCompleted())
    }

    @Test
    fun `completing the introduction is read back`() = runTest {
        repository.setIntroductionCompleted()
        assertTrue(repository.isIntroductionCompleted())
    }

    @Test
    fun `a read failure returns false instead of throwing`() = runTest {
        store.failure = IOException("disk")
        assertFalse(repository.isIntroductionCompleted())
    }

    @Test
    fun `a write failure does not throw`() = runTest {
        store.failure = IOException("disk")
        repository.setIntroductionCompleted()
    }
}
