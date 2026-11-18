package com.example.alpha_ai.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Plain, per-phone preferences (`app_preferences`). Not sensitive; not cleared by sign-out. */
interface AppPreferencesStore {
    val introductionCompleted: Flow<Boolean>

    val legacyCleanupDone: Flow<Boolean>

    suspend fun setIntroductionCompleted()

    suspend fun setLegacyCleanupDone()
}

internal class DataStoreAppPreferencesStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AppPreferencesStore {

    override val introductionCompleted: Flow<Boolean> =
        dataStore.data.map { it[INTRODUCTION_COMPLETED] ?: false }

    override val legacyCleanupDone: Flow<Boolean> =
        dataStore.data.map { it[LEGACY_CLEANUP_DONE] ?: false }

    override suspend fun setIntroductionCompleted() {
        dataStore.edit { it[INTRODUCTION_COMPLETED] = true }
    }

    override suspend fun setLegacyCleanupDone() {
        dataStore.edit { it[LEGACY_CLEANUP_DONE] = true }
    }

    private companion object {
        val INTRODUCTION_COMPLETED = booleanPreferencesKey("introduction_completed")
        val LEGACY_CLEANUP_DONE = booleanPreferencesKey("legacy_cleanup_done")
    }
}
