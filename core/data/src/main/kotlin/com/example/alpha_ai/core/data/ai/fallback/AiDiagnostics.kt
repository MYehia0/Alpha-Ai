package com.example.alpha_ai.core.data.ai.fallback

import com.example.alpha_ai.core.data.ai.config.Capability
import com.example.alpha_ai.core.data.ai.config.ModelChoice
import com.example.alpha_ai.core.data.ai.prompt.Prompts
import com.example.alpha_ai.core.domain.ai.AiFailure
import com.example.alpha_ai.core.network.ProviderFailure
import timber.log.Timber

/**
 * The diagnostic lines of the AI layer: one per attempt and one per request. They name what
 * was tried and how it ended. They never hold input, output, a key, a header, or a raw
 * provider message (data-model.md, section 8).
 */
internal object AiDiagnostics {

    const val LOG_TAG = "AiLayer"
    private const val SUCCESS = "SUCCESS"

    /** @param failure the attempt's failure, or `null` when it succeeded */
    fun attempt(
        capability: Capability,
        choice: ModelChoice,
        attemptNumber: Int,
        failure: ProviderFailure?,
        elapsedMillis: Long,
    ) {
        Timber.tag(LOG_TAG).d(
            "attempt capability=%s provider=%s model=%s promptVersion=%d attempt=%d outcome=%s elapsedMs=%d",
            capability,
            choice.provider,
            choice.modelId,
            Prompts.VERSION,
            attemptNumber,
            failure?.let { it::class.simpleName } ?: SUCCESS,
            elapsedMillis,
        )
    }

    /** @param failure the request's failure, or `null` when it succeeded */
    fun request(capability: Capability, failure: AiFailure?, totalMillis: Long) {
        Timber.tag(LOG_TAG).d(
            "request capability=%s promptVersion=%d result=%s totalMs=%d",
            capability,
            Prompts.VERSION,
            failure?.let { it::class.simpleName } ?: SUCCESS,
            totalMillis,
        )
    }
}
