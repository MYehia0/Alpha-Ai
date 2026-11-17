package com.example.alpha_ai.core.data.ai.config

import javax.inject.Inject

/** The chains built into the app: every enabled choice for a capability, lowest rank first. */
internal class BundledModelChains @Inject constructor() : ModelChains {

    override suspend fun chainFor(capability: Capability): List<ModelChoice> =
        listOf(OpenRouterModels)
            .flatMap { it.choices[capability].orEmpty() }
            .filter { it.enabled }
            .sortedBy { it.rank }
}
