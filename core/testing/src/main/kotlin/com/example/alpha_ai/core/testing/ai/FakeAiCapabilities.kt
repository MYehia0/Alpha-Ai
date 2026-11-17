package com.example.alpha_ai.core.testing.ai

import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.domain.ai.DetectedLanguage
import com.example.alpha_ai.core.domain.ai.GrammarCorrector
import com.example.alpha_ai.core.domain.ai.ImageInput
import com.example.alpha_ai.core.domain.ai.ImageTextExtractor
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.domain.ai.LanguageDetector
import com.example.alpha_ai.core.domain.ai.SpeechTranscriber
import com.example.alpha_ai.core.domain.ai.Summarizer
import com.example.alpha_ai.core.domain.ai.TextStreamEvent
import com.example.alpha_ai.core.domain.ai.Translator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Test doubles for the AI capabilities, for ViewModel and use-case tests in feature modules.
// Each one returns the `result` it is given, records the calls it receives in `calls`, and
// streams the events in `streamEvents` when they are set (otherwise one terminal event made
// from `result`). No network or model is involved.

class FakeGrammarCorrector(
    var result: AiResult<String> = AiResult.Success(""),
) : GrammarCorrector {
    data class Call(val text: String, val language: Language?)

    val calls = mutableListOf<Call>()
    var streamEvents: List<TextStreamEvent>? = null

    override suspend fun correct(text: String, language: Language?): AiResult<String> {
        calls += Call(text, language)
        return result
    }

    override fun correctStreaming(text: String, language: Language?): Flow<TextStreamEvent> = flow {
        calls += Call(text, language)
        emitEvents(streamEvents, result)
    }
}

class FakeTranslator(
    var result: AiResult<String> = AiResult.Success(""),
) : Translator {
    data class Call(val text: String, val from: Language, val to: Language)

    val calls = mutableListOf<Call>()
    var streamEvents: List<TextStreamEvent>? = null

    override suspend fun translate(text: String, from: Language, to: Language): AiResult<String> {
        calls += Call(text, from, to)
        return result
    }

    override fun translateStreaming(text: String, from: Language, to: Language): Flow<TextStreamEvent> = flow {
        calls += Call(text, from, to)
        emitEvents(streamEvents, result)
    }
}

class FakeSummarizer(
    var result: AiResult<String> = AiResult.Success(""),
) : Summarizer {
    data class Call(val text: String, val language: Language?)

    val calls = mutableListOf<Call>()
    var streamEvents: List<TextStreamEvent>? = null

    override suspend fun summarize(text: String, language: Language?): AiResult<String> {
        calls += Call(text, language)
        return result
    }

    override fun summarizeStreaming(text: String, language: Language?): Flow<TextStreamEvent> = flow {
        calls += Call(text, language)
        emitEvents(streamEvents, result)
    }
}

class FakeSpeechTranscriber(
    var result: AiResult<String> = AiResult.Success(""),
) : SpeechTranscriber {
    data class Call(val audio: AudioClip, val language: Language?)

    val calls = mutableListOf<Call>()

    override suspend fun transcribe(audio: AudioClip, language: Language?): AiResult<String> {
        calls += Call(audio, language)
        return result
    }
}

class FakeImageTextExtractor(
    var result: AiResult<String> = AiResult.Success(""),
) : ImageTextExtractor {
    val calls = mutableListOf<ImageInput>()

    override suspend fun extractText(image: ImageInput): AiResult<String> {
        calls += image
        return result
    }
}

class FakeLanguageDetector(
    var result: AiResult<DetectedLanguage> = AiResult.Success(DetectedLanguage.ENGLISH),
) : LanguageDetector {
    val calls = mutableListOf<String>()

    override suspend fun detect(text: String): AiResult<DetectedLanguage> {
        calls += text
        return result
    }
}

private suspend fun kotlinx.coroutines.flow.FlowCollector<TextStreamEvent>.emitEvents(
    events: List<TextStreamEvent>?,
    result: AiResult<String>,
) {
    if (events != null) {
        events.forEach { emit(it) }
        return
    }
    emit(
        when (result) {
            is AiResult.Success -> TextStreamEvent.Completed(result.value)
            is AiResult.Failure -> TextStreamEvent.Failed(result.failure)
        },
    )
}
