package com.example.alpha_ai.core.data.ai.validate

import com.example.alpha_ai.core.data.ai.prompt.Prompts
import com.example.alpha_ai.core.domain.ai.Language

/** The kind of text result, which decides the checks applied to it. */
internal enum class TextTask { CORRECTION, TRANSLATION, SUMMARY }

/**
 * Model output is untrusted. It is cleaned of the wrappers small models add, then checked
 * against the shape the capability expects. A `null` result means the output is unusable and
 * the next model should be tried.
 */
internal object OutputValidator {

    /** Below this input length a correction's length is not compared with the input's. */
    private const val LENGTH_RULE_MIN_INPUT = 20

    private val thinkBlock = Regex("^\\s*<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
    private val contentTags = Regex("</?content>")
    private val codeFence = Regex("^```[\\w-]*\\s*\\n?(.*?)\\n?```$", RegexOption.DOT_MATCHES_ALL)
    private val quotePairs = listOf('"' to '"', '\'' to '\'', '“' to '”', '«' to '»', '`' to '`')

    /**
     * The usable text of a correction, translation, or summary, or `null` when it fails a check.
     *
     * @param input the user's text, for the length rules
     * @param expected the language the result must be in
     * @param systemPrompt the instruction sent, to detect a model echoing it back
     * @param truncated whether the provider cut the reply off at the token limit
     */
    fun text(
        task: TextTask,
        input: String,
        raw: String,
        expected: Language,
        systemPrompt: String,
        truncated: Boolean,
    ): String? {
        if (truncated) return null
        val output = clean(raw)
        if (output.isEmpty()) return null
        if (repeatsInstruction(output, systemPrompt)) return null
        if (!ScriptAnalyzer.isAcceptedAs(output, expected)) return null

        val inputLength = input.trim().length
        val lengthOk = when (task) {
            TextTask.CORRECTION ->
                inputLength < LENGTH_RULE_MIN_INPUT ||
                    (output.length * 2 >= inputLength && output.length <= inputLength * 2)
            TextTask.SUMMARY -> output.length < inputLength
            TextTask.TRANSLATION -> true
        }
        return output.takeIf { lengthOk }
    }

    /**
     * The usable text of a transcription or an image reading, or `null` when it fails a check.
     * An empty string is a valid result: the clip had no speech or the image had no text.
     *
     * @param expected the language the result must be in, when the caller stated one
     */
    fun media(raw: String, expected: Language?, systemPrompt: String, truncated: Boolean): String? {
        if (truncated) return null
        val output = clean(raw)
        if (output.isEmpty() || output == Prompts.NONE_TOKEN) return ""
        if (repeatsInstruction(output, systemPrompt)) return null
        if (expected != null && !ScriptAnalyzer.isAcceptedAs(output, expected)) return null
        return output
    }

    /** Removes a leading reasoning block, content tags, a code fence, and one pair of quotes. */
    fun clean(raw: String): String {
        var text = raw.replace(thinkBlock, "").trim()
        codeFence.matchEntire(text)?.let { text = it.groupValues[1].trim() }
        text = text.replace(contentTags, "").trim()
        for ((open, close) in quotePairs) {
            if (text.length >= 2 && text.first() == open && text.last() == close) {
                val inner = text.substring(1, text.length - 1)
                // Keep quotes that belong to the text, such as two quoted phrases.
                if (open !in inner && close !in inner) text = inner.trim()
                break
            }
        }
        return text
    }

    private fun repeatsInstruction(output: String, systemPrompt: String): Boolean {
        val opening = systemPrompt.take(INSTRUCTION_PROBE_LENGTH)
        return opening.length == INSTRUCTION_PROBE_LENGTH && output.contains(opening, ignoreCase = true)
    }

    private const val INSTRUCTION_PROBE_LENGTH = 40
}
