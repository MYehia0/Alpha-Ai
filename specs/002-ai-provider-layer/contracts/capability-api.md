# Contract: Capability API

**Owner**: `:core:domain`, package `com.example.alpha_ai.core.domain.ai`

**Consumers**: every `:feature:*` module, through Hilt injection. Features depend on
`:core:domain` only and never see a provider, a model, or an HTTP type.

Types referenced here are defined in [../data-model.md](../data-model.md), sections 1 to 3.

## Interfaces

```kotlin
interface GrammarCorrector {
    suspend fun correct(text: String, language: Language? = null): AiResult<String>
    fun correctStreaming(text: String, language: Language? = null): Flow<TextStreamEvent>
}

interface Translator {
    suspend fun translate(text: String, from: Language, to: Language): AiResult<String>
    fun translateStreaming(text: String, from: Language, to: Language): Flow<TextStreamEvent>
}

interface Summarizer {
    suspend fun summarize(text: String, language: Language? = null): AiResult<String>
    fun summarizeStreaming(text: String, language: Language? = null): Flow<TextStreamEvent>
}

interface SpeechTranscriber {
    suspend fun transcribe(audio: AudioClip, language: Language? = null): AiResult<String>
}

interface ImageTextExtractor {
    suspend fun extractText(image: ImageInput): AiResult<String>
}

interface LanguageDetector {
    suspend fun detect(text: String): AiResult<DetectedLanguage>
}
```

When `language` is `null`, the implementation detects it and fails with
`InvalidInput(UNSUPPORTED_LANGUAGE)` if it is neither Arabic nor English.

## Guarantees to callers

| # | Guarantee | Requirement |
|---|---|---|
| C-1 | A `suspend` function returns exactly one `AiResult` and never throws, except `CancellationException` when the caller is cancelled | FR-019 |
| C-2 | A streaming `Flow` is cold, ends with exactly one `Completed` or `Failed`, and never throws except on cancellation | FR-007, FR-019 |
| C-3 | Cancelling the calling coroutine stops the network call; nothing is delivered afterwards and no further model is called | FR-006 |
| C-4 | Functions are main-safe: they may be called from any dispatcher | Principle III |
| C-5 | Input outside `AiLimits` returns `Failure(InvalidInput(reason))` without any network call | FR-005 |
| C-6 | With no connection, the result is `Failure(NoConnection)` within 3 seconds | SC-006 |
| C-7 | Text calls end within 30 seconds, speech and image calls within 60 seconds | FR-017, SC-005 |
| C-8 | `Success` values are already cleaned and checked: correction and summary are in the input's language, translation is in the target language, and none contains commentary, quotation wrapping, or the instructions | FR-003, FR-004, FR-024 |
| C-9 | An audio clip with no speech, or an image with no text, returns `Success("")` | Spec edge cases |
| C-10 | No `AiFailure` exposes a status code, provider name, model name, or raw message | FR-022 |
| C-11 | Concurrent calls are safe; implementations hold no per-call state | Spec edge cases |
| C-12 | `LanguageDetector.detect` makes no network call | plan.md, Complexity Tracking |

## What callers must do

- Call from a scope tied to the screen (`viewModelScope`), so leaving the screen cancels the
  request.
- Turn an `AiFailure` into text with `AiFailure.messageRes()` from `:core:ui`, and offer
  "Try again" only when `canRetry` is true.
- Keep inputs within `AiLimits` where the screen can (for example, cap the text field), and
  still handle `InvalidInput`.
- Downscale and compress images, and delete temporary audio and image files, before or after
  the call. This layer only reads the bytes it is given.
- On `TextStreamEvent.Restarted`, clear the partial text shown.

## Failure messages (`:core:ui`)

```kotlin
@StringRes fun AiFailure.messageRes(): Int
```

| Key | Used for |
|---|---|
| `ai_failure_no_connection` | `NoConnection` |
| `ai_failure_timed_out` | `TimedOut` |
| `ai_failure_service_busy` | `ServiceBusy` |
| `ai_failure_service_unavailable` | `ServiceUnavailable` |
| `ai_failure_invalid_response` | `InvalidResponse` |
| `ai_failure_unknown` | `Unknown` |
| `ai_input_empty`, `ai_input_too_short`, `ai_input_too_long`, `ai_input_too_large`, `ai_input_unsupported_language`, `ai_input_unsupported_format` | `InvalidInput`, by reason |

Every key exists in `values/` and `values-ar/`. No message contains a number, a provider
name, or a model name.

## Test doubles (`:core:testing`)

A fake for each interface, configurable with the result to return and recording the calls it
received, for use by feature ViewModel tests in later specs.
