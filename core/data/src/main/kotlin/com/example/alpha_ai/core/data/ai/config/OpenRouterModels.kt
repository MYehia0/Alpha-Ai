package com.example.alpha_ai.core.data.ai.config

import com.example.alpha_ai.core.network.AiProvider

/**
 * The OpenRouter models that serve each capability. This is the only place an OpenRouter
 * model identifier or base URL appears. To change or reorder models, edit this file only,
 * then update docs/ai-models.md with the reason.
 *
 * Rules: every identifier is a free model (ends in `:free`, or is the free router), and
 * ranks are unique within a capability across both provider files.
 * See specs/002-ai-provider-layer/contracts/model-configuration.md.
 *
 * The order below is provisional until the comparison recorded in docs/ai-models.md is run.
 * The first two choices of a capability are served by different upstream hosts (Google and
 * Nvidia), so one host's outage does not take the capability down. The free router is last:
 * it picks any free model that is available, which covers a named model being withdrawn.
 */
internal val OpenRouterModels: ProviderModels = run {
    fun choice(modelId: String, rank: Int) = ModelChoice(AiProvider.OPENROUTER, modelId, rank)

    val text = listOf(
        choice("google/gemma-4-31b-it:free", rank = 10),
        choice("nvidia/nemotron-3-ultra-550b-a55b:free", rank = 20),
        choice(OPENROUTER_FREE_ROUTER, rank = 30),
    )

    ProviderModels(
        provider = AiProvider.OPENROUTER,
        baseUrl = "https://openrouter.ai/api/v1/",
        choices = mapOf(
            Capability.GRAMMAR to text,
            Capability.TRANSLATION to text,
            Capability.SUMMARIZATION to text,
            Capability.IMAGE_TEXT to listOf(
                choice("google/gemma-4-31b-it:free", rank = 10),
                choice("nvidia/nemotron-3.5-super-vl:free", rank = 20),
                choice(OPENROUTER_FREE_ROUTER, rank = 30),
            ),
            // The only free model that accepts audio.
            Capability.SPEECH to listOf(
                choice("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", rank = 10),
            ),
        ),
    )
}

/** OpenRouter's router that selects any available free model. */
internal const val OPENROUTER_FREE_ROUTER = "openrouter/free"

/** The suffix OpenRouter gives to model variants with no usage charge. */
internal const val OPENROUTER_FREE_SUFFIX = ":free"
