package com.example.alpha_ai.core.data.ai.validate

import com.example.alpha_ai.core.data.ai.prompt.Prompts
import com.example.alpha_ai.core.domain.ai.AiLimits
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.domain.ai.DetectedLanguage
import com.example.alpha_ai.core.domain.ai.ImageInput
import com.example.alpha_ai.core.domain.ai.InvalidInputReason
import com.example.alpha_ai.core.domain.ai.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ARABIC = "ذهب الولد إلى المدرسة في الصباح"
private const val ENGLISH = "The boy went to school in the morning"

class ScriptAnalyzerTest {

    @Test
    fun `arabic text is detected as arabic`() {
        assertEquals(DetectedLanguage.ARABIC, ScriptAnalyzer.detect(ARABIC))
    }

    @Test
    fun `english text is detected as english`() {
        assertEquals(DetectedLanguage.ENGLISH, ScriptAnalyzer.detect(ENGLISH))
    }

    @Test
    fun `digits and punctuation are ignored when counting`() {
        assertEquals(DetectedLanguage.ENGLISH, ScriptAnalyzer.detect("Call 0100 200 300 !!! now"))
    }

    @Test
    fun `an even mix is unsupported`() {
        assertEquals(DetectedLanguage.UNSUPPORTED, ScriptAnalyzer.detect("abcde ابتثج"))
    }

    @Test
    fun `text with no letters is unsupported`() {
        assertEquals(DetectedLanguage.UNSUPPORTED, ScriptAnalyzer.detect("123 ... !?"))
    }

    @Test
    fun `another script is unsupported`() {
        assertEquals(DetectedLanguage.UNSUPPORTED, ScriptAnalyzer.detect("Привет, как дела"))
        assertEquals(DetectedLanguage.UNSUPPORTED, ScriptAnalyzer.detect("你好，世界"))
    }

    @Test
    fun `arabic text with forty percent english terms is accepted as arabic when stated`() {
        // 6 Arabic letters and 4 Latin letters.
        val mixed = "ابتثجح abcd"
        assertEquals(0.6, ScriptAnalyzer.share(mixed, Language.ARABIC), 0.0001)
        assertTrue(ScriptAnalyzer.isAcceptedAs(mixed, Language.ARABIC))
        assertTrue(ScriptAnalyzer.isAcceptedAs(mixed, Language.ENGLISH))
    }

    @Test
    fun `text is rejected for a stated language below thirty percent`() {
        // 2 Arabic letters and 8 Latin letters.
        assertEquals(false, ScriptAnalyzer.isAcceptedAs("اب abcdefgh", Language.ARABIC))
    }
}

class InputValidatorTest {

    private fun rejected(reason: InvalidInputReason) = InputCheck.Rejected(reason)

    @Test
    fun `blank text is empty for every text capability`() {
        assertEquals(rejected(InvalidInputReason.EMPTY), InputValidator.grammar("  \n ", null))
        assertEquals(rejected(InvalidInputReason.EMPTY), InputValidator.summary("", Language.ENGLISH))
        assertEquals(
            rejected(InvalidInputReason.EMPTY),
            InputValidator.translation(" ", Language.ENGLISH, Language.ARABIC),
        )
        assertEquals(rejected(InvalidInputReason.EMPTY), InputValidator.detection("   "))
    }

    @Test
    fun `grammar accepts text at the limit and rejects one character over`() {
        val atLimit = "a".repeat(AiLimits.GRAMMAR_MAX_CHARS)
        assertEquals(InputCheck.Accepted(Language.ENGLISH), InputValidator.grammar(atLimit, null))
        assertEquals(rejected(InvalidInputReason.TOO_LONG), InputValidator.grammar(atLimit + "a", null))
    }

    @Test
    fun `translation accepts text at the limit and rejects one character over`() {
        val atLimit = "a".repeat(AiLimits.TRANSLATION_MAX_CHARS)
        assertEquals(
            InputCheck.Accepted(Unit),
            InputValidator.translation(atLimit, Language.ENGLISH, Language.ARABIC),
        )
        assertEquals(
            rejected(InvalidInputReason.TOO_LONG),
            InputValidator.translation(atLimit + "a", Language.ENGLISH, Language.ARABIC),
        )
    }

