package com.example.alpha_ai.core.data.ai.config

import com.example.alpha_ai.core.network.AiProvider

/** The capabilities served by a hosted model. Language detection runs on the device. */
internal enum class Capability { GRAMMAR, TRANSLATION, SUMMARIZATION, SPEECH, IMAGE_TEXT }

/** How a model is called. */
internal enum class ApiStyle {
    /** The chat completions API, for text, images, and audio sent as message parts. */
    CHAT,

    /** Hugging Face's speech recognition task API, which takes raw audio. */
    HF_SPEECH,
}

/**
 * One model on one provider that can serve a capability.
 *
 * @param rank the model's place in the capability's fallback chain; lowest is tried first
 * @param enabled false keeps the entry documented but out of the chain
 */
internal data class ModelChoice(
    val provider: AiProvider,
    val modelId: String,
    val rank: Int,
    val apiStyle: ApiStyle = ApiStyle.CHAT,
    val enabled: Boolean = true,
)

/** Everything configured for one provider: where it is and which models serve what. */
internal data class ProviderModels(
    val provider: AiProvider,
    val baseUrl: String,
    val choices: Map<Capability, List<ModelChoice>>,
)

/** Supplies the ordered, enabled model choices for a capability. */
internal interface ModelChains {
    suspend fun chainFor(capability: Capability): List<ModelChoice>
}
