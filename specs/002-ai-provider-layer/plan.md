# Implementation Plan: AI Provider Layer

**Branch**: `002-ai-provider-layer` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-ai-provider-layer/spec.md`

## Summary

Build the one shared layer that every AI feature will call. Features ask for a capability
(grammar correction, translation, summarization, speech recognition, image text extraction,
language detection) through an interface in `:core:domain` and get back a result or a
classified failure. Which provider and model did the work is decided by configuration in
`:core:data`, with automatic fallback. No screen is added or changed.

Technical approach, from [research.md](research.md):

- Capability interfaces, the result type, the failure categories, and the input limits live in
  `:core:domain` as pure Kotlin.
- `:core:network` holds one Retrofit service and one OkHttp client per provider (OpenRouter,
  Hugging Face), typed request and response models with kotlinx.serialization, an auth
  interceptor, and a classifier that turns every HTTP, body-level, and transport error into a
  typed provider failure.
- `:core:data` holds the model configuration (one file per provider), versioned prompts, input
  and output validation, the fallback runner with cooldowns and a total time bound, and the
  capability implementations.
- `:core:ui` holds the Arabic and English failure messages and the mapping from a failure to
  its message.
- Tests use fakes and OkHttp's mock web server. No test calls a live provider.

Three facts found during research shape the plan (details in research.md, section 1):

1. **Hugging Face gives free accounts no inference any more.** The project's token returns
   `402`. Decision (project owner, 2026-11-17): the Hugging Face service is built and tested,
   and its models are present in configuration but switched off until the account has credits.
2. **OpenRouter allows 50 free-model requests a day per account**, shared by every user because
   the key ships in the app. Decision (project owner, 2026-11-17): stay on that tier and spend
   nothing. The layer is therefore designed to spend as few requests as possible.
3. **Every working chain is on OpenRouter**, so "fallback on a different provider" (FR-013) is
   met by using a model served by a different upstream host, which is what the requirement
   allows when the second provider has no suitable free model.

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java and Kotlin target 17

**Primary Dependencies**: Retrofit 2.12.0, OkHttp 4.12.1, Hilt 2.60.1 with KSP, Kotlin
Coroutines 1.11.0, Timber 5.0.1 (all already in the catalog). New: kotlinx.serialization JSON
with the Kotlin serialization compiler plugin, Retrofit's kotlinx.serialization converter,
`javax.inject` for dispatcher qualifiers in the pure-Kotlin module, and OkHttp MockWebServer
for tests. Exact versions of the new entries are pinned by the first implementation task from
the official release pages. Justifications are in research.md, section 4.

**Storage**: N/A. Model health (cooldowns) is kept in memory for the life of the process.
Nothing is cached or persisted.

**Testing**: JUnit 4, `kotlinx-coroutines-test`, Turbine, OkHttp MockWebServer, hand-written
fakes in `:core:testing`. MockK is available but fakes are preferred.

**Target Platform**: Android, `minSdk` 26, `compileSdk` 37, `targetSdk` 34

**Project Type**: mobile-app (multi-module Android); this feature is library code only

**Performance Goals**: a text request ends in a result or failure within 30 seconds including
fallbacks; speech and image requests within 60 seconds; offline is reported within 3 seconds;
language detection answers without a network call

**Constraints**: zero usage fees; 50 OpenRouter free-model requests a day and 20 a minute for
the whole app; no screen added or changed; the legacy `ApiManager` path is not touched; no
provider key or user content in logs; domain module stays free of Android, Retrofit, and
serialization types

**Scale/Scope**: 6 capability interfaces, 2 provider services, 5 touched modules
(`:core:domain`, `:core:network`, `:core:data`, `:core:ui`, `:core:testing`) plus DI wiring in
`:app`, about 45 new Kotlin files and about 25 test files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Clean Architecture | Pass | Capability interfaces and domain models in `:core:domain` (Kotlin JVM, so no Android or Retrofit import is possible). DTOs stay `internal` to `:core:network` and are mapped before they leave it. Forwarding-only use cases are omitted, as the principle allows |
| II. Modularization and Hilt DI | Pass | Code goes into the existing `:core:*` modules; implementations are `internal` and bound with Hilt; dispatchers are injected through qualifiers; all new versions go in the catalog. No singleton `object` holds mutable state: the health tracker is a Hilt `@Singleton` class |
| III. MVI with Coroutines and Flow | Pass (data side) | One-shot calls are `suspend`, streamed results are cold `Flow`, `CancellationException` is always rethrown. No ViewModel is added |
| IV. Compose-First UI | Pass | No UI is added. Failure messages are string resources in Arabic and English |
| V. Type-Safe Navigation | N/A | No navigation change |
| VI. Motion and UX Quality | Pass (enabling) | Every request is cancellable and bounded in time, and every failure states whether retry helps, so later screens can meet this principle |
| VII. Android Security | Pass, with one recorded exception | Keys come from git-ignored `local.properties` or CI secrets. **Exception**: the OpenRouter key ships in the APK. It is treated as public and restricted on the provider side (see Complexity Tracking and research.md, section 6). HTTPS only, enforced by a configuration rule and test on every base URL; the app-wide network security configuration stays with spec 015, as spec 001 recorded. The logging interceptor exists only in the debug source set, redacts `Authorization`, and never logs bodies. Diagnostics carry no user content |
| VIII. Resilient Networking | Pass | Retrofit services in the data side, one OkHttp client per backend with explicit timeouts, auth as an interceptor, typed classified results, cancellation through structured concurrency, Timber for diagnostics. The legacy `ApiManager` keeps its `BODY` logging until its screens migrate (pre-2.0.0 code under the Migration rule; not changed here because FR-029 forbids it) |
| IX. Tested Business Logic | Pass | Every implementation, mapper, validator, and the fallback runner ship with JVM tests for success and failure paths, using virtual time and a mock web server. Shared fakes go in `:core:testing` |
| X. Provider-Agnostic AI | Pass, with two conditions | Domain interfaces per capability; provider code in data and network; one configuration file per provider; versioned prompts with user content sent as separate, delimited data; output validated before it reaches domain. **Condition 1**: the measured model comparison cannot be run during planning (no OpenRouter key exists yet), so the candidates and the method are fixed here and the measurement is the first implementation phase; chains in this plan are provisional until then. **Condition 2**: speech recognition has no working free fallback today (see Complexity Tracking) |
| Technology constraints | Pass, with one addition | kotlinx.serialization is added for typed, validated JSON. It replaces Gson for new code and does not duplicate it in any new module (research.md, section 4) |
| Workflow and quality gates | Pass | Feature branch from `development`, Conventional Commits, build, tests, and lint gate every push through the CI added in spec 001 |

**Result**: gate passes. Exceptions and conditions are justified in Complexity Tracking.

**Post-design re-check (after Phase 1)**: unchanged. The design in
[data-model.md](data-model.md) and [contracts/](contracts/) adds no dependency between
modules beyond those allowed by spec 001's rules DR-1 to DR-5, keeps `:core:domain` free of
framework types, and introduces no further exception.

## Project Structure

### Documentation (this feature)

```text
specs/002-ai-provider-layer/
├── plan.md              # This file
├── research.md          # Phase 0: findings, decisions, alternatives
├── data-model.md        # Phase 1: entities, failure mapping, fallback rules
├── quickstart.md        # Phase 1: how to validate the feature
├── contracts/
│   ├── capability-api.md     # What features call (`:core:domain`)
│   ├── provider-api.md       # What `:core:network` offers `:core:data`, and the wire formats
│   └── model-configuration.md # How models and chains are configured
├── checklists/
│   └── requirements.md
└── tasks.md             # Created by /speckit-tasks
```

### Source Code (repository root)

Package root is `com.example.alpha_ai.core.<module>`. Everything is new unless marked.

```text
core/domain/src/main/kotlin/.../core/domain/ai/
├── Language.kt, DetectedLanguage.kt
├── AiResult.kt, AiFailure.kt, InvalidInputReason.kt
├── AiLimits.kt                     # input limits per capability
├── AudioClip.kt, ImageInput.kt, TextStreamEvent.kt
└── GrammarCorrector.kt, Translator.kt, Summarizer.kt,
    SpeechTranscriber.kt, ImageTextExtractor.kt, LanguageDetector.kt

