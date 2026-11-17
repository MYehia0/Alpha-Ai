---

description: "Task list for spec 002: AI Provider Layer"
---

# Tasks: AI Provider Layer

**Input**: Design documents from `/specs/002-ai-provider-layer/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/capability-api.md,
contracts/provider-api.md, contracts/model-configuration.md, quickstart.md

**Tests**: Required. FR-031 and Principle IX ask for automated tests of every success path,
failure category, and fallback path, none of which may call a live provider. Test tasks are
written next to the code they cover. Tasks marked **MANUAL** need a person, an OpenRouter key,
or a device.

**Organization**: Tasks are grouped by user story so each story can be implemented and checked
on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: The user story the task belongs to (US1 to US5)
- Paths are relative to the repository root

## Conventions for every task

- Path shorthands, each followed by the package path `com/example/alpha_ai/core/<module>/`:
  - `DOMAIN` = `core/domain/src/main/kotlin/com/example/alpha_ai/core/domain/ai`
  - `NET` = `core/network/src/main/kotlin/com/example/alpha_ai/core/network`
  - `DATA` = `core/data/src/main/kotlin/com/example/alpha_ai/core/data/ai`
  - `UI` = `core/ui/src/main/kotlin/com/example/alpha_ai/core/ui/ai`
  - Tests use the same path under `src/test/kotlin/`, written `DOMAIN-TEST`, `NET-TEST`,
    `DATA-TEST`, `UI-TEST`.
- Terminal builds need a JDK 17: in PowerShell, `$env:JAVA_HOME = "$HOME\.jdks\jbr-17.0.14"`.
- Commit after each task or logical group, using Conventional Commits. The app must build and
  `./gradlew testDebugUnitTest` must pass at every commit.
- Implementation classes are `internal`. Only the types named in the contracts are public.
- Never catch `CancellationException` without rethrowing it. Never reference `Dispatchers.IO`
  or `Dispatchers.Default` directly; inject them with the qualifiers from T004.
- No test may open a real network connection. Use fakes, `MockWebServer`, and
  `kotlinx-coroutines-test` virtual time.
- Do not change anything under `app/src/main/java/com/example/alpha_ai/data/remote/`, any
  Activity, Fragment, ViewModel, or layout (FR-029, FR-030).
- **Before starting**: the working tree has an uncommitted change in `gradle/libs.versions.toml`
  (`agp` `9.5.1` → `9.3.0-alpha04`) that is not part of this feature. Revert it unless the
  project owner confirms it is intended.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencies, keys, and module wiring that every later task needs.

- [X] T001 Add to `gradle/libs.versions.toml`, pinning each version from its official release page: `kotlinx-serialization-json`, `retrofit-converter-kotlinx-serialization` (use `version.ref = "retrofit"`), `okhttp-mockwebserver` (use `version.ref = "okhttp"`), `javax-inject` (`javax.inject:javax.inject:1`), and the plugin `kotlin-serialization` (`org.jetbrains.kotlin.plugin.serialization`, `version.ref = "kotlin"`). Change no existing entry
- [X] T002 Update module build files: `core/common/build.gradle.kts` (api `javax-inject`), `core/domain/build.gradle.kts` (api `kotlinx-coroutines-core`; test: `junit`, `kotlinx-coroutines-test`), `core/network/build.gradle.kts` (serialization plugin; `retrofit-core`, the kotlinx converter, `okhttp-core`, `okhttp-logging` as `debugImplementation`, `kotlinx-serialization-json`, `kotlinx-coroutines-core`, `timber`; test: `junit`, `okhttp-mockwebserver`, `kotlinx-coroutines-test`, `turbine`), `core/data/build.gradle.kts` (`kotlinx-coroutines-core`, `timber`; test: `junit`, `kotlinx-coroutines-test`, `turbine`, `projects.core.testing`), `core/ui/build.gradle.kts` (test: `junit`). Confirm `./gradlew assembleDebug` passes
- [X] T003 In `app/build.gradle.kts`, read `OPENROUTER_API_KEY` exactly as `HF_API_TOKEN` is read (Gradle property, then environment variable, then `local.properties`), add `buildConfigField("String", "OPENROUTER_API_KEY", ...)`, and extend the missing-key warning to name whichever key is blank. Do not add the core module dependencies to `:app` yet (T026 does it, when the Hilt graph is complete). Add `OPENROUTER_API_KEY=sk-or-your_key_here` with a one-line comment to `local.properties.example`
- [X] T004 [P] Create `core/common/src/main/kotlin/com/example/alpha_ai/core/common/Dispatchers.kt` with two `@Qualifier @Retention(AnnotationRetention.BINARY)` annotations: `IoDispatcher` and `DefaultDispatcher`
- [X] T005 [P] Create `core/network/src/main/AndroidManifest.xml` declaring `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The public contracts between modules. Every story builds on these types.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T006 Create the domain result types in `DOMAIN/`: `Language.kt` (enum `ARABIC`, `ENGLISH`), `DetectedLanguage.kt` (enum `ARABIC`, `ENGLISH`, `UNSUPPORTED`), `InvalidInputReason.kt` (enum `EMPTY`, `TOO_SHORT`, `TOO_LONG`, `TOO_LARGE`, `UNSUPPORTED_LANGUAGE`, `UNSUPPORTED_FORMAT`), `AiResult.kt` (sealed: `Success(value: T)`, `Failure(failure: AiFailure)`), and `AiFailure.kt` (sealed with `val canRetry: Boolean`: `NoConnection` true, `TimedOut` true, `ServiceBusy(retryAfterSeconds: Long?)` true, `ServiceUnavailable` false, `InvalidInput(reason: InvalidInputReason)` false, `InvalidResponse` true, `Unknown` true), per data-model.md sections 1 and 2. No failure may carry a status code, provider, model, or raw message
- [X] T007 [P] Create in `DOMAIN/`: `AudioClip.kt` (`bytes: ByteArray`, `mimeType: String`, `durationMillis: Long`), `ImageInput.kt` (`bytes: ByteArray`, `mimeType: String`), `TextStreamEvent.kt` (sealed: `Delta(text)`, `Restarted`, `Completed(text)`, `Failed(failure)`), and `AiLimits.kt` with these constants from data-model.md section 3: grammar "at most 5,000 characters"; translation "at most 5,000 characters"; summarization "200 to 20,000 characters"; speech "at most 60 seconds and 5 MB" with MIME types `audio/wav`, `audio/mpeg`, `audio/mp4`, `audio/ogg`, `audio/flac`; image "at most 4 MB" with MIME types `image/jpeg`, `image/png`, `image/webp`
- [X] T008 Create the six capability interfaces in `DOMAIN/` with exactly the signatures in contracts/capability-api.md: `GrammarCorrector.kt`, `Translator.kt`, `Summarizer.kt`, `SpeechTranscriber.kt`, `ImageTextExtractor.kt`, `LanguageDetector.kt`. Add KDoc stating guarantees C-1 to C-3
- [X] T009 Create the public network types in `NET/` per data-model.md section 4 and contracts/provider-api.md part 1: `AiProvider.kt` (enum `OPENROUTER`, `HUGGING_FACE`), `AiCredentials.kt`, `ConnectivityChecker.kt`, `ProviderResult.kt`, `ProviderFailure.kt` (sealed: `NoConnection`, `Timeout`, `RateLimited(retryAfterSeconds: Long?)`, `QuotaExhausted(resetAtEpochMillis: Long?)`, `Loading(estimateSeconds: Long?)`, `Overloaded`, `ModelNotFound`, `AccessRejected`, `NoCredits`, `ServerError`, `RequestRejected`, `ContentBlocked`, `MalformedResponse`, `Unknown`), `ChatRequest.kt` (with sealed `ContentPart`: `Text`, `Image(base64, mimeType)`, `Audio(base64, format)`), `ChatReply.kt` (`text`, `servedBy: String?`, `truncated: Boolean`), `ChatStreamEvent.kt` (sealed: `Delta(text)`, `Done(truncated)`, `Error(failure)`), `OpenRouterClient.kt`, `HuggingFaceClient.kt`

