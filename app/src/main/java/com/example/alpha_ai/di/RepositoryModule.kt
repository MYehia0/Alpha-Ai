package com.example.alpha_ai.di

import com.example.alpha_ai.data.local.PreferencesDataSource
import com.example.alpha_ai.data.remote.firebase.FirebaseAuthService
import com.example.alpha_ai.data.repository.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthService: FirebaseAuthService,
        preferencesDataSource: PreferencesDataSource
    ): AuthRepository {
        return AuthRepository(firebaseAuthService, preferencesDataSource)
    }
}