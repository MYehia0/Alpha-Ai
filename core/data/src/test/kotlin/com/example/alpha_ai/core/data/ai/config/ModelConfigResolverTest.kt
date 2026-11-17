package com.example.alpha_ai.core.data.ai.config

import com.example.alpha_ai.core.data.ai.fakes.AiTestHarness
import com.example.alpha_ai.core.domain.ai.AiResult
import com.example.alpha_ai.core.domain.ai.AudioClip
import com.example.alpha_ai.core.network.AiProvider
import com.example.alpha_ai.core.network.ProviderFailure
import com.example.alpha_ai.core.network.ProviderResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import timber.log.Timber
import java.io.IOException

class ModelConfigResolverTest {

    private val warnings = mutableListOf<String>()

    private fun recordWarnings() {
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                if (tag == ModelConfigResolver.LOG_TAG) warnings += message
            }
        })
    }

    @After
    fun tearDown() = Timber.uprootAll()

    private fun source(overrides: Map<AiProvider, ProviderModels>?) = object : ModelConfigSource {
        override suspend fun overrides() = overrides
    }

    private fun openRouter(modelId: String, rank: Int) = ModelChoice(AiProvider.OPENROUTER, modelId, rank)

    private fun huggingFace(modelId: String, rank: Int, style: ApiStyle = ApiStyle.CHAT) =
        ModelChoice(AiProvider.HUGGING_FACE, modelId, rank, style)

    // ---- the built-in files ---------------------------------------------------------------

    @Test
    fun `the built-in files break no rule`() {
        for (models in BuiltInModels) {
            assertTrue(models.provider.name, ModelRules.isSecure(models.baseUrl))
            for ((capability, choices) in models.choices) {
                for (choice in choices) {
                    assertNull("$capability ${choice.modelId}", ModelRules.violation(models.provider, choice))
                }
            }
        }
        for (capability in Capability.entries) {
            val all = BuiltInModels.flatMap { it.choices[capability].orEmpty() }
            assertTrue(capability.name, ModelRules.hasUniqueRanks(all))
        }
    }

    @Test
    fun `the built-in chains are used whole and in rank order with nothing logged`() = runTest {
        recordWarnings()
        val resolver = ModelConfigResolver(NoOverrides())

        for (capability in Capability.entries) {
            val enabled = BuiltInModels.flatMap { it.choices[capability].orEmpty() }.filter { it.enabled }
            assertEquals(capability.name, enabled.sortedBy { it.rank }, resolver.chainFor(capability))
        }
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `every capability has two enabled choices except speech`() = runTest {
        val resolver = ModelConfigResolver(NoOverrides())

        for (capability in Capability.entries - Capability.SPEECH) {
            assertTrue(capability.name, resolver.chainFor(capability).size >= 2)
        }
        assertEquals(1, resolver.chainFor(Capability.SPEECH).size)
    }

    @Test
    fun `only free models are configured on openrouter and hugging face is off`() = runTest {
        val resolver = ModelConfigResolver(NoOverrides())

        for (capability in Capability.entries) {
            for (choice in resolver.chainFor(capability)) {
                assertEquals(AiProvider.OPENROUTER, choice.provider)
                assertTrue(choice.modelId, choice.modelId.endsWith(":free") || choice.modelId == "openrouter/free")
            }
        }
    }

    // ---- rules ----------------------------------------------------------------------------

    @Test
    fun `each rule rejects the entry that breaks it`() {
        assertEquals("M-1", ModelRules.violation(AiProvider.OPENROUTER, openRouter(" ", 10)))
        assertEquals("M-2", ModelRules.violation(AiProvider.OPENROUTER, openRouter("a/b:free", 0)))
        assertEquals("M-3", ModelRules.violation(AiProvider.OPENROUTER, openRouter("vendor/paid-model", 10)))
        assertEquals(
            "M-4",
            ModelRules.violation(
                AiProvider.OPENROUTER,
                ModelChoice(AiProvider.OPENROUTER, "a/b:free", 10, ApiStyle.HF_SPEECH),
            ),
        )
        assertNull(ModelRules.violation(AiProvider.HUGGING_FACE, huggingFace("vendor/paid-model", 10)))
        assertTrue(ModelRules.isSecure("https://example.org/"))
        assertTrue(!ModelRules.isSecure("http://example.org/"))
        assertTrue(!ModelRules.hasUniqueRanks(listOf(openRouter("a:free", 10), huggingFace("b", 10))))
    }

    // ---- overrides ------------------------------------------------------------------------

    @Test
    fun `an override moves the first choice to the other provider and changes nothing else`() = runTest {
        val overrides = mapOf(
            AiProvider.HUGGING_FACE to HuggingFaceModels.copy(
                choices = mapOf(Capability.GRAMMAR to listOf(huggingFace("vendor/grammar-model", 5))),
            ),
        )
        val resolver = ModelConfigResolver(source(overrides))
        val builtIn = ModelConfigResolver(NoOverrides())

        assertEquals(
            listOf(huggingFace("vendor/grammar-model", 5)) + builtIn.chainFor(Capability.GRAMMAR),
            resolver.chainFor(Capability.GRAMMAR),
        )
        for (capability in Capability.entries - Capability.GRAMMAR) {
            assertEquals(builtIn.chainFor(capability), resolver.chainFor(capability))
        }
    }

    @Test
    fun `an override replaces a provider's models for one capability`() = runTest {
        val replacement = listOf(openRouter("vendor/new-model:free", 10), openRouter("openrouter/free", 20))
        val overrides = mapOf(
            AiProvider.OPENROUTER to OpenRouterModels.copy(choices = mapOf(Capability.TRANSLATION to replacement)),
        )
        val resolver = ModelConfigResolver(source(overrides))

        assertEquals(replacement, resolver.chainFor(Capability.TRANSLATION))
        assertEquals(
            ModelConfigResolver(NoOverrides()).chainFor(Capability.GRAMMAR),
            resolver.chainFor(Capability.GRAMMAR),
        )
    }

    @Test
    fun `an override with an insecure base url is rejected and logged`() = runTest {
        recordWarnings()
        val overrides = mapOf(
            AiProvider.OPENROUTER to ProviderModels(
                provider = AiProvider.OPENROUTER,
                baseUrl = "http://openrouter.ai/api/v1/",
                choices = mapOf(Capability.GRAMMAR to listOf(openRouter("vendor/new-model:free", 10))),
            ),
        )
        val resolver = ModelConfigResolver(source(overrides))

        assertEquals(
            ModelConfigResolver(NoOverrides()).chainFor(Capability.GRAMMAR),
            resolver.chainFor(Capability.GRAMMAR),
        )
        assertTrue(warnings.any { "capability=GRAMMAR" in it && "provider=OPENROUTER" in it && "rule=M-8" in it })
    }

    @Test
    fun `a malformed override keeps the built-in entries and is logged once`() = runTest {
        recordWarnings()
        val overrides = mapOf(
            AiProvider.OPENROUTER to OpenRouterModels.copy(
                choices = mapOf(Capability.GRAMMAR to listOf(openRouter("vendor/paid-model", 10))),
            ),
        )
        val resolver = ModelConfigResolver(source(overrides))

        repeat(3) {
            assertEquals(
                ModelConfigResolver(NoOverrides()).chainFor(Capability.GRAMMAR),
                resolver.chainFor(Capability.GRAMMAR),
            )
        }
        assertEquals(
            1,
            warnings.count { "capability=GRAMMAR" in it && "provider=OPENROUTER" in it && "rule=M-3" in it },
        )
    }

    @Test
    fun `a missing override keeps the built-in entries and is logged`() = runTest {
        recordWarnings()
        val resolver = ModelConfigResolver(source(emptyMap()))

        assertEquals(
            ModelConfigResolver(NoOverrides()).chainFor(Capability.SUMMARIZATION),
            resolver.chainFor(Capability.SUMMARIZATION),
        )
        assertTrue(warnings.any { "capability=SUMMARIZATION" in it && "rule=missing" in it })
    }

    @Test
    fun `an override that repeats a rank keeps the built-in chain`() = runTest {
        recordWarnings()
        val overrides = mapOf(
            AiProvider.HUGGING_FACE to HuggingFaceModels.copy(
                choices = mapOf(Capability.GRAMMAR to listOf(huggingFace("vendor/grammar-model", 10))),
            ),
        )
        val resolver = ModelConfigResolver(source(overrides))

        assertEquals(
            ModelConfigResolver(NoOverrides()).chainFor(Capability.GRAMMAR),
            resolver.chainFor(Capability.GRAMMAR),
        )
        assertTrue(warnings.any { "capability=GRAMMAR" in it && "rule=M-2" in it })
    }

    @Test
    fun `an override that disables every choice falls back to the built-in chain`() = runTest {
        val overrides = mapOf(
            AiProvider.OPENROUTER to OpenRouterModels.copy(
                choices = mapOf(
                    Capability.GRAMMAR to listOf(openRouter("vendor/new-model:free", 10).copy(enabled = false)),
                ),
            ),
        )
        val resolver = ModelConfigResolver(source(overrides))

        assertEquals(
            ModelConfigResolver(NoOverrides()).chainFor(Capability.GRAMMAR),
            resolver.chainFor(Capability.GRAMMAR),
        )
    }

    @Test
    fun `a source that throws is treated as no overrides`() = runTest {
        recordWarnings()
        val resolver = ModelConfigResolver(
            object : ModelConfigSource {
                override suspend fun overrides(): Map<AiProvider, ProviderModels>? = throw IOException("offline")
            },
        )

        assertEquals(
            ModelConfigResolver(NoOverrides()).chainFor(Capability.IMAGE_TEXT),
            resolver.chainFor(Capability.IMAGE_TEXT),
        )
        assertEquals(1, warnings.size)
    }

    // ---- an enabled speech recognition model is used as the speech fallback ----------------

    @Test
    fun `an enabled hugging face speech model serves as the speech fallback`() = runTest {
        val enabled = HuggingFaceModels.choices.getValue(Capability.SPEECH).map { it.copy(enabled = true) }
        val overrides = mapOf(
            AiProvider.HUGGING_FACE to HuggingFaceModels.copy(choices = mapOf(Capability.SPEECH to enabled)),
        )
        val ai = AiTestHarness(this, ModelConfigResolver(source(overrides)))
        ai.client.handler = { ProviderResult.Failure(ProviderFailure.Overloaded) }
        ai.huggingFace.transcript = ProviderResult.Success(" good morning ")
        val clip = AudioClip(byteArrayOf(1, 2, 3), "audio/wav", durationMillis = 2_000)

        assertEquals(AiResult.Success("good morning"), ai.speech.transcribe(clip))
        assertEquals(1, ai.client.requests.size)
        val (model, bytes, mimeType) = ai.huggingFace.transcriptions.single()
        assertEquals("openai/whisper-large-v3-turbo", model)
        assertEquals("audio/wav", mimeType)
        assertTrue(bytes.contentEquals(clip.bytes))
    }
}
