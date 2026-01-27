package com.example.alpha_ai.di

import com.example.alpha_ai.data.remote.firebase.FirebaseAuthService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

//    @Provides
//    @Singleton
//    fun provideFirebaseStorage(): FirebaseStorage {
//        return FirebaseStorage.getInstance()
//    }

    @Provides
    @Singleton
    fun provideFirebaseAuthService(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): FirebaseAuthService {
        return FirebaseAuthService(auth, firestore)
    }
}