package com.example.alpha_ai.di

import com.example.alpha_ai.BuildConfig
import com.example.alpha_ai.core.network.AiCredentials
import com.example.alpha_ai.core.network.AiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Supplies the AI provider keys to the network layer. The keys come from `local.properties`
 * or CI secrets through `BuildConfig`; they are never in source control. A blank key is
 * reported as absent, which marks its provider as unavailable.
 */
@Module
@InstallIn(SingletonComponent::class)
object AiCredentialsModule {

    @Provides
    @Singleton
    fun provideAiCredentials(): AiCredentials = object : AiCredentials {
        override fun tokenFor(provider: AiProvider): String? = when (provider) {
            AiProvider.OPENROUTER -> BuildConfig.OPENROUTER_API_KEY
            AiProvider.HUGGING_FACE -> BuildConfig.HF_API_TOKEN
        }.takeIf { it.isNotBlank() }
    }
}
