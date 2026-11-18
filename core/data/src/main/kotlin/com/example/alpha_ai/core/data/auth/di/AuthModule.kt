package com.example.alpha_ai.core.data.auth.di

import com.example.alpha_ai.core.common.ApplicationScope
import com.example.alpha_ai.core.common.IoDispatcher
import com.example.alpha_ai.core.data.auth.AndroidLegacyAuthStorage
import com.example.alpha_ai.core.data.auth.DataStoreAppPreferencesRepository
import com.example.alpha_ai.core.data.auth.FirebaseAuthDataSource
import com.example.alpha_ai.core.data.auth.FirebaseAuthDataSourceImpl
import com.example.alpha_ai.core.data.auth.FirebaseAuthRepository
import com.example.alpha_ai.core.data.auth.LegacyAuthCleanup
import com.example.alpha_ai.core.data.auth.LegacyAuthStorage
import com.example.alpha_ai.core.domain.auth.AppPreferencesRepository
import com.example.alpha_ai.core.domain.auth.AuthRepository
import com.example.alpha_ai.core.domain.auth.LegacyCleanup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthModule {

    @Binds
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindAppPreferencesRepository(
        impl: DataStoreAppPreferencesRepository,
    ): AppPreferencesRepository

    @Binds
    abstract fun bindFirebaseAuthDataSource(impl: FirebaseAuthDataSourceImpl): FirebaseAuthDataSource

    @Binds
    abstract fun bindLegacyCleanup(impl: LegacyAuthCleanup): LegacyCleanup

    @Binds
    abstract fun bindLegacyAuthStorage(impl: AndroidLegacyAuthStorage): LegacyAuthStorage

    companion object {
        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

        /** Monotonic, so changing the phone's clock cannot shorten a cooldown. */
        @Provides
        fun provideTimeSource(): TimeSource.WithComparableMarks = TimeSource.Monotonic

        /** Lives as long as the process; a failing child does not cancel the others. */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
            CoroutineScope(SupervisorJob() + io)
    }
}
