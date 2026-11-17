package com.example.alpha_ai.core.data.ai.config

import com.example.alpha_ai.core.network.AiProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An optional replacement for the built-in model configuration, for example a remote source
 * added by a later feature. Returning `null` means "use what is built in".
 */
internal interface ModelConfigSource {
    suspend fun overrides(): Map<AiProvider, ProviderModels>?
}

/** The source bound in this feature: there are no overrides. */
internal class NoOverrides @Inject constructor() : ModelConfigSource {
    override suspend fun overrides(): Map<AiProvider, ProviderModels>? = null
}

/** The rules a configuration entry must satisfy (contracts/model-configuration.md). */
internal object ModelRules {

    /** The rule [choice] breaks as an entry of [provider]'s file, or `null` when it is valid. */
    fun violation(provider: AiProvider, choice: ModelChoice): String? = when {
        choice.provider != provider -> "provider"
        choice.modelId.isBlank() -> "M-1"
        choice.rank <= 0 -> "M-2"
        provider == AiProvider.OPENROUTER && !choice.modelId.isFreeOpenRouterModel() -> "M-3"
        choice.apiStyle == ApiStyle.HF_SPEECH && provider != AiProvider.HUGGING_FACE -> "M-4"
        else -> null
    }

    /** M-8: a provider is only ever reached over an encrypted connection. */
    fun isSecure(baseUrl: String): Boolean = baseUrl.startsWith("https://")

    /** M-2: ranks are unique within a capability across providers. */
    fun hasUniqueRanks(choices: List<ModelChoice>): Boolean =
        choices.map { it.rank }.toSet().size == choices.size

    private fun String.isFreeOpenRouterModel(): Boolean =
        this == OPENROUTER_FREE_ROUTER || endsWith(OPENROUTER_FREE_SUFFIX)
}

/**
 * Builds each capability's fallback chain: the built-in files, with valid overrides applied,
 * merged across providers, disabled entries dropped, sorted by rank.
 *
 * Configuration can never crash the app. An entry that breaks a rule is left out and logged
 * once; an override that breaks a rule keeps the built-in entries; a chain that would end up
 * empty uses the built-in chain. Overrides replace model choices only: a provider's address
 * is fixed when the app starts.
 */
@Singleton
internal class ModelConfigResolver(
    private val source: ModelConfigSource,
    private val builtIn: List<ProviderModels>,
) : ModelChains {

    @Inject
    constructor(source: ModelConfigSource) : this(source, BuiltInModels)

    private val mutex = Mutex()
    private var resolved: Map<Capability, List<ModelChoice>>? = null

    override suspend fun chainFor(capability: Capability): List<ModelChoice> =
        mutex.withLock { resolved ?: resolve().also { resolved = it } }[capability].orEmpty()

    private suspend fun resolve(): Map<Capability, List<ModelChoice>> {
        val overrides = loadOverrides()
        return Capability.entries.associateWith { capability ->
            val defaults = builtIn.flatMap { validBuiltIn(it, capability) }
            val merged = builtIn.flatMap { provider ->
                overrideFor(overrides, provider.provider, capability) ?: validBuiltIn(provider, capability)
            }
            val chosen = when {
                ModelRules.hasUniqueRanks(merged) -> merged
                else -> {
                    log(capability, provider = null, rule = "M-2")
                    defaults
                }
            }
            chosen.toChain().ifEmpty {
                if (chosen !== defaults) log(capability, provider = null, rule = "empty chain")
                defaults.toChain()
            }
        }
    }

    private suspend fun loadOverrides(): Map<AiProvider, ProviderModels>? =
        try {
            source.overrides()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            Timber.tag(LOG_TAG).w("config overrides could not be read; using the built-in models")
            null
        }

    /** The override's entries for one provider and capability, or `null` to keep the built-in ones. */
    private fun overrideFor(
        overrides: Map<AiProvider, ProviderModels>?,
        provider: AiProvider,
        capability: Capability,
    ): List<ModelChoice>? {
        if (overrides == null) return null
        val models = overrides[provider]
        val entries = models?.choices?.get(capability)
        val rule = when {
            models == null || entries == null -> "missing"
            !ModelRules.isSecure(models.baseUrl) -> "M-8"
            else -> entries.firstNotNullOfOrNull { ModelRules.violation(provider, it) }
        }
        if (rule != null) {
            log(capability, provider, rule)
            return null
        }
        return entries
    }

    private fun validBuiltIn(models: ProviderModels, capability: Capability): List<ModelChoice> {
        if (!ModelRules.isSecure(models.baseUrl)) {
            log(capability, models.provider, "M-8")
            return emptyList()
        }
        return models.choices[capability].orEmpty().filter { choice ->
            val rule = ModelRules.violation(models.provider, choice)
            if (rule != null) log(capability, models.provider, rule)
            rule == null
        }
    }

    private fun List<ModelChoice>.toChain(): List<ModelChoice> = filter { it.enabled }.sortedBy { it.rank }

    private fun log(capability: Capability, provider: AiProvider?, rule: String) {
        Timber.tag(LOG_TAG).w(
            "config entry not used capability=%s provider=%s rule=%s",
            capability,
            provider ?: "ALL",
            rule,
        )
    }

    internal companion object {
        const val LOG_TAG = "AiConfig"
    }
}
