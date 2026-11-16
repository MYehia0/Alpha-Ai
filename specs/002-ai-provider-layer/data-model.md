# Data Model: AI Provider Layer

**Date**: 2026-11-17 | **Plan**: [plan.md](plan.md)

Nothing here is stored. These are the in-memory types the layer passes around, grouped by the
module that owns them, and the rules that connect them.

## 1. Domain types (`:core:domain`, package `core.domain.ai`)

Pure Kotlin. These are the only types a feature sees.

| Type | Kind | Fields or values | Rules |
|---|---|---|---|
| `Language` | enum | `ARABIC`, `ENGLISH` | The languages a capability accepts |
| `DetectedLanguage` | enum | `ARABIC`, `ENGLISH`, `UNSUPPORTED` | Result of language detection |
| `AiResult<T>` | sealed | `Success(value: T)`, `Failure(failure: AiFailure)` | Every one-shot call returns exactly one. Cancellation is not a value: the coroutine is cancelled |
| `AiFailure` | sealed | see section 2 | Carries `canRetry`. Never carries a status code, provider, model, or raw message |
| `InvalidInputReason` | enum | `EMPTY`, `TOO_SHORT`, `TOO_LONG`, `TOO_LARGE`, `UNSUPPORTED_LANGUAGE`, `UNSUPPORTED_FORMAT` | The specific reason inside `AiFailure.InvalidInput` |
| `AudioClip` | class | `bytes: ByteArray`, `mimeType: String`, `durationMillis: Long` | Held in memory only; the caller owns and deletes any file it came from |
| `ImageInput` | class | `bytes: ByteArray`, `mimeType: String` | Same |
| `TextStreamEvent` | sealed | `Delta(text)`, `Restarted`, `Completed(text)`, `Failed(failure)` | See section 6 |
| `AiLimits` | object | see section 3 | One place for input limits |

## 2. Failure categories

| `AiFailure` | `canRetry` | Meaning for the user | Extra data |
|---|---|---|---|
| `NoConnection` | yes | The device is offline | |
| `TimedOut` | yes | No answer in time | |
| `ServiceBusy` | yes, later | Rate limit, daily allowance spent, model loading or overloaded | `retryAfterSeconds: Long?` |
| `ServiceUnavailable` | no | Model gone, access rejected, provider error | |
| `InvalidInput` | no | The input cannot be accepted | `reason: InvalidInputReason` |
| `InvalidResponse` | yes | The models answered, but not with a usable result | |
| `Unknown` | yes | Anything else | |

Each category, and each `InvalidInputReason`, has one string resource in English and one in
Arabic in `:core:ui`. A `when` over the sealed type with no `else` branch makes a missing
mapping a compile error.

## 3. Input limits (`AiLimits`)

Defaults for this feature. A later feature spec may change a value here; nothing else changes.

| Capability | Limit | Failure reason when broken |
|---|---|---|
| All text capabilities | not blank after trimming | `EMPTY` |
| Grammar correction | at most 5,000 characters | `TOO_LONG` |
| Translation | at most 5,000 characters; source and target differ | `TOO_LONG`, `UNSUPPORTED_LANGUAGE` |
| Summarization | 200 to 20,000 characters | `TOO_SHORT`, `TOO_LONG` |
| Grammar correction, translation, summarization | when a language is stated (for translation, the source): at least 30% of the letters are in that language's script, so Arabic text with English terms, or the reverse, is accepted. When none is stated: the detected language (60% rule, research.md section 5) is Arabic or English | `UNSUPPORTED_LANGUAGE` |
| Speech recognition | at most 60 seconds and 5 MB; `audio/wav`, `audio/mpeg`, `audio/mp4`, `audio/ogg`, `audio/flac` | `TOO_LONG`, `TOO_LARGE`, `UNSUPPORTED_FORMAT`, `EMPTY` |
| Image text extraction | at most 4 MB; `image/jpeg`, `image/png`, `image/webp` | `TOO_LARGE`, `UNSUPPORTED_FORMAT`, `EMPTY` |
| Language detection | not blank | `EMPTY` |

Validation runs before any model is called (FR-005). The audio formats are confirmed against
the chosen model in implementation phase 2.

## 4. Network types (`:core:network`)

Public to `:core:data`; DTOs are `internal` and not listed.

| Type | Kind | Fields or values |
|---|---|---|
| `AiProvider` | enum | `OPENROUTER`, `HUGGING_FACE` |
| `AiCredentials` | interface | `tokenFor(provider): String?`; supplied by `:app` |
| `ConnectivityChecker` | interface | `isOnline(): Boolean` |
| `ChatRequest` | class | `model`, `systemPrompt`, `content: List<ContentPart>`, `maxTokens`, `temperature` |
| `ContentPart` | sealed | `Text(text)`, `Image(base64, mimeType)`, `Audio(base64, format)` |
| `ChatReply` | class | `text`, `servedBy: String?` (the model that answered), `truncated: Boolean` |
| `ChatStreamEvent` | sealed | `Delta(text)`, `Done(truncated)`, `Error(failure)` |
| `ProviderResult<T>` | sealed | `Success(value)`, `Failure(failure: ProviderFailure)` |
| `ProviderFailure` | sealed | see section 5 |

## 5. Provider failures and what the layer does with them

`ProviderFailure` is what the network module reports for one attempt on one model. The
fallback runner in `:core:data` decides what happens next and what the user is finally told.