core/common/src/main/kotlin/.../core/common/
└── Dispatchers.kt                  # @IoDispatcher, @DefaultDispatcher qualifiers

core/network/
├── build.gradle.kts                # CHANGED: Retrofit, OkHttp, serialization, Timber
└── src/
    ├── main/AndroidManifest.xml    # INTERNET, ACCESS_NETWORK_STATE
    ├── main/kotlin/.../core/network/
    │   ├── AiProvider.kt, AiCredentials.kt, ConnectivityChecker.kt
    │   ├── ProviderResult.kt, ProviderFailure.kt
    │   ├── ChatRequest.kt, ChatReply.kt, ChatStreamEvent.kt   # public, provider-neutral
    │   ├── OpenRouterClient.kt, HuggingFaceClient.kt          # public interfaces
    │   ├── internal/               # services, DTOs, interceptors, classifiers, SSE reader
    │   └── di/NetworkModule.kt
    ├── debug/kotlin/.../DebugLogging.kt    # redacted, no bodies
    ├── release/kotlin/.../DebugLogging.kt  # no-op
    └── test/kotlin/                # MockWebServer tests

core/data/
├── build.gradle.kts                # CHANGED: coroutines, Timber
└── src/
    ├── main/kotlin/.../core/data/ai/
    │   ├── config/                 # OpenRouterModels.kt, HuggingFaceModels.kt,
    │   │                           # ModelChoice, ModelConfigSource, ModelConfigResolver
    │   ├── prompt/Prompts.kt       # versioned constants
    │   ├── validate/               # InputValidator, OutputValidator, ScriptAnalyzer
    │   ├── fallback/               # FallbackRunner, ModelHealthTracker, FailureSelector
    │   ├── engine/                 # ChatEngine, HfSpeechEngine, streaming engine
    │   ├── Default*.kt             # the six capability implementations
    │   └── di/AiModule.kt, DispatchersModule.kt
    └── test/kotlin/                # unit tests; `live/` holds the opt-in comparison runner

