package com.example.alpha_ai.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppPreferencesStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.store(file: File, job: Job): DataStoreAppPreferencesStore =
        DataStoreAppPreferencesStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(StandardTestDispatcher(testScheduler) + job),
                produceFile = { file },
            ),
        )

    private fun newFile() = File(folder.root, "app_preferences.preferences_pb")

    @Test
    fun `both flags default to false`() = runTest {
        val job = Job()
        val store = store(newFile(), job)
        assertFalse(store.introductionCompleted.first())
        assertFalse(store.legacyCleanupDone.first())
        job.cancel()
    }

    @Test
    fun `flags round trip independently`() = runTest {
        val job = Job()
        val store = store(newFile(), job)
        store.setIntroductionCompleted()
        assertTrue(store.introductionCompleted.first())
        assertFalse(store.legacyCleanupDone.first())
        store.setLegacyCleanupDone()
        assertTrue(store.legacyCleanupDone.first())
        job.cancel()
    }

    @Test
    fun `values persist for a new store on the same file`() = runTest {
        val file = newFile()
        val first = Job()
        store(file, first).setIntroductionCompleted()
        first.cancel()
        testScheduler.advanceUntilIdle()

        val second = Job()
        assertTrue(store(file, second).introductionCompleted.first())
        second.cancel()
    }
}
