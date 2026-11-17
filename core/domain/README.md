# :core:domain

Use cases, domain models, and repository interfaces. Pure Kotlin: no Android, Compose, Retrofit, or Firebase.

**May depend on**: `:core:common` only.

## What is here

`ai/`: the AI capabilities the rest of the app uses (spec 002). Features depend on these
interfaces and never on a provider or a model:

- `GrammarCorrector`, `Translator`, `Summarizer` (each with a streaming form),
  `SpeechTranscriber`, `ImageTextExtractor`, `LanguageDetector`.
- `AiResult`: success with a value, or failure with an `AiFailure`.
- `AiFailure`: the reason shown to the user, with `canRetry`. `:core:ui` maps each one to an
  Arabic and English message (`AiFailure.messageRes()`).
- `AiLimits`: the input limits, so a screen can check before it sends.

See [the module rules](../../specs/001-build-foundation/data-model.md).
