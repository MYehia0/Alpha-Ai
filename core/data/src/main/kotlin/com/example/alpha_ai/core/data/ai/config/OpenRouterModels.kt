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
 * The order was measured on 2026-11-17 (docs/ai-models.md). For text, the first two choices
 * are served by different upstream hosts (Nvidia and Google), so one host's outage does not
 * take the capability down. Gemma's shared free pool was busy on nearly every attempt that
 * day; a busy model fails in under a second, so it costs little as the second try. For image
 * text, the free router comes before Nemotron VL, which misread the Arabic samples.
 */
internal val OpenRouterModels: ProviderModels = run {
    fun choice(modelId: String, rank: Int) = ModelChoice(AiProvider.OPENROUTER, modelId, rank)

    val text = listOf(
        choice("nvidia/nemotron-3-ultra-550b-a55b:free", rank = 10),
        choice("google/gemma-4-31b-it:free", rank = 20),
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
                choice(OPENROUTER_FREE_ROUTER, rank = 20),
                // Exact on English; misread the Arabic samples.
                choice("nvidia/nemotron-3.5-super-vl:free", rank = 30),
            ),
            // The only free model that accepts audio. OpenRouter refuses any audio request
            // unless the account holds at least $0.50 of credit (measured 2026-11-17), so on
            // a zero-credit account this capability always reports ServiceUnavailable.
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
