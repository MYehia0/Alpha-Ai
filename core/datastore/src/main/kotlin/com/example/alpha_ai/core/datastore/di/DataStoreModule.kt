package com.example.alpha_ai.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.alpha_ai.core.common.IoDispatcher
import com.example.alpha_ai.core.datastore.AppPreferencesStore
import com.example.alpha_ai.core.datastore.DataStoreAppPreferencesStore
import com.example.alpha_ai.core.datastore.DataStoreSessionStore
import com.example.alpha_ai.core.datastore.KeystoreSessionCipher
import com.example.alpha_ai.core.datastore.SessionCipher
import com.example.alpha_ai.core.datastore.SessionData
import com.example.alpha_ai.core.datastore.SessionSerializer
import com.example.alpha_ai.core.datastore.SessionStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {

    @Binds
    abstract fun bindSessionCipher(cipher: KeystoreSessionCipher): SessionCipher

    @Binds
    @Singleton
    abstract fun bindSessionStore(store: DataStoreSessionStore): SessionStore

    @Binds
    @Singleton
    abstract fun bindAppPreferencesStore(store: DataStoreAppPreferencesStore): AppPreferencesStore

    companion object {
        // One DataStore per file for the whole process; a second instance on the same file throws.
        @Provides
        @Singleton
        fun provideSessionDataStore(
            @ApplicationContext context: Context,
            cipher: SessionCipher,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<SessionData> = DataStoreFactory.create(
            serializer = SessionSerializer(cipher),
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.dataStoreFile("session.enc") },
        )

        @Provides
        @Singleton
        fun providePreferencesDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile("app_preferences") },
        )
    }
}
