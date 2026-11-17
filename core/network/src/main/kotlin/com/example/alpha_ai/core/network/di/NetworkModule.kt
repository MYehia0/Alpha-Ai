package com.example.alpha_ai.core.network.di

import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ConnectivityChecker
import com.example.alpha_ai.core.network.HuggingFaceClient
import com.example.alpha_ai.core.network.OpenRouterClient
import com.example.alpha_ai.core.network.internal.AndroidConnectivityChecker
import com.example.alpha_ai.core.network.internal.DefaultHuggingFaceClient
import com.example.alpha_ai.core.network.internal.DefaultOpenRouterClient
import com.example.alpha_ai.core.network.internal.HuggingFaceService
import com.example.alpha_ai.core.network.internal.OpenRouterService
import com.example.alpha_ai.core.network.internal.baseOkHttpClient
import com.example.alpha_ai.core.network.internal.providerRetrofit
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

/** Names of the base URL strings this module needs. The data module provides them. */
object NetworkBindingNames {
    const val OPENROUTER_BASE_URL = "openRouterBaseUrl"
    const val HUGGING_FACE_BASE_URL = "huggingFaceBaseUrl"
}

/** The shared OkHttp client that provider clients are derived from. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class AiBaseClient

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {

    @Provides
    @Singleton
    @AiBaseClient
    fun provideBaseClient(): OkHttpClient = baseOkHttpClient()

    @Provides
    @Singleton
    fun provideOpenRouterService(
        @AiBaseClient baseClient: OkHttpClient,
        credentials: AiCredentials,
        @Named(NetworkBindingNames.OPENROUTER_BASE_URL) baseUrl: String,
    ): OpenRouterService =
        providerRetrofit(baseClient, AiProvider.OPENROUTER, credentials, baseUrl)
            .create(OpenRouterService::class.java)

    @Provides
    @Singleton
    fun provideHuggingFaceService(
        @AiBaseClient baseClient: OkHttpClient,
        credentials: AiCredentials,
        @Named(NetworkBindingNames.HUGGING_FACE_BASE_URL) baseUrl: String,
    ): HuggingFaceService =
        providerRetrofit(baseClient, AiProvider.HUGGING_FACE, credentials, baseUrl)
            .create(HuggingFaceService::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindingsModule {

    @Binds
    @Singleton
    abstract fun bindOpenRouterClient(impl: DefaultOpenRouterClient): OpenRouterClient

    @Binds
    @Singleton
    abstract fun bindHuggingFaceClient(impl: DefaultHuggingFaceClient): HuggingFaceClient

    @Binds
    @Singleton
    abstract fun bindConnectivityChecker(impl: AndroidConnectivityChecker): ConnectivityChecker
}
