package com.example.alpha_ai.core.ui.ai

import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class AiFailureMessagesTest {

    private val expectedKeys = setOf(
        "ai_failure_no_connection",
        "ai_failure_timed_out",
        "ai_failure_service_busy",
        "ai_failure_service_unavailable",
        "ai_failure_invalid_response",
        "ai_failure_unknown",
        "ai_input_empty",
        "ai_input_too_short",
        "ai_input_too_long",
        "ai_input_too_large",
        "ai_input_unsupported_language",
        "ai_input_unsupported_format",
    )

    /** Words that would expose a provider, a model, or a technical code to the user. */
    private val forbidden = listOf(
        "OpenRouter", "Hugging Face", "HuggingFace", "HTTP", "gemma", "nemotron", "whisper", ":free", "API",
    )

    private fun strings(folder: String): Map<String, String> {
        val file = File("src/main/res/$folder/strings_ai_failures.xml")
        assertTrue("missing ${file.path}", file.isFile)
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file).getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    @Test
    fun `every failure and every input reason has its own message`() {
        val failures = listOf(
            AiFailure.NoConnection,
            AiFailure.TimedOut,
            AiFailure.ServiceBusy(retryAfterSeconds = 30),
            AiFailure.ServiceUnavailable,
            AiFailure.InvalidResponse,
            AiFailure.Unknown,
        ) + InvalidInputReason.entries.map { AiFailure.InvalidInput(it) }

        val resources = failures.map { it.messageRes() }

        assertEquals(expectedKeys.size, failures.size)
        assertTrue(resources.none { it == 0 })
        assertEquals(failures.size, resources.toSet().size)
    }

    @Test
    fun `a busy failure uses one message whatever the wait`() {
        assertEquals(AiFailure.ServiceBusy().messageRes(), AiFailure.ServiceBusy(90).messageRes())
    }

    @Test
    fun `english and arabic files hold the same twelve keys`() {
        assertEquals(expectedKeys, strings("values").keys)
        assertEquals(expectedKeys, strings("values-ar").keys)
    }

    @Test
    fun `no message is empty or exposes a number a provider or a model`() {
        for (folder in listOf("values", "values-ar")) {
            for ((key, value) in strings(folder)) {
                assertTrue("$folder/$key is blank", value.isNotBlank())
                assertFalse("$folder/$key has a digit", value.any { it.isDigit() })
                for (word in forbidden) {
                    assertFalse("$folder/$key names $word", value.contains(word, ignoreCase = true))
                }
            }
        }
    }

    @Test
    fun `arabic messages are written in arabic`() {
        for ((key, value) in strings("values-ar")) {
            assertTrue("values-ar/$key", value.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC })
        }
    }
}
