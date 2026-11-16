# Contract: Model Configuration

**Owner**: `:core:data`, package `com.example.alpha_ai.core.data.ai.config`

**Audience**: the developer who changes which model serves a capability.

Types are defined in [../data-model.md](../data-model.md), section 6.

## Where models are set

One file per provider. These are the only places a model identifier or provider base URL
appears in the project (FR-011, Principle X):

| Provider | File | Base URL |
|---|---|---|
| OpenRouter | `OpenRouterModels.kt` | `https://openrouter.ai/api/v1/` |
| Hugging Face | `HuggingFaceModels.kt` | `https://router.huggingface.co/` |

Each file lists, per capability, that provider's models with a `rank`. A capability's
fallback chain is every enabled entry from both files, lowest rank first.

## Initial contents (provisional until the comparison in `docs/ai-models.md`)

| Capability | Rank | Provider | Model | Style | Enabled |
|---|---|---|---|---|---|
| Grammar, translation, summarization | 10 | OpenRouter | `google/gemma-4-31b-it:free` | `CHAT` | yes |
| | 20 | OpenRouter | `nvidia/nemotron-3-ultra-550b-a55b:free` | `CHAT` | yes |
| | 30 | OpenRouter | `openrouter/free` | `CHAT` | yes |
| | 40 | Hugging Face | chosen when the provider is switched on | `CHAT` | no |
| Image text extraction | 10 | OpenRouter | `google/gemma-4-31b-it:free` | `CHAT` | yes |
| | 20 | OpenRouter | `nvidia/nemotron-3.5-super-vl:free` | `CHAT` | yes |
| | 30 | OpenRouter | `openrouter/free` | `CHAT` | yes |
| Speech recognition | 10 | OpenRouter | `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free` | `CHAT` | yes |
| | 20 | Hugging Face | `openai/whisper-large-v3-turbo` | `HF_SPEECH` | no |

Language detection is not configured here. It is a fixed on-device implementation (script
analysis) with no hosted model.

## How to make a change

| Change | Edit |
|---|---|
| Replace a model | change its `modelId` in the provider's file |
| Reorder fallbacks | change `rank` values |
| Move the first choice to the other provider | give the entry in the other provider's file the lowest `rank`, and set `enabled = true` |
| Switch Hugging Face on | set `enabled = true` on its entries and add `HF_API_TOKEN` with credits |
| Retire a model | delete its entry, or set `enabled = false` |

No file outside these two changes. After any change: run the unit tests, run the live check
for that capability ([../quickstart.md](../quickstart.md), section 3), and update
`docs/ai-models.md` with the reason.

## Rules the configuration must satisfy

| # | Rule | Checked by |
|---|---|---|
| M-1 | `modelId` is not blank | resolver, unit test on the built-in files |
| M-2 | `rank` is positive and unique within a capability across both providers | resolver, unit test |
| M-3 | An OpenRouter `modelId` ends in `:free` or is `openrouter/free` (FR-008, SC-010) | resolver, unit test |
| M-4 | The `apiStyle` is one the provider supports (`HF_SPEECH` only on Hugging Face) | resolver, unit test |
| M-5 | Every capability has at least two enabled choices, except speech recognition | unit test on the built-in files |
| M-6 | Where two enabled choices exist, the first two are served by different upstream hosts | review, recorded in `docs/ai-models.md` |
| M-7 | A model is added only after its data-use terms are recorded in `docs/ai-models.md` (FR-010) | review |

An entry that breaks M-1 to M-4 is dropped at start-up, logged once (capability, provider,
rule), and the built-in entry is used in its place. The app never crashes on configuration
(FR-012).

## Override source

```kotlin
interface ModelConfigSource {
    suspend fun overrides(): Map<AiProvider, ProviderModels>?
}
```

This feature binds an implementation that returns `null`. A later feature can bind a remote
source; its entries go through the same rules, and anything invalid falls back to the
built-in files. No capability or feature code changes when that happens.

## Documentation record (`docs/ai-models.md`)

The one place a developer looks to learn which model serves a capability and why (US4,
scenario 4). It contains, per capability: the chain in order, the upstream host of each
model, the data-use terms found and the date checked, the comparison table (samples, scores,
timings), and a dated note for every later change.