**Checkpoint**: `./gradlew :core:domain:build :core:network:assembleDebug` passes.

---

## Phase 3: User Story 1 - A feature asks for an AI result and gets it (Priority: P1) 🎯 MVP

**Goal**: Each of the six capabilities returns a cleaned, checked result or a classified
failure, using the first configured model on OpenRouter.

**Independent Test**: `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest`
passes with a success test in Arabic and in English for each capability; then the MANUAL live
check in T028 returns a correct result for each.

### Network (OpenRouter)

- [X] T010 [P] [US1] Create the `internal` wire models in `NET/internal/ChatDtos.kt` with `@Serializable`, matching contracts/provider-api.md part 2: request (`model`, `messages`, `max_tokens`, `temperature`, `stream`), message content as either a plain string or a list of parts (`text`, `image_url` with a `data:` URL, `input_audio` with `data` and `format`), response (`model`, `choices[].message.content`, `choices[].finish_reason`), stream chunk (`choices[].delta.content`), and the error envelope (`error.code`, `error.message`, `error.metadata.error_type`). Configure one `Json { ignoreUnknownKeys = true; explicitNulls = false }` in `NET/internal/NetworkJson.kt`
- [X] T011 [US1] Create `NET/internal/OpenRouterService.kt` (Retrofit: `@POST("chat/completions")` returning `Response<ResponseBody>`), `NET/internal/AuthInterceptor.kt` (adds `Authorization: Bearer <token>` from `AiCredentials` for a given `AiProvider`), and `NET/internal/HttpClients.kt` building one shared base `OkHttpClient` with "connect 5 s, read 30 s, write 30 s" and one derived client per provider (guarantee N-9). Create `core/network/src/debug/kotlin/com/example/alpha_ai/core/network/internal/DebugLogging.kt` (an `HttpLoggingInterceptor` at `BASIC` level with `redactHeader("Authorization")`) and `core/network/src/release/kotlin/.../internal/DebugLogging.kt` (returns no interceptor), per N-8
- [X] T012 [US1] Create `NET/internal/OpenRouterErrorClassifier.kt` mapping status, headers, and error body to `ProviderFailure` exactly as the OpenRouter classification table in contracts/provider-api.md (`error_type` first, then status; 429 with `X-RateLimit-Remaining: 0` → `QuotaExhausted(X-RateLimit-Reset)`), and `NET/internal/TransportErrorClassifier.kt` for the transport table (`UnknownHostException`, `ConnectException`, `NoRouteToHostException` → `NoConnection`; `SocketTimeoutException` → `Timeout`; other `IOException` and `SerializationException` → `MalformedResponse`; else `Unknown`; `CancellationException` is rethrown)
- [X] T013 [US1] Create `NET/internal/DefaultOpenRouterClient.kt` implementing `chat`: return `AccessRejected` without sending when the key is blank (N-5); treat a `200` whose body has an `error` object as a failure (N-3); return `MalformedResponse` for no choices or blank text and set `truncated` when `finish_reason` is `length` (N-4); make no retry (N-7). Implement `chatStream` for now by calling `chat` and emitting one `Delta` and `Done`, or `Error`; T042 replaces it
- [X] T014 [P] [US1] Create `NET/internal/AndroidConnectivityChecker.kt` using `ConnectivityManager`: online when the active network has `NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`
- [X] T015 [US1] Create `NET/di/NetworkModule.kt` (Hilt, `SingletonComponent`) providing the base client, the OpenRouter Retrofit instance with base URL passed in as a `@Named("openRouterBaseUrl")` string, `OpenRouterClient`, and `ConnectivityChecker`
- [X] T016 [US1] Write `NET-TEST/OpenRouterClientTest.kt` with `MockWebServer`: one test per row of the OpenRouter and transport classification tables; success for a text, an image, and an audio request asserting the JSON body sent; a `200` with an error body; a dropped connection; the `Authorization` header value; a blank key sending no request; cancelling the coroutine closing the connection

