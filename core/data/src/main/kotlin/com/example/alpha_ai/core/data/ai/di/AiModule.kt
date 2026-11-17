package com.example.alpha_ai.core.data.ai.di

import com.example.alpha_ai.core.common.DefaultDispatcher
import com.example.alpha_ai.core.common.IoDispatcher
import com.example.alpha_ai.core.data.ai.DefaultGrammarCorrector
import com.example.alpha_ai.core.data.ai.DefaultImageTextExtractor
import com.example.alpha_ai.core.data.ai.DefaultSpeechTranscriber
import com.example.alpha_ai.core.data.ai.DefaultSummarizer
import com.example.alpha_ai.core.data.ai.DefaultTranslator
import com.example.alpha_ai.core.data.ai.ScriptLanguageDetector
import com.example.alpha_ai.core.data.ai.config.HuggingFaceModels
import com.example.alpha_ai.core.data.ai.config.ModelChains
import com.example.alpha_ai.core.data.ai.config.ModelConfigResolver
import com.example.alpha_ai.core.data.ai.config.ModelConfigSource
import com.example.alpha_ai.core.data.ai.config.NoOverrides
import com.example.alpha_ai.core.data.ai.config.OpenRouterModels
import com.example.alpha_ai.core.data.ai.fallback.AiClock
import com.example.alpha_ai.core.domain.ai.GrammarCorrector
import com.example.alpha_ai.core.domain.ai.ImageTextExtractor
import com.example.alpha_ai.core.domain.ai.LanguageDetector
import com.example.alpha_ai.core.domain.ai.SpeechTranscriber
import com.example.alpha_ai.core.domain.ai.Summarizer
import com.example.alpha_ai.core.domain.ai.Translator
import com.example.alpha_ai.core.network.di.NetworkBindingNames
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiModule {

    @Binds
    abstract fun bindGrammarCorrector(impl: DefaultGrammarCorrector): GrammarCorrector

    @Binds
    abstract fun bindTranslator(impl: DefaultTranslator): Translator

    @Binds
    abstract fun bindSummarizer(impl: DefaultSummarizer): Summarizer

    @Binds
    abstract fun bindSpeechTranscriber(impl: DefaultSpeechTranscriber): SpeechTranscriber

    @Binds
    abstract fun bindImageTextExtractor(impl: DefaultImageTextExtractor): ImageTextExtractor

    @Binds
    abstract fun bindLanguageDetector(impl: ScriptLanguageDetector): LanguageDetector

    @Binds
    abstract fun bindModelChains(impl: ModelConfigResolver): ModelChains

    @Binds
    abstract fun bindModelConfigSource(impl: NoOverrides): ModelConfigSource

    companion object {

        @Provides
        @Named(NetworkBindingNames.OPENROUTER_BASE_URL)
        fun provideOpenRouterBaseUrl(): String = OpenRouterModels.baseUrl

        @Provides
        @Named(NetworkBindingNames.HUGGING_FACE_BASE_URL)
        fun provideHuggingFaceBaseUrl(): String = HuggingFaceModels.baseUrl

        @Provides
        fun provideClock(): AiClock = AiClock { System.currentTimeMillis() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
