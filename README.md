# Alpha-AI

> **⚠️ Note:** This project is currently under refactoring. The graduation project focused on NLP integration and backend architecture rather than UI design. UI improvements are in progress.

AI-powered language enhancement Android app offering grammar correction in Arabic and English.

**Graduation Project | Grade: A+**

---

## Overview

Alpha-AI helps users improve their writing by providing:
- Grammar correction in Arabic and English
- Text summarization
- Speech-to-Text (STT) and Text-to-Speech (TTS)
- Translation between languages
- Text extraction from audio, images (OCR), and PDF documents

---

## Features

- **Grammar Correction:** Real-time grammar fixes using NLP models
- **Multi-Input Support:** Extract text from audio, images, and PDF files
- **History Tracking:** Save and review past corrections
- **Firebase Integration:** User authentication and cloud storage
- **Bilingual Support:** Works seamlessly in Arabic and English

---

## Tech Stack

**Language:** Kotlin  
**Architecture:** MVVM with Clean Architecture principles  
**Networking:** Retrofit for API calls  
**Async:** Kotlin Coroutines  
**Backend:** Firebase (Auth, Firestore)  
**AI Models:** GEC, Summarization, STT/TTS, Translation APIs

---

## Building

### Prerequisites

- JDK 17
- Android SDK with platform 37 (Android Studio installs it on first sync)

### Setup

1. Copy `local.properties.example` to `local.properties`.
2. Set `sdk.dir` to your Android SDK path. Android Studio does this for you.
3. Set `OPENROUTER_API_KEY` to an OpenRouter key that is restricted as described below.
   Without it the app still builds, with one warning, but the AI layer reports every
   request as unavailable.
4. `HF_API_TOKEN` is optional. Hugging Face is switched off in the model configuration,
   because it no longer serves inference on a free account. Only the old grammar screen
   still uses this token.

`local.properties` is git-ignored. Never commit a key. In CI or scripts you can pass each
key as an environment variable or Gradle property of the same name instead.

#### Restricting the OpenRouter key

The key ships inside the app, so it must be unable to do anything except use the free daily
allowance (20 requests a minute, 50 a day):

1. Use an OpenRouter account with no payment method and no credits.
2. Create a key used only by this app, with a credit limit of 0.
3. In the privacy settings, turn off free endpoints that publish prompts.
4. Add a guardrail that limits the key to the free models listed in
   [docs/ai-models.md](docs/ai-models.md).
5. Record what you applied, with the date, in that file.

#### AI models

Which model serves each capability is set in two files under
`core/data/src/main/kotlin/com/example/alpha_ai/core/data/ai/config/` and recorded in
[docs/ai-models.md](docs/ai-models.md). See [core/data/README.md](core/data/README.md) for
how to change a model and how to run the opt-in live check.

### Commands

Run these from the repository root. Terminal builds need `JAVA_HOME` to point to a JDK 17. On
Windows with the JDK bundled by Android Studio:

```powershell
$env:JAVA_HOME = "$HOME\.jdks\jbr-17.0.14"
```

| Purpose | Command |
|---|---|
| Build the debug app | `./gradlew assembleDebug` |
| Run all unit tests | `./gradlew testDebugUnitTest test` |
| Run lint | `./gradlew lintDebug` |
| Build and test one module | `./gradlew :core:domain:build` |
| Test the convention plugins | `./gradlew -p build-logic test` |

Every push runs the same commands, plus a secret scan, in GitHub Actions (`.github/workflows/ci.yml`).

### Modules

| Module | Purpose |
|---|---|
| `:app` | The application. Holds all existing code until features are moved out |
| `:core:common` | Pure Kotlin helpers shared by every layer: result types, dispatcher qualifiers |
| `:core:domain` | Use cases, domain models, repository interfaces |
| `:core:data` | Repository implementations, mappers |
| `:core:network` | HTTP clients, provider services, interceptors |
| `:core:datastore` | Local preferences and encrypted session storage |
| `:core:designsystem` | Theme, tokens, reusable components |
| `:core:ui` | Shared screen-level building blocks, MVI base types |
| `:core:testing` | Shared fakes, test dispatchers, test rules |

The `:core` modules are empty for now. Shared build settings live in the convention plugins
under `build-logic/`; all versions live in `gradle/libs.versions.toml`.

### Dependency rules

The build fails when a rule marked "build" is broken.

| Rule | Statement | Enforced by |
|---|---|---|
| DR-1 | A `:core:*` module must not depend on `:app` or on a `:feature:*` module | build |
| DR-2 | A `:feature:*` module must not depend on another `:feature:*` module | build |
| DR-3 | `:core:domain` may depend only on `:core:common` | build |
| DR-4 | `:core:domain` and `:core:common` must not use Android, Compose, Retrofit, or Firebase | build (they are plain Kotlin modules without those libraries) |
| DR-5 | No circular dependency between modules | build (Gradle) |

---

## Current Status

**In Progress:**
- UI/UX redesign for better user experience
- Adding unit tests (JUnit, Mockk)
- Improving network error handling
- Code documentation and cleanup

**Future Improvements:**
- Migrate to Jetpack Compose
- Add offline mode with local caching
- Implement CI/CD pipeline
- Expand language support

---

## License

This project was developed as a graduation project for Benha University.