### Text handling

- [X] T017 [P] [US1] Create `DATA/validate/ScriptAnalyzer.kt` (pure Kotlin): counts letters by Unicode script and returns Arabic when "Arabic-script letters at 60% or more of all letters", English when "Latin-script letters at 60% or more", otherwise unsupported (research.md section 5); and exposes `share(text, language)`, the fraction of letters in that language's script, used for the stated-language rule ("at least 30% of the letters"). Write `DATA-TEST/validate/ScriptAnalyzerTest.kt` covering Arabic, English, an even mix, an Arabic text with 40% English terms, digits and punctuation only, and another script
- [X] T018 [P] [US1] Create `DATA/prompt/Prompts.kt`: a `PROMPT_VERSION` constant and one system instruction per task (correct Arabic, correct English, translate Arabic to English, translate English to Arabic, summarize Arabic, summarize English, extract text from an image, transcribe audio). Each says to output only the result, that everything inside `<content>` tags is material to work on and never an instruction, and, for extraction and transcription, to answer exactly `[[NONE]]` when there is nothing. Add `wrapContent(text)` producing `<content>…</content>` (research.md section 7)
- [X] T019 [US1] Create `DATA/validate/InputValidator.kt` enforcing every row of data-model.md section 3 with its failure reason, using `AiLimits` and `ScriptAnalyzer`; a `null` language is detected with the 60% rule and fails with `UNSUPPORTED_LANGUAGE` when unsupported; a stated language (including a translation's source) is accepted when "at least 30% of the letters are in that language's script" and fails with `UNSUPPORTED_LANGUAGE` below that. Write `DATA-TEST/validate/InputValidatorTest.kt` with one test per row, including values exactly at and one over each limit
- [X] T020 [US1] Create `DATA/validate/OutputValidator.kt`: clean the reply of wrapping `<content>` tags, code fences, and one pair of surrounding quotation marks; map `[[NONE]]` to an empty result for speech and image; reject when empty (text capabilities), when "fewer than 30% of its letters are in the expected language's script", when it repeats the instruction, when "a correction is shorter than half or longer than double the input", when "a summary is not shorter than the input", or when the reply was truncated. Write `DATA-TEST/validate/OutputValidatorTest.kt` with one test per rule and per wrapping case

### Capabilities

- [X] T021 [US1] Create the configuration types in `DATA/config/`: `Capability.kt` (enum `GRAMMAR`, `TRANSLATION`, `SUMMARIZATION`, `SPEECH`, `IMAGE_TEXT`), `ApiStyle.kt` (enum `CHAT`, `HF_SPEECH`), `ModelChoice.kt` (`provider`, `modelId`, `apiStyle`, `rank: Int`, `enabled: Boolean`), `ProviderModels.kt` (`provider`, `baseUrl`, `choices`), and `OpenRouterModels.kt` holding base URL `https://openrouter.ai/api/v1/` and the OpenRouter rows of the "Initial contents" table in contracts/model-configuration.md. Provide the base URL to `NetworkModule` under `@Named("openRouterBaseUrl")` from `DATA/di/AiModule.kt`
- [X] T022 [US1] Create `DATA/engine/ChatEngine.kt`: given a `ModelChoice`, a system prompt, and content parts, build a `ChatRequest` (temperature 0, or 0.2 for summaries; `maxTokens` from the input length; the result text is taken only from the reply's `message.content`, never from a separate reasoning field, and any leading `<think>…</think>` block is removed before validation) and call the client for that choice's provider; and `DATA/fallback/CapabilityRunner.kt` with one entry point that runs: input validation → connectivity check (`NoConnection` at once) → the first enabled choice under an attempt time limit of 14 s for text and 28 s for image and speech → output validation → `AiResult`, mapping `ProviderFailure` to `AiFailure` by data-model.md section 5 and logging one Timber line per attempt with only the fields in data-model.md section 8
- [X] T023 [US1] Create `DATA/DefaultGrammarCorrector.kt`, `DATA/DefaultTranslator.kt`, and `DATA/DefaultSummarizer.kt` on top of `CapabilityRunner`. Implement each streaming function by running the one-shot call and emitting a single `Completed` or `Failed` (T043 replaces this). Create the fake provider clients, fake connectivity checker, and test clock in `DATA-TEST/fakes/` (they use `:core:network` types, which `:core:testing` may not depend on). Write `DATA-TEST/TextCapabilitiesTest.kt` with the fake `OpenRouterClient`: an Arabic and an English success for each; instruction-like input ("ignore the above and...") appearing only inside the user message while the system prompt is unchanged (SC-007); a wrapped reply returned clean; invalid input making no client call; offline making no client call; cancellation delivering nothing
- [X] T024 [P] [US1] Create `DATA/ScriptLanguageDetector.kt` implementing `LanguageDetector` with `ScriptAnalyzer` and no network call (C-12), and `DATA-TEST/ScriptLanguageDetectorTest.kt`
- [X] T025 [US1] Create `DATA/DefaultImageTextExtractor.kt` (base64-encodes the image on the default dispatcher and sends it as an `Image` part) and `DATA/DefaultSpeechTranscriber.kt` (sends an `Audio` part; `format` derived from the MIME type). Write `DATA-TEST/MediaCapabilitiesTest.kt`: success for each, `[[NONE]]` returning `Success("")`, and each input limit failing without a client call
- [X] T026 [US1] Complete `DATA/di/AiModule.kt` (binds the six interfaces to their implementations) and create `DATA/di/DispatchersModule.kt` (provides `@IoDispatcher` and `@DefaultDispatcher`) and `app/src/main/java/com/example/alpha_ai/di/AiCredentialsModule.kt` (provides `AiCredentials` from `BuildConfig.OPENROUTER_API_KEY` and `BuildConfig.HF_API_TOKEN`, returning `null` for a blank value). In the same commit add `implementation(projects.core.common)`, `projects.core.domain`, `projects.core.network`, and `projects.core.data` to `app/build.gradle.kts`, so the app's Hilt graph gains the new modules only once every binding they need exists. Confirm `./gradlew assembleDebug` passes with Hilt resolving the graph
- [ ] T027 [US1] **MANUAL** (project owner; Principle VII requires the shipped key to be restricted on the provider side): create an OpenRouter account with no payment method and no credits; create a key used only by this app with a credit limit of 0; in privacy settings turn off free endpoints that publish prompts; add a guardrail limiting the key to the free models in `DATA/config/OpenRouterModels.kt`; put the key in `local.properties` as `OPENROUTER_API_KEY`. Record the date and each setting applied under a "Key restriction" heading in a new `docs/ai-models.md` (T041 adds the comparison to the same file). Do not run any live step before this is done
- [ ] T028 [US1] Create the opt-in live runner in `DATA-TEST/live/LiveCapabilityCheck.kt` with sample files under `core/data/src/test/resources/ai-samples/` (3 Arabic and 3 English per text capability with expected output, one of each containing instruction-like text; 2 printed-text images and 2 short voice clips per language): skipped unless Gradle property `liveAi` is set and `OPENROUTER_API_KEY` is present; `-Pcapability=` selects one capability; `-Pcompare` runs every candidate from research.md section 3; writes a results table to `core/data/build/reports/ai-live/`. Pass the properties through in `core/data/build.gradle.kts`. Then **MANUAL**: run it for `grammar` (6 requests) and confirm correct results (SC-001)

**Checkpoint**: US1 works end to end with one model per capability.

---

## Phase 4: User Story 2 - The app keeps working when a model is unavailable (Priority: P1)

**Goal**: A failed first choice is followed automatically by the next, within a total time
bound, skipping models known to be failing.

**Independent Test**: `./gradlew :core:data:testDebugUnitTest --tests "*Fallback*"` passes:
with the first choice failing in each of the five ways, every capability that has a fallback
returns the fallback's result.

- [X] T029 [P] [US2] Create `DATA/fallback/ModelHealthTracker.kt` (`@Singleton`, injected clock, thread-safe): records a `ProviderFailure` for a provider and model and answers "is this choice skipped now, and why", using the cooldown table in research.md section 9 (rate limited: `Retry-After` else 60 s; daily limit: provider-wide until reset else 1 hour; loading or overloaded: estimate else 30 s; timeout or server error: 30 s; not found: model until restart; access rejected or no credits: provider until restart; invalid output: none, then 5 minutes after 3 in a row; success clears the model). Write `DATA-TEST/fallback/ModelHealthTrackerTest.kt` with a test clock, one test per row
- [X] T030 [P] [US2] Create `DATA/fallback/FailureSelector.kt` returning one `AiFailure` from a list of attempt outcomes and skip reasons by the ranking `ServiceBusy` > `TimedOut` > `ServiceUnavailable` > `InvalidResponse` > `Unknown`, with `retryAfterSeconds` set to the shortest known wait (data-model.md section 5). Write `DATA-TEST/fallback/FailureSelectorTest.kt`
- [X] T031 [US2] Create `DATA/fallback/FallbackRunner.kt` implementing the loop in data-model.md section 7: iterate enabled choices by rank; skip choices the tracker marks; give each attempt the smaller of its limit (14 s text, 28 s image and speech) and the time left; total limit 30 s text, 60 s image and speech; start no attempt with less than 3 s left; stop at once on `NoConnection`; treat failed output validation as `MalformedResponse`; record every outcome in the tracker; return the `FailureSelector` result when nothing is left; rethrow cancellation and start nothing further; log one request line with `result` and `totalMs`. Replace the single-choice step in `DATA/fallback/CapabilityRunner.kt` with it
- [X] T032 [US2] Write `DATA-TEST/fallback/FallbackRunnerTest.kt` with fake clients and virtual time, one test per acceptance scenario of US2: rate limited, loading or overloaded, model not found, too slow, malformed output, all choices failing (asserting the selected category), offline (asserting one client call at most and no fallback), and a model rate-limited a moment ago being skipped on the next request and tried again after its cooldown. Add: cancellation during a fallback attempt makes no further call; a chain of slow models ends with `TimedOut` at the total limit; daily limit spent returns `ServiceBusy` on the next request with no client call; two concurrent requests both complete; with a recording Timber tree planted, each attempt line and request line carries exactly the fields in data-model.md section 8 and no line contains the input text, the output text, or a key (FR-026, FR-028)
- [X] T033 [US2] Write `DATA-TEST/fallback/CapabilityFallbackTest.kt`: for grammar, translation, summarization, and image text extraction, a first choice failing by each of the five causes returns the second choice's result (SC-002); for speech, the single enabled choice failing returns a classified failure

**Checkpoint**: US1 and US2 together are the working core of the layer.

---

## Phase 5: User Story 3 - Failures are explained to the user in their language (Priority: P2)

**Goal**: Every failure maps to a plain message in Arabic and English, with a correct retry
flag and no technical detail.

**Independent Test**: `./gradlew :core:ui:testDebugUnitTest` passes, proving every category
and reason has a message in both languages.

- [ ] T034 [P] [US3] Create `core/ui/src/main/res/values/strings_ai_failures.xml` and `core/ui/src/main/res/values-ar/strings_ai_failures.xml` with the 12 keys listed in contracts/capability-api.md ("Failure messages"). Each message is one plain sentence saying what happened and, where retry helps, what to do; none contains a number, a provider name, or a model name
- [ ] T035 [US3] Create `UI/AiFailureMessages.kt` with `@StringRes fun AiFailure.messageRes(): Int`, using a `when` with no `else` branch over `AiFailure` and over `InvalidInputReason`
- [ ] T036 [US3] Write `UI-TEST/AiFailureMessagesTest.kt`: every `AiFailure` subtype and every `InvalidInputReason` maps to a resource; parsing both XML files from `core/ui/src/main/res/` shows the same 12 keys in each, no empty value, and no value containing a digit or any of `OpenRouter`, `Hugging Face`, `HTTP`, or a configured model id (SC-003). Write `DATA-TEST/fallback/FailureMappingTest.kt`: each `ProviderFailure` maps to the `AiFailure` and `canRetry` given in data-model.md sections 2 and 5, and a rejected key produces `ServiceUnavailable` with a log line that does not contain the key (US3 scenario 4)

**Checkpoint**: US3 complete; a screen can show any failure in the user's language.

---

## Phase 6: User Story 4 - A developer changes a model without touching feature code (Priority: P2)

**Goal**: Models and their order are set in one file per provider, validated at start-up,
documented with the comparison that justified them, with Hugging Face present and switched
off.

**Independent Test**: quickstart.md section 4: change the first-choice grammar model in
`OpenRouterModels.kt`, run the unit tests, and see one file modified and all tests passing.

- [ ] T037 [US4] Add the Hugging Face wire handling (in the same commit, provide `@Named("huggingFaceBaseUrl")` = `https://router.huggingface.co/` from `DATA/di/AiModule.kt` so the app keeps building; T038 moves the value into `HuggingFaceModels.kt`): `NET/internal/HuggingFaceService.kt` (`@POST("v1/chat/completions")` and `@POST("hf-inference/models/{model}")` with a raw audio body and `Content-Type` header), `NET/internal/HuggingFaceErrorClassifier.kt` implementing the Hugging Face table in contracts/provider-api.md (including both error body shapes), `NET/internal/DefaultHuggingFaceClient.kt` (`chat`, `chatStream` as in T013, `transcribe` reading `{"text": ...}`), and its bindings in `NET/di/NetworkModule.kt` with `@Named("huggingFaceBaseUrl")`. Write `NET-TEST/HuggingFaceClientTest.kt`: one test per table row, chat success, transcription success asserting the raw body and content type, and a blank token sending nothing
- [ ] T038 [US4] Create `DATA/config/HuggingFaceModels.kt` (base URL `https://router.huggingface.co/`; the Hugging Face rows of the "Initial contents" table, all `enabled = false`) and `DATA/engine/HfSpeechEngine.kt` handling `ApiStyle.HF_SPEECH` through `HuggingFaceClient.transcribe`; route by `apiStyle` and provider in `DATA/engine/ChatEngine.kt` and `DATA/DefaultSpeechTranscriber.kt`. Add a test in `DATA-TEST/MediaCapabilitiesTest.kt` that an enabled `HF_SPEECH` choice is used as the speech fallback
- [ ] T039 [US4] Create `DATA/config/ModelConfigSource.kt` (interface from contracts/model-configuration.md, plus a `NoOverrides` implementation bound in `DATA/di/AiModule.kt`) and `DATA/config/ModelConfigResolver.kt` applying the resolution rules in data-model.md section 6 and rules M-1 to M-4: "`modelId` is not blank"; "`rank` is positive and unique within a capability across both providers"; "an OpenRouter `modelId` ends in `:free` or is `openrouter/free`"; "`HF_SPEECH` only on Hugging Face"; and M-8 "every provider base URL starts with `https://`" (FR-027). An invalid or missing override keeps the built-in entry and is logged once with capability, provider, and rule; an empty chain falls back to the built-in chain. Make `FallbackRunner` take its chain from the resolver
- [ ] T040 [US4] Write `DATA-TEST/config/ModelConfigResolverTest.kt`: the built-in files pass M-1 to M-4 and M-8 with nothing dropped; an override with an `http://` base URL is rejected; M-5 holds ("every capability has at least two enabled choices, except speech recognition"); a fake override source can move a capability's first choice to the other provider with no other change (US4 scenarios 1 and 2); a malformed and a missing override each fall back and are logged (scenario 3); nothing throws
- [ ] T041 [US4] **MANUAL** (needs the OpenRouter key; about 70 requests, so two days of quota): run the comparison from T028 with `-Pcompare` for each capability, read each candidate endpoint's data-use terms on its OpenRouter model page, and add to `docs/ai-models.md`, per capability: the chain in order, each model's upstream host, the terms found and the date checked, the comparison table (samples, scores, timings), and the scoring method from research.md section 3. Update `OpenRouterModels.kt` to the measured order, keeping rule M-6 (the first two choices on different upstream hosts), confirm the audio formats in `AiLimits` against the chosen speech model, and confirm for each chosen model that its replies carry no reasoning text in `message.content` (if one does, record in `docs/ai-models.md` the request setting that turns it off and add it to `ChatEngine`)

**Checkpoint**: US4 complete; the Principle X comparison gate is closed.

---

## Phase 7: Streaming (FR-007, SHOULD)

**Purpose**: Deliver long text progressively. This phase can be skipped without affecting any
user story; the streaming functions already satisfy their contract by emitting one terminal
event.

- [ ] T042 [P] Create `NET/internal/SseReader.kt` reading a streamed `ResponseBody` line by line: `data:` lines are parsed as chunks, lines starting with `:` are ignored, `data: [DONE]` ends the stream, and a chunk with a top-level `error` becomes `ChatStreamEvent.Error` through the classifier. Add `@Streaming` variants to both services and replace `chatStream` in `DefaultOpenRouterClient.kt` and `DefaultHuggingFaceClient.kt`. Write `NET-TEST/ChatStreamTest.kt`: deltas in order, comment lines ignored, normal end, mid-stream error, dropped connection, and cancellation closing the connection
- [ ] T043 Create `DATA/fallback/StreamingFallbackRunner.kt` implementing the streaming variant in data-model.md section 7 (`Delta` as text arrives; `Restarted` when an attempt fails after any `Delta` and another choice takes over; exactly one terminal `Completed` with the cleaned and checked full text, or `Failed`) and use it in the streaming functions of `DefaultGrammarCorrector.kt`, `DefaultTranslator.kt`, and `DefaultSummarizer.kt`. Write `DATA-TEST/fallback/StreamingFallbackRunnerTest.kt` with Turbine: plain success, restart on mid-stream failure, final check failing with no choice left, and cancellation

---

## Phase 8: User Story 5 - Screens not yet migrated keep working (Priority: P3)

**Goal**: Prove the legacy path was not touched.

**Independent Test**: quickstart.md section 5.

- [ ] T044 [US5] Run `git diff --stat development -- app/src/main` and confirm the only entry is `app/src/main/java/com/example/alpha_ai/di/AiCredentialsModule.kt`; revert anything else. Then **MANUAL**: install the debug build, open the grammar correction screen, and confirm it behaves as on `development` (including the current `402` failure from Hugging Face, which this feature does not fix)

---

## Phase 9: Polish & Cross-Cutting Concerns

- [ ] T045 [P] Create fakes in `core/testing/src/main/kotlin/com/example/alpha_ai/core/testing/ai/`: `FakeGrammarCorrector.kt`, `FakeTranslator.kt`, `FakeSummarizer.kt`, `FakeSpeechTranscriber.kt`, `FakeImageTextExtractor.kt`, `FakeLanguageDetector.kt`, each configurable with the result to return and recording the calls received. Provider client fakes stay in `DATA-TEST/fakes/`
- [ ] T046 [P] Update `README.md` (OpenRouter key setup and restriction steps from research.md section 6, how to run the live check, where models are configured), `core/network/README.md`, `core/data/README.md`, and `core/domain/README.md` (what each now contains)
- [ ] T047 Run the full gate `./gradlew assembleDebug testDebugUnitTest lintDebug` and fix any new lint warning in changed files. Then **MANUAL** quickstart.md section 6: capture `adb logcat` during a live call and confirm neither key prefix (`sk-or-`, `hf_`) nor the input text appears, and that `core/network/src/release/` holds the no-op logging file (SC-008)
- [ ] T048 Update `ROADMAP.md`: mark spec 002 done in the progress table with the date; record that Hugging Face is switched off because it is no longer free, that the app runs on 50 OpenRouter requests a day, and that the legacy grammar screen cannot get a live result until spec 006; tick "Every AI request can be cancelled and has a working fallback" only if T041 confirmed a second free audio path, otherwise leave it unticked with a note about speech

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: needs Phase 1. Blocks every story.
- **US1 (Phase 3)**: needs Phase 2.
- **US2 (Phase 4)**: needs US1 (it replaces the single-choice step in `CapabilityRunner`).
- **US3 (Phase 5)**: T034 to T036's UI part needs only Phase 2; `FailureMappingTest` needs US1.
- **US4 (Phase 6)**: T037 needs US1's network and DI tasks (T015, T021, T026); T038 to T040
  need US2; T041 needs T028 and the key.
- **Merge gate**: T041 (the measured model comparison and terms check) must be complete
  before this feature merges into `development`, whatever scope is chosen. Until then the
  chains are provisional and Principle X is not met.
- **Streaming (Phase 7)**: needs US2 and T037.
- **US5 (Phase 8)** and **Polish (Phase 9)**: after all chosen phases.

### Within US1

T010 → T011 → T012 → T013 → T015 → T016 (network). T017 and T018 first, then T019 and T020
(text handling). T021 → T022 → T023, T025 → T026 → T027 → T028 (capabilities). T014 and T024 any time.

### Parallel opportunities

- Phase 1: T004 and T005 alongside T001 to T003.
- Phase 2: T007 with T006; T009 with T006 to T008 (different module).
- US1: the network track (T010 to T016) and the text-handling track (T017 to T020) are in
  different modules and can run side by side; T014 and T024 are independent.
- US2: T029 and T030 together.
- US3's T034 can start as soon as Phase 2 is done.
- T041 runs over two days alongside any other work.
- Polish: T045 and T046 together.

### Parallel example: User Story 1

```text
Track A (core/network): T010 → T011 → T012 → T013 → T015 → T016
Track B (core/data):    T017, T018 → T019, T020 → T021 → T022
Join:                   T023, T024, T025 → T026 → T027 → T028
```

---

## Implementation Strategy

### MVP first

1. Phases 1 and 2.
2. US1 (Phase 3): every capability answers with one model. Validate with the unit tests and
   the live check for grammar.
3. US2 (Phase 4): fallback. US1 and US2 are both P1 and together are the smallest working
   version. Merging still needs T041 (see "Merge gate" above).

### Incremental delivery

4. US3: failure messages.
5. US4: Hugging Face (off), configuration resolver, and the measured comparison. Start T041
   as soon as T028 works, because it needs two days of quota.
6. Streaming, if wanted.
7. US5 and Polish, then merge into `development`.

### Notes

- 48 tasks. The roadmap suggests splitting a spec above about 40; Phase 7 (streaming, 2 tasks)
  is the part to defer to a later spec if the feature needs to shrink.
- Every MANUAL live step spends requests from the 50-a-day allowance: T028 about 6, T041
  about 70, T047 one or two.
