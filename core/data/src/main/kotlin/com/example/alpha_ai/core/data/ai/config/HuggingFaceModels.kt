package com.example.alpha_ai.core.data.ai.config

import com.example.alpha_ai.core.network.AiProvider

/**
 * The Hugging Face models that can serve each capability. This is the only place a Hugging
 * Face model identifier or base URL appears.
 *
 * Every entry is switched off: Hugging Face no longer serves inference on a free account, so
 * a request would fail with "no credits". To switch the provider on, add credits to the
 * account behind `HF_API_TOKEN`, set `enabled = true` here, and record the change in
 * docs/ai-models.md. Text and image entries are added the same way once models are chosen;
 * their ranks start at 40, after the OpenRouter choices.
 */
internal val HuggingFaceModels = ProviderModels(
    provider = AiProvider.HUGGING_FACE,
    baseUrl = "https://router.huggingface.co/",
    choices = mapOf(
        Capability.SPEECH to listOf(
            ModelChoice(
                provider = AiProvider.HUGGING_FACE,
                modelId = "openai/whisper-large-v3-turbo",
                rank = 20,
                apiStyle = ApiStyle.HF_SPEECH,
                enabled = false,
            ),
        ),
    ),
)

/** The configuration built into the app, one entry per provider. */
internal val BuiltInModels: List<ProviderModels> = listOf(OpenRouterModels, HuggingFaceModels)
