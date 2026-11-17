package com.example.alpha_ai.core.data.ai.engine

import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.fallback.FallbackRunner
import com.example.alpha_ai.core.data.ai.fallback.StreamingFallbackRunner
import com.example.alpha_ai.core.data.ai.prompt.Prompts
import com.example.alpha_ai.core.data.ai.validate.OutputValidator
import com.example.alpha_ai.core.data.ai.validate.TextTask
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.Language
import com.example.alpha_ai.core.domain.ai.TextStreamEvent
import com.example.alpha_ai.core.network.ContentPart
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The shared path of the three text capabilities: send the instruction and the user's text
 * as separate messages, check the reply, and fall back when it is unusable.
 */
internal class TextTaskRunner @Inject constructor(
    private val runner: FallbackRunner,
    private val streamingRunner: StreamingFallbackRunner,
    private val invoker: ModelInvoker,
) {

    /**
     * @param input the user's text, already validated
     * @param expected the language the result must be in
     * @param reminder the fixed reminder of the task, sent after the user's text
     */
    suspend fun run(
        capability: Capability,
        task: TextTask,
        input: String,
        systemPrompt: String,
        expected: Language,
        reminder: String,
    ): AiResult<String> =
        runner.run(capability) { choice ->
            val reply = invoker.chat(
                choice = choice,
                systemPrompt = systemPrompt,
                // The user's text is the whole of its own message, never part of the instruction.
                content = listOf(ContentPart.Text(Prompts.wrap(input, reminder))),
                maxTokens = maxTokensFor(task, input),
                temperature = temperatureFor(task),
            )
            when (reply) {
                is ProviderResult.Failure -> reply
                is ProviderResult.Success ->
                    OutputValidator.text(
                        task = task,
                        input = input,
                        raw = reply.value.text,
                        expected = expected,
                        systemPrompt = systemPrompt,
                        truncated = reply.value.truncated,
                    )?.let { ProviderResult.Success(it) }
                        ?: ProviderResult.Failure(ProviderFailure.MalformedResponse)
            }
        }

    /** The streaming form of [run]: the same request and checks, delivered as it is generated. */
    fun stream(
        capability: Capability,
        task: TextTask,
        input: String,
        systemPrompt: String,
        expected: Language,
        reminder: String,
    ): Flow<TextStreamEvent> =
        streamingRunner.run(
            capability = capability,
            attempt = { choice ->
                invoker.chatStream(
                    choice = choice,
                    systemPrompt = systemPrompt,
                    content = listOf(ContentPart.Text(Prompts.wrap(input, reminder))),
                    maxTokens = maxTokensFor(task, input),
                    temperature = temperatureFor(task),
                )
            },
            check = { raw, truncated ->
                OutputValidator.text(task, input, raw, expected, systemPrompt, truncated)
            },
        )

    private fun temperatureFor(task: TextTask): Double =
        if (task == TextTask.SUMMARY) SUMMARY_TEMPERATURE else 0.0

    private fun maxTokensFor(task: TextTask, input: String): Int {
        // A token is rarely shorter than one character, so the character count is a safe
        // ceiling for a result about as long as the input. The margin leaves room for models
        // that reason before answering: on 2026-11-17 one used about 500 tokens of reasoning
        // for a 100-character translation, which a 512 margin cut off. Free models cost
        // nothing per token, so the margin is generous.
        val expectedChars = if (task == TextTask.SUMMARY) input.length / 2 else input.length
        return (expectedChars + TOKEN_MARGIN).coerceAtMost(MAX_TOKENS_CEILING)
    }

    private companion object {
        const val SUMMARY_TEMPERATURE = 0.2
        const val TOKEN_MARGIN = 2_048
        const val MAX_TOKENS_CEILING = 12_000
    }
}
