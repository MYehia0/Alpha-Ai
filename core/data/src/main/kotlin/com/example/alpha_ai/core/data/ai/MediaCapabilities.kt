package com.example.alpha_ai.core.data.ai

import com.example.alpha_ai.core.common.DefaultDispatcher
import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.engine.ModelInvoker
import com.example.alpha_ai.core.data.ai.fallback.FallbackRunner
import com.example.alpha_ai.core.data.ai.prompt.Prompts
import com.example.alpha_ai.core.data.ai.validate.InputCheck
import com.example.alpha_ai.core.data.ai.validate.InputValidator
import com.example.alpha_ai.core.data.ai.validate.OutputValidator
import com.example.alpha_ai.core.data.ai.validate.ScriptAnalyzer
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.domain.ai.DetectedLanguage
import com.example.alpha_ai.core.domain.ai.ImageInput
import com.example.alpha_ai.core.domain.ai.ImageTextExtractor
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.domain.ai.LanguageDetector
import com.example.alpha_ai.core.domain.ai.SpeechTranscriber
import com.example.alpha_ai.core.network.ChatReply
import com.example.alpha_ai.core.network.ContentPart
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.Base64
import javax.inject.Inject

internal class DefaultImageTextExtractor @Inject constructor(
    private val runner: FallbackRunner,
    private val invoker: ModelInvoker,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageTextExtractor {

    override suspend fun extractText(image: ImageInput): AiResult<String> {
        val check = InputValidator.image(image)
        if (check is InputCheck.Rejected) return check.toFailure()

        val part = withContext(defaultDispatcher) {
            ContentPart.Image(Base64.getEncoder().encodeToString(image.bytes), image.mimeType)
        }
        return runner.run(Capability.IMAGE_TEXT) { choice ->
            invoker.chat(
                choice = choice,
                systemPrompt = Prompts.imageText,
                content = listOf(part),
                maxTokens = MEDIA_MAX_TOKENS,
                temperature = 0.0,
            ).checked(expected = null, systemPrompt = Prompts.imageText)
        }
    }
}

internal class DefaultSpeechTranscriber @Inject constructor(
    private val runner: FallbackRunner,
    private val invoker: ModelInvoker,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : SpeechTranscriber {

    override suspend fun transcribe(audio: AudioClip, language: Language?): AiResult<String> {
        val check = InputValidator.speech(audio)
        if (check is InputCheck.Rejected) return check.toFailure()

        val prompt = Prompts.speech(language)
        val part = withContext(defaultDispatcher) {
            ContentPart.Audio(Base64.getEncoder().encodeToString(audio.bytes), audioFormat(audio.mimeType))
        }
        return runner.run(Capability.SPEECH) { choice ->
            invoker.chat(
                choice = choice,
                systemPrompt = prompt,
                content = listOf(part),
                maxTokens = MEDIA_MAX_TOKENS,
                temperature = 0.0,
            ).checked(expected = language, systemPrompt = prompt)
        }
    }

    /** The short format name chat models expect, from a MIME type in `AiLimits`. */
    private fun audioFormat(mimeType: String): String = when (mimeType) {
        "audio/mpeg" -> "mp3"
        "audio/mp4" -> "m4a"
        else -> mimeType.substringAfter('/')
    }
}

/** Language detection by script analysis: on the device, instant, and free of quota. */
internal class ScriptLanguageDetector @Inject constructor() : LanguageDetector {

    override suspend fun detect(text: String): AiResult<DetectedLanguage> =
        when (val check = InputValidator.detection(text)) {
            is InputCheck.Rejected -> check.toFailure()
            is InputCheck.Accepted -> AiResult.Success(ScriptAnalyzer.detect(text))
        }
}

private const val MEDIA_MAX_TOKENS = 4_096

private fun ProviderResult<ChatReply>.checked(expected: Language?, systemPrompt: String): ProviderResult<String> =
    when (this) {
        is ProviderResult.Failure -> this
        is ProviderResult.Success ->
            OutputValidator.media(value.text, expected, systemPrompt, value.truncated)
                ?.let { ProviderResult.Success(it) }
                ?: ProviderResult.Failure(ProviderFailure.MalformedResponse)
    }
