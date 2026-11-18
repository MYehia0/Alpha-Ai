package com.example.alpha_ai.core.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The encrypted session cache (`files/datastore/session.enc`). */
interface SessionStore {
    val data: Flow<SessionData>

    suspend fun update(transform: (SessionData) -> SessionData)

    /** Writes the empty value; what sign-out calls (FR-016). */
    suspend fun clear()
}

internal class DataStoreSessionStore @Inject constructor(
    private val dataStore: DataStore<SessionData>,
) : SessionStore {
    override val data: Flow<SessionData> = dataStore.data

    override suspend fun update(transform: (SessionData) -> SessionData) {
        dataStore.updateData(transform)
    }

    override suspend fun clear() {
        dataStore.updateData { SessionData.EMPTY }
    }
}
