package com.example.alpha_ai.core.data.ai

import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.engine.TextTaskRunner
import com.example.alpha_ai.core.data.ai.engine.toTerminalEvent
import com.example.alpha_ai.core.data.ai.prompt.Prompts
import com.example.alpha_ai.core.data.ai.validate.InputCheck
import com.example.alpha_ai.core.data.ai.validate.InputValidator
import com.example.alpha_ai.core.data.ai.validate.TextTask
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.GrammarCorrector
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.domain.ai.Summarizer
import com.example.alpha_ai.core.domain.ai.TextStreamEvent
import com.example.alpha_ai.core.domain.ai.Translator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class DefaultGrammarCorrector @Inject constructor(
    private val tasks: TextTaskRunner,
) : GrammarCorrector {

    override suspend fun correct(text: String, language: Language?): AiResult<String> =
        when (val check = InputValidator.grammar(text, language)) {
            is InputCheck.Rejected -> check.toFailure()
            is InputCheck.Accepted -> tasks.run(
                capability = Capability.GRAMMAR,
                task = TextTask.CORRECTION,
                input = text.trim(),
                systemPrompt = Prompts.grammar(check.value),
                expected = check.value,
            )
        }

    override fun correctStreaming(text: String, language: Language?): Flow<TextStreamEvent> =
        flow { emit(correct(text, language).toTerminalEvent()) }
}

internal class DefaultTranslator @Inject constructor(
    private val tasks: TextTaskRunner,
) : Translator {

    override suspend fun translate(text: String, from: Language, to: Language): AiResult<String> =
        when (val check = InputValidator.translation(text, from, to)) {
            is InputCheck.Rejected -> check.toFailure()
            is InputCheck.Accepted -> tasks.run(
                capability = Capability.TRANSLATION,
                task = TextTask.TRANSLATION,
                input = text.trim(),
                systemPrompt = Prompts.translation(from, to),
                expected = to,
            )
        }

    override fun translateStreaming(text: String, from: Language, to: Language): Flow<TextStreamEvent> =
        flow { emit(translate(text, from, to).toTerminalEvent()) }
}

internal class DefaultSummarizer @Inject constructor(
    private val tasks: TextTaskRunner,
) : Summarizer {

    override suspend fun summarize(text: String, language: Language?): AiResult<String> =
        when (val check = InputValidator.summary(text, language)) {
            is InputCheck.Rejected -> check.toFailure()
            is InputCheck.Accepted -> tasks.run(
                capability = Capability.SUMMARIZATION,
                task = TextTask.SUMMARY,
                input = text.trim(),
                systemPrompt = Prompts.summary(check.value),
                expected = check.value,
            )
        }

    override fun summarizeStreaming(text: String, language: Language?): Flow<TextStreamEvent> =
        flow { emit(summarize(text, language).toTerminalEvent()) }
}

internal fun InputCheck.Rejected.toFailure(): AiResult.Failure =
    AiResult.Failure(AiFailure.InvalidInput(reason))