    @Test
    fun `translation into the same language is unsupported`() {
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE),
            InputValidator.translation(ENGLISH, Language.ENGLISH, Language.ENGLISH),
        )
    }

    @Test
    fun `translation rejects text that is not in the stated source language`() {
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE),
            InputValidator.translation(ARABIC, Language.ENGLISH, Language.ARABIC),
        )
    }

    @Test
    fun `summary enforces its minimum and maximum`() {
        val min = "a".repeat(AiLimits.SUMMARY_MIN_CHARS)
        val max = "a".repeat(AiLimits.SUMMARY_MAX_CHARS)
        assertEquals(rejected(InvalidInputReason.TOO_SHORT), InputValidator.summary(min.dropLast(1), null))
        assertEquals(InputCheck.Accepted(Language.ENGLISH), InputValidator.summary(min, null))
        assertEquals(InputCheck.Accepted(Language.ENGLISH), InputValidator.summary(max, null))
        assertEquals(rejected(InvalidInputReason.TOO_LONG), InputValidator.summary(max + "a", null))
    }

    @Test
    fun `language is detected when none is stated`() {
        assertEquals(InputCheck.Accepted(Language.ARABIC), InputValidator.grammar(ARABIC, null))
        assertEquals(InputCheck.Accepted(Language.ENGLISH), InputValidator.grammar(ENGLISH, null))
    }

    @Test
    fun `undetectable language is unsupported when none is stated`() {
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE),
            InputValidator.grammar("Привет, как дела", null),
        )
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE),
            InputValidator.grammar("abcde ابتثج", null),
        )
    }

    @Test
    fun `mixed text is accepted for a stated language and rejected when far from it`() {
        assertEquals(
            InputCheck.Accepted(Language.ARABIC),
            InputValidator.grammar("ابتثجح abcd", Language.ARABIC),
        )
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_LANGUAGE),
            InputValidator.grammar(ENGLISH, Language.ARABIC),
        )
    }

    @Test
    fun `speech enforces emptiness format duration and size`() {
        fun clip(size: Int = 10, mime: String = "audio/wav", millis: Long = 1_000) =
            AudioClip(ByteArray(size), mime, millis)

        assertEquals(InputCheck.Accepted(Unit), InputValidator.speech(clip()))
        assertEquals(rejected(InvalidInputReason.EMPTY), InputValidator.speech(clip(size = 0)))
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_FORMAT),
            InputValidator.speech(clip(mime = "audio/amr")),
        )
        assertEquals(
            InputCheck.Accepted(Unit),
            InputValidator.speech(clip(millis = AiLimits.SPEECH_MAX_MILLIS)),
        )
        assertEquals(
            rejected(InvalidInputReason.TOO_LONG),
            InputValidator.speech(clip(millis = AiLimits.SPEECH_MAX_MILLIS + 1)),
        )
        assertEquals(
            InputCheck.Accepted(Unit),
            InputValidator.speech(clip(size = AiLimits.SPEECH_MAX_BYTES)),
        )
        assertEquals(
            rejected(InvalidInputReason.TOO_LARGE),
            InputValidator.speech(clip(size = AiLimits.SPEECH_MAX_BYTES + 1)),
        )
    }

    @Test
    fun `image enforces emptiness format and size`() {
        fun image(size: Int = 10, mime: String = "image/jpeg") = ImageInput(ByteArray(size), mime)

        assertEquals(InputCheck.Accepted(Unit), InputValidator.image(image()))
        assertEquals(rejected(InvalidInputReason.EMPTY), InputValidator.image(image(size = 0)))
        assertEquals(
            rejected(InvalidInputReason.UNSUPPORTED_FORMAT),
            InputValidator.image(image(mime = "image/gif")),
        )
        assertEquals(InputCheck.Accepted(Unit), InputValidator.image(image(size = AiLimits.IMAGE_MAX_BYTES)))
        assertEquals(
            rejected(InvalidInputReason.TOO_LARGE),
            InputValidator.image(image(size = AiLimits.IMAGE_MAX_BYTES + 1)),
        )
    }
}