| `ProviderFailure` | Typical signal | Try next choice? | Cooldown | Becomes `AiFailure` |
|---|---|---|---|---|
| `NoConnection` | no validated network; unknown host; connection refused | no, stop | none | `NoConnection` |
| `Timeout` | attempt time limit; socket timeout; HTTP 408 | yes | model, 30 s | `TimedOut` |
| `RateLimited(retryAfter)` | 429 | yes | model, `Retry-After` or 60 s | `ServiceBusy` |
| `QuotaExhausted(resetAt)` | 429 with `X-RateLimit-Remaining: 0` | yes, skipping that provider | provider, until reset or 1 hour | `ServiceBusy` |
| `Loading(estimate)` | 503 with a loading message or `estimated_time` | yes | model, estimate or 30 s | `ServiceBusy` |
| `Overloaded` | 503; `provider_overloaded` | yes | model, 30 s | `ServiceBusy` |
| `ModelNotFound` | 404; `not_found`; `model_not_supported` | yes | model, until restart | `ServiceUnavailable` |
| `AccessRejected` | 401; 403 `permission_denied`; blank key | yes, skipping that provider | provider, until restart | `ServiceUnavailable` |
| `NoCredits` | 402 | yes, skipping that provider | provider, until restart | `ServiceUnavailable` |
| `ServerError` | 500, 502; `provider_unavailable` | yes | model, 30 s | `ServiceUnavailable` |
| `RequestRejected` | 400, 413, 422; context length exceeded | yes | none | `ServiceUnavailable` |
| `ContentBlocked` | 403 `content_policy_violation` or `refusal` | yes | none | `InvalidResponse` |
| `MalformedResponse` | body does not parse; no choices; empty text; connection dropped mid-body | yes | none (5 min after 3 in a row) | `InvalidResponse` |
| `Unknown` | anything else | yes | none | `Unknown` |

Output that parses but fails the capability's output check (research.md, section 7) is
treated as `MalformedResponse`.

### Choosing the one failure to return (FR-018)

When every choice has failed or been skipped, the runner returns the highest-ranked category
among the attempts and the cooldown reasons of skipped choices:

`ServiceBusy` > `TimedOut` > `ServiceUnavailable` > `InvalidResponse` > `Unknown`

`ServiceBusy.retryAfterSeconds` is the shortest known wait among the busy choices.
`NoConnection` and `InvalidInput` never reach this step: they end the request on their own.

## 6. Configuration types (`:core:data`, internal)

| Type | Fields | Rules |
|---|---|---|
| `Capability` | enum: `GRAMMAR`, `TRANSLATION`, `SUMMARIZATION`, `SPEECH`, `IMAGE_TEXT` | The capabilities served by a hosted model. Language detection is a fixed on-device implementation and has no chain |
| `ApiStyle` | enum: `CHAT`, `HF_SPEECH` | Which engine handles a choice |
| `ModelChoice` | `provider`, `modelId`, `apiStyle`, `rank: Int`, `enabled: Boolean` | Valid when `modelId` is not blank, `rank` is positive and unique within the capability, and (for OpenRouter) the id ends in `:free` or is `openrouter/free` |
| `ProviderModels` | `provider`, `baseUrl`, `choices: Map<Capability, List<ModelChoice>>` | One per provider: `OpenRouterModels`, `HuggingFaceModels` |
| `FallbackChain` | `capability`, `choices: List<ModelChoice>` | Enabled choices from every provider, sorted by `rank`. Never empty after resolution |
| `ModelConfigSource` | `overrides(): Map<AiProvider, ProviderModels>?` | Optional replacement, for a remote source later. Bound to "none" in this feature |
| `ModelHealth` | `skippedUntil: Instant?`, `reason: ProviderFailure`, `consecutiveInvalid: Int` | Per provider and per model, in memory |

### Resolution rules (FR-011, FR-012)

1. Start from the built-in files. They are the defaults.
2. If the source supplies overrides, validate each entry. A valid entry replaces the built-in
   one for that capability and provider. An invalid or missing entry keeps the built-in one
   and is logged once with the capability, provider, and reason.
3. Merge the two providers' entries per capability, drop disabled ones, sort by `rank`.
4. If a chain ends up empty, use the built-in chain for that capability and log it.

A unit test asserts that the built-in files pass validation with nothing dropped and that
every capability's chain has at least two enabled choices, except speech recognition
(plan.md, Complexity Tracking).

## 7. Request lifecycle

```text
validate input ──invalid──────────────────────────────► Failure(InvalidInput)
      │
check connectivity ──offline──────────────────────────► Failure(NoConnection)
      │
for each choice in the chain, while time remains:
      skip if the model or its provider is cooling down
      call the model (attempt time limit)
        ├─ success ─► clean and check output
        │               ├─ passes ───────────────────► Success(value)
        │               └─ fails ─► record, next choice
        ├─ NoConnection ─────────────────────────────► Failure(NoConnection)
        └─ any other failure ─► record, set cooldown, next choice
      │
no choice left, or time is up ───────────────────────► Failure(chosen by section 5)
```

Cancelling the caller's coroutine at any point stops the current call and ends the loop with
no value delivered.

### Streaming variant

The same loop, emitting `TextStreamEvent`:

- `Delta` for each piece of text as it arrives.
- `Restarted` when an attempt fails after sending any `Delta` and another choice takes over;
  the screen discards the text shown so far.
- Exactly one terminal event: `Completed` with the cleaned and checked full text, or `Failed`.
  If the final check fails and no choice is left, the terminal event is `Failed`.

## 8. Diagnostics record (FR-028)

One Timber line per attempt and one per request, with these fields only:

`capability`, `provider`, `model`, `promptVersion`, `attempt`, `outcome` (success or the
`ProviderFailure` name), `elapsedMs`, and for the request line `result` (success or the
`AiFailure` name) and `totalMs`.

No input, output, key, header, or raw provider message is ever included.