core/ui/src/main/
├── res/values/strings_ai_failures.xml
├── res/values-ar/strings_ai_failures.xml
└── kotlin/.../core/ui/ai/AiFailureMessages.kt

core/testing/src/main/kotlin/.../core/testing/ai/
└── Fake capability implementations, TestClock, fake clients

app/
├── build.gradle.kts                # CHANGED: OPENROUTER_API_KEY, core module dependencies
└── src/main/java/.../di/AiCredentialsModule.kt   # supplies keys from BuildConfig

docs/ai-models.md                   # NEW: chosen models, fallbacks, comparison record
gradle/libs.versions.toml           # CHANGED: new entries
local.properties.example            # CHANGED: OPENROUTER_API_KEY
.github/workflows/ci.yml            # unchanged; unit tests need no key
```

**Structure Decision**: the feature lives entirely in the `:core:*` modules created by spec
001, following their allowed dependencies (`:core:data` → `:core:domain`, `:core:network`;
`:core:ui` → `:core:domain`). `:app` only supplies credentials and aggregates the Hilt
modules. No `:feature:*` module is created. The legacy code under
`app/.../data/remote/api/` is left exactly as it is.

## Implementation Sequence

Each phase ends with the build, unit tests, and lint passing, and is committed separately.

The order matches [tasks.md](tasks.md).

1. **Setup**: catalog entries, serialization plugin, module dependencies, `OPENROUTER_API_KEY`
   wiring and the missing-key warning, `local.properties.example`, dispatcher qualifiers.
2. **Contracts**: domain interfaces, result and failure types, limits; public network types.
3. **One model per capability** (US1): OpenRouter DTOs, service, client, auth interceptor,
   error classifier, and connectivity check, with MockWebServer tests for every status and
   body shape in contracts/provider-api.md; prompts, input and output validation, chat engine;
   the six capabilities; Hilt wiring and the `:app` credentials module; the restricted
   OpenRouter key; the opt-in live runner.
4. **Fallback** (US2): health tracker, fallback runner, failure selection, total time bound,
   offline short-circuit.
5. **Failure messages** (US3): Arabic and English strings and the mapper in `:core:ui`, with
   the test that checks both languages cover every category and reason.
6. **Configuration and model choice** (US4): Hugging Face service and Whisper engine (tested
   against the mock server only, switched off), per-provider model files, resolver with
   validation and built-in defaults.
   **Model comparison (gate for Principle X)**: run the live runner against the candidates in
   research.md section 3, check each chosen endpoint's data-use terms, and record the results
   and final chains in `docs/ai-models.md`. It needs a working client, so it cannot come
   first; it starts as soon as the live runner works, takes two days of quota at 50 requests
   a day, and must be complete before the feature merges.
7. **Streaming** (FR-007, SHOULD): server-sent events reader and the streaming variants of
   the three text capabilities. This phase can be dropped without affecting the others.
8. **Guard and polish** (US5): confirm the legacy flow is untouched, fakes in
   `:core:testing`, README and `ROADMAP.md`.

## Risks

| Risk | Mitigation |
|---|---|
| 50 requests a day is shared by all users and by development. A few users exhaust it | When OpenRouter reports the daily limit, the provider is skipped until the reset time it gives, so users get "service busy" at once instead of waiting. Language detection uses no request. No automatic retry of the same model. The credential source is an interface, so a later feature can supply a per-user key or a proxy without touching this layer |
| The free model list changes without notice (17 free models on 2026-11-17, all newer than a year) | Chains end with OpenRouter's free router, which picks any available free model; a withdrawn model is skipped for the rest of the session and logged; changing a model is a one-line edit |
| Free endpoints may retain or train on user input | Data-use terms are checked per endpoint in phase 2 before a model is adopted (FR-010); the account's privacy setting is set to exclude free endpoints that publish prompts; anything unavoidable is recorded in `docs/ai-models.md` for the disclosure that spec 015 must make |
| Only one free model accepts audio | See Complexity Tracking. Speech quality and audio format support are verified in phase 2 before phase 8 starts |
| Output of a small free model is wrapped in commentary or is in the wrong language | Output validation rejects it and triggers the fallback; prompts ask for bare output; tests cover each wrapping case from the spec's edge cases |
| The existing grammar screen already fails against live Hugging Face (`402`) | Out of scope by FR-029 and FR-030. It is recorded in `ROADMAP.md`; spec 006 moves that screen onto this layer |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| The OpenRouter key ships inside the APK (Principle VII prefers a backend proxy) | A proxy needs hosting, and the product must cost nothing to run | The principle allows this when the key is restricted on the provider side and the exception is recorded. Restriction: the key has a credit limit of zero so it cannot spend money, a guardrail limits it to the configured free models, and the account holds no payment method. Credentials are read through an interface so a proxy can replace the key later |
| Language detection runs on the device only and has no fallback (FR-008, FR-013) | Arabic and English use different scripts, so counting letters by script is exact, instant, works offline, and costs none of the 50 daily requests | A hosted detector would spend a request before every correction and summary, halving the app's daily capacity, for no gain in accuracy between these two languages. Principle X permits on-device processing at the same quality bar. Known limit: other Latin-script languages are reported as English; other scripts are reported as unsupported |
| Speech recognition has no working free fallback (FR-013, Principle X) | Exactly one free hosted model accepts audio. The Hugging Face Whisper entry is built and configured as the fallback but is switched off because that provider is no longer free | An on-device recognizer as fallback was considered: Android's recognizer accepts recorded audio only from API 33 and its Arabic support varies by device, which cannot be judged without the recording screen. Spec 008 (speech to text) already has "hosted or on-device" as an open decision and will add it there. Until then a failure of the one model is reported as a classified failure |
| Fallbacks are on the same provider as the first choice (Principle X asks for a fallback model or provider; FR-013 prefers another provider) | Hugging Face has no free inference | FR-013 requires another provider only "where a second provider offers a suitable free model". Each fallback is served by a different upstream host from the first choice, so one host's outage does not take down a capability |
| The model comparison is recorded after planning, in `docs/ai-models.md` (Principle X says "stated in the feature plan") | Running it needs an OpenRouter key, which does not exist yet, and two days of quota | Guessing results now would be a fabricated record. The candidates, samples, and scoring method are fixed in research.md section 3, the comparison is implementation phase 2, and this plan links to its result file |