class OutputValidatorTest {

    private val input = "He go to school every day and he like it very much."
    private val fixed = "He goes to school every day and he likes it very much."
    private val prompt = Prompts.grammar(Language.ENGLISH)

    private fun correction(raw: String, truncated: Boolean = false, expected: Language = Language.ENGLISH) =
        OutputValidator.text(TextTask.CORRECTION, input, raw, expected, prompt, truncated)

    @Test
    fun `a clean reply is returned unchanged`() {
        assertEquals(fixed, correction(fixed))
    }

    @Test
    fun `wrappers are removed`() {
        assertEquals(fixed, correction("  $fixed \n"))
        assertEquals(fixed, correction("\"$fixed\""))
        assertEquals(fixed, correction("“$fixed”"))
        assertEquals(fixed, correction("<content>\n$fixed\n</content>"))
        assertEquals(fixed, correction("```\n$fixed\n```"))
        assertEquals(fixed, correction("```text\n$fixed\n```"))
        assertEquals(fixed, correction("<think>Let me fix the verbs.</think>\n$fixed"))
    }

    @Test
    fun `quotes that belong to the text are kept`() {
        val quoted = "\"Yes,\" he said, and then he added \"no\""
        assertEquals(quoted, OutputValidator.clean(quoted))
    }

    @Test
    fun `an empty reply is rejected`() {
        assertNull(correction("   "))
        assertNull(correction("\"\""))
    }

    @Test
    fun `a truncated reply is rejected`() {
        assertNull(correction(fixed, truncated = true))
    }

    @Test
    fun `a reply in the wrong language is rejected`() {
        assertNull(correction("ذهب الولد إلى المدرسة كل يوم وهو يحبها كثيرا جدا"))
    }

    @Test
    fun `a reply that repeats the instruction is rejected`() {
        assertNull(correction("$prompt\n$fixed"))
    }

    @Test
    fun `a correction shorter than half or longer than double the input is rejected`() {
        assertNull(correction("He goes to school."))
        assertNull(correction("$fixed Here is why I changed it: $fixed $fixed"))
    }

    @Test
    fun `the length rule does not apply to very short inputs`() {
        assertEquals(
            "You are",
            OutputValidator.text(TextTask.CORRECTION, "u r", "You are", Language.ENGLISH, prompt, false),
        )
    }

    @Test
    fun `a summary must be shorter than the input`() {
        val summaryPrompt = Prompts.summary(Language.ENGLISH)
        assertEquals(
            "He likes school.",
            OutputValidator.text(TextTask.SUMMARY, input, "He likes school.", Language.ENGLISH, summaryPrompt, false),
        )
        assertNull(
            OutputValidator.text(TextTask.SUMMARY, input, "$input Indeed.", Language.ENGLISH, summaryPrompt, false),
        )
    }

    @Test
    fun `a translation must be in the target language`() {
        val translationPrompt = Prompts.translation(Language.ENGLISH, Language.ARABIC)
        val arabic = "يذهب إلى المدرسة كل يوم"
        assertEquals(
            arabic,
            OutputValidator.text(TextTask.TRANSLATION, input, arabic, Language.ARABIC, translationPrompt, false),
        )
        assertNull(
            OutputValidator.text(TextTask.TRANSLATION, input, input, Language.ARABIC, translationPrompt, false),
        )
    }

    @Test
    fun `media output maps the none token and blank to an empty result`() {
        assertEquals("", OutputValidator.media(Prompts.NONE_TOKEN, null, Prompts.imageText, false))
        assertEquals("", OutputValidator.media("  ", null, Prompts.imageText, false))
        assertEquals("EXIT", OutputValidator.media("EXIT", null, Prompts.imageText, false))
    }

    @Test
    fun `media output is rejected when truncated or in the wrong stated language`() {
        assertNull(OutputValidator.media("EXIT", null, Prompts.imageText, true))
        assertNull(OutputValidator.media("hello there", Language.ARABIC, Prompts.speech(Language.ARABIC), false))
    }
}
