# Alpha-AI Constitution

## Core Principles

### I. Clean Architecture

- Code MUST be organized into three layers: **presentation** (Compose UI, ViewModels),
  **domain** (use cases, domain models, repository interfaces), and **data** (repository
  implementations, remote and local data sources, DTOs, mappers).
- Dependencies MUST point inward: presentation → domain ← data. The domain layer MUST be pure
  Kotlin with no Android, Firebase, Retrofit, or Compose imports.
- Presentation MUST reach data only through use cases or repository interfaces defined in
  domain. ViewModels MUST NOT reference Retrofit services, Firebase SDKs, or DTOs.
- DTOs and Firestore documents MUST be mapped to domain models at the data-layer boundary. UI
  state MUST be built from domain models, never from DTOs.
- A use case MUST do one thing and expose a single public `operator fun invoke`. A use case that
  only forwards to a repository with no added logic MAY be omitted.

Rationale: every AI tool in the app (GEC, STT, OCR, translation, summarization) is backed by a
replaceable remote model; isolating the domain keeps the product logic stable when endpoints,
providers, or the UI toolkit change.

### II. Modularization and Hilt Dependency Injection

- The project MUST be split into Gradle modules along these lines: `:app` (entry point,
  navigation host, DI wiring), `:core:*` (for example `designsystem`, `ui`, `common`, `domain`,
  `data`, `network`, `datastore`, `testing`), and `:feature:*` (one module per user-facing
  capability, for example `auth`, `home`, `gec`, `stt`, `ocr`, `translate`, `history`).
- A feature module MUST NOT depend on another feature module. Shared code moves to a `:core`
  module; cross-feature navigation goes through `:app`.
- `:core` modules MUST NOT depend on `:feature` modules or `:app`.
- Modules MUST expose the smallest possible API; implementation classes are `internal`.
- All dependencies MUST be provided through Hilt with constructor injection. Field injection is
  permitted only in Android entry points. Manual singletons, service locators, and `object`
  holders of mutable state MUST NOT be added.
- Coroutine dispatchers MUST be injected, never referenced directly as `Dispatchers.IO` or
  `Dispatchers.Default` inside classes.
- Build configuration shared by modules SHOULD live in convention plugins, and all dependency
  versions MUST be declared in `gradle/libs.versions.toml`.

Rationale: module boundaries make the layering rules compiler-enforced, keep builds incremental,
and let each AI tool be developed and tested in isolation.

### III. MVI with Coroutines and Flow

- Every screen MUST follow MVI with a single contract: an immutable `UiState` data class,
  a sealed `UiIntent` (user actions), and a sealed `UiEffect` (one-off events such as navigation
  or snackbars).
- The ViewModel MUST expose exactly one `StateFlow<UiState>` and one effect stream, and accept
  input through a single `onIntent` entry point. State MUST change only by producing a new
  state from the previous state.
- The UI MUST collect state with lifecycle-aware collection (`collectAsStateWithLifecycle`).
- Asynchronous work MUST use coroutines under structured concurrency in `viewModelScope` or a
  scope tied to a lifecycle. `GlobalScope`, `runBlocking` in production code, and blocking calls
  on the main thread MUST NOT be used.
- Data layers MUST expose streams as cold `Flow` and one-shot operations as `suspend` functions.
  `LiveData` MUST NOT be used in new code.
- Coroutine cancellation MUST be respected: `CancellationException` MUST NOT be swallowed.

Rationale: AI requests are slow and can fail midway; a single immutable state per screen makes
loading, partial results, and errors reproducible and testable.

### IV. Compose-First UI and Design System

- All new and migrated screens MUST be written in Jetpack Compose with Material 3. New XML
  layouts, Data Binding, and View Binding MUST NOT be added.
- Composables MUST be stateless where possible: state is hoisted, screen-level composables
  receive `UiState` and an `onIntent` lambda, and ViewModels are not passed below the
  screen-level composable.
- Colors, typography, shapes, spacing, and reusable components MUST come from
  `:core:designsystem`. Hardcoded colors, text sizes, and spacing values in feature code MUST
  NOT be added.
- Every screen MUST support light and dark themes, both Arabic (RTL) and English (LTR), and
  system font scaling.
- Every screen composable MUST have a `@Preview` covering at least its loading, content, and
  error states.
- State passed to composables MUST be stable or immutable so that recomposition can be skipped;
  lazy lists MUST supply item keys.
- All user-visible text MUST come from string resources with Arabic and English translations.

Rationale: the UI redesign is the main goal of the refactor; a single design system is what
makes the app consistent across its tools rather than a collection of separate screens.

### V. Type-Safe Navigation and App Flow

- Navigation MUST use Navigation Compose with type-safe, serializable route definitions in a
  single-activity architecture. String-built routes and new Activities per screen MUST NOT be
  added.
- Each feature module MUST expose its own navigation graph and route types; `:app` composes
  them into the root graph.
- Navigation MUST be triggered by a `UiEffect` handled at the screen boundary. ViewModels MUST
  NOT hold a `NavController`.
- Arguments passed between destinations MUST be identifiers or small primitives, not full
  objects.
- The start destination MUST be resolved from session state (onboarding seen, signed in) before
  the first frame, so users never see a screen they are immediately redirected away from.
- Every tool flow MUST allow the user to go from input to result in the fewest possible steps,
  and system back MUST behave predictably on every screen.

Rationale: the app is a set of tools reached from one home; a clear, predictable flow matters
more to perceived quality than any single screen.

### VI. Purposeful Motion and UX Quality

- Animation MUST communicate state change, hierarchy, or continuity. Decorative animation that
  delays the user MUST NOT be added.
- Motion durations and easing MUST be defined as tokens in the design system and reused, not
  set per screen.
- Animations MUST target 60 fps on a mid-range device, MUST be interruptible, and MUST honor the
  system "remove animations" setting.
- Every asynchronous operation MUST show explicit loading, empty, error, and success states.
  Operations expected to exceed one second MUST show progress and MUST be cancellable.
- Interactive elements MUST have a touch target of at least 48dp, a content description where
  they have no text, and text contrast meeting WCAG AA.

Rationale: users judge a writing assistant by how fast and trustworthy it feels; motion and
feedback are how the app shows it is working during slow model calls.

### VII. Android Security (NON-NEGOTIABLE)

- Secrets MUST NOT be committed. API keys, keystores, and credentials are read from git-ignored
  local files or CI secrets.
- Any key shipped inside the APK MUST be treated as public. Keys for paid or abusable services
  SHOULD be held behind a backend proxy; where that is not yet possible, the key MUST be
  restricted on the provider side and the exception recorded in the feature plan. This applies
  to the AI provider keys covered by Principle X.
- Auth tokens and persisted credentials MUST be encrypted with a key held in the Android
  Keystore. Passwords MUST NOT be persisted.
- All network traffic MUST use HTTPS. Cleartext traffic MUST be disabled through the network
  security configuration.
- Release builds MUST enable R8 minification and resource shrinking, MUST NOT be debuggable,
  and MUST NOT log HTTP bodies.
- Logs, analytics events, and crash reports MUST NOT contain passwords, tokens, API keys, or
  user content.
- Components MUST declare `android:exported` explicitly and default to `false`. Data received
  through intents and deep links MUST be validated before use.
- Permissions MUST be the minimum required, requested at the point of use with a rationale, and
  the feature MUST degrade gracefully when a permission is denied.
- Files MUST be shared with other apps only through `FileProvider`. Temporary audio, image, and
  document files MUST be stored in app-private storage and deleted after processing.
- Sensitive data MUST be excluded from device backup.

Rationale: the app processes personal accounts and user-authored text, audio, images, and
documents; a leaked key or credential cannot be recalled once published.

### VIII. Resilient Networking and Error Handling

- All HTTP calls MUST go through Retrofit services defined in the data layer, sharing one
  configured OkHttp client per backend with explicit connect, read, and write timeouts.
- Cross-cutting concerns (auth headers, logging, retries) MUST be implemented as OkHttp
  interceptors, not repeated per call.
- Data-layer functions MUST return a typed result that distinguishes success from failure and
  classifies failures (no connection, timeout, HTTP error, invalid response, unknown).
  Exceptions MUST NOT propagate uncaught into presentation.
- Every failure MUST map to a user-visible state with a localized message and, where retrying
  can help, a retry action. Failures MUST NOT be swallowed silently.
- Requests MUST be cancelled when the user leaves the screen or cancels the operation.
- Diagnostics MUST be logged through Timber; `println` and `android.util.Log` MUST NOT be used.

Rationale: every core feature depends on a remote model and unreliable mobile networks are the
normal case, not the exception.

### IX. Tested Business Logic

- Use cases, ViewModels, repositories, and mappers MUST ship with JVM unit tests covering the
  success path and at least one failure path.
- ViewModel tests MUST assert the sequence of emitted `UiState` and `UiEffect` values for a
  given sequence of intents.
- A bug fix MUST include a test that fails without the fix.
- Tests MUST be deterministic: test dispatchers and virtual time replace real delays, and tests
  MUST NOT call live Firebase or model endpoints. Fakes are preferred over mocks; shared fakes
  live in `:core:testing`.
- Compose UI tests SHOULD cover the critical flows (sign-in, grammar correction end to end) and
  are otherwise added when a feature specification asks for them.

Rationale: the refactor replaces working code feature by feature; tests on the logic layers are
the only guard against regressions while the UI and structure both change.

### X. Provider-Agnostic AI Model Integration

- AI capabilities (GEC, translation, summarization, speech-to-text, text-to-speech, OCR) MUST
  be served by hosted inference providers, with OpenRouter and Hugging Face Inference as the
  approved providers. Adding another provider requires a justification in the feature plan.
- Free-tier models MUST be the default choice. A paid model MAY be used only when no free model
  meets the quality bar for that capability, and the cost MUST be recorded in the feature plan.
- Each capability MUST be defined as a domain interface (for example a grammar corrector or a
  translator). Provider-specific code MUST live in the data layer, and replacing a provider or
  model MUST NOT require changes in domain or presentation code.
- Provider base URLs and model identifiers MUST be defined in one configuration source per
  provider, never inline at call sites, and SHOULD be changeable without an app release.
- Model selection MUST be based on a recorded comparison of candidate models on Arabic and
  English samples for that capability. The chosen model and the comparison MUST be stated in
  the feature plan.
- Every capability MUST define a fallback model or provider. Rate limiting, quota exhaustion,
  model cold start, and model withdrawal MUST be handled as classified failures under
  Principle VIII, with automatic fallback where one exists.
- Prompts MUST be versioned constants in the data layer. User content MUST be passed as clearly
  delimited data, never concatenated into instructions.
- Model output MUST be treated as untrusted input: it is parsed and validated against the
  expected shape before reaching domain models, and malformed output is a classified failure.
- Long text generation SHOULD be streamed to the UI.
- The data-use terms of a provider and model MUST be checked before adoption. A model whose
  terms allow training on or retaining user input MUST NOT be used when an alternative of
  comparable quality exists, and otherwise its use MUST be disclosed to the user.
- On-device processing MAY replace a hosted model for a capability when it meets the same
  quality bar.

Rationale: free hosted models change, throttle, and disappear without notice; keeping providers
behind domain interfaces with measured selection and fallbacks lets the app stay free to run
without tying product quality to any single model.

## Technology and Platform Constraints

- **Product scope**: Alpha-AI is an AI writing assistant centered on grammatical error
  correction (GEC) in Arabic and English, supported by speech-to-text, text-to-speech, OCR for
  images and documents, translation, summarization, and history. A new capability MUST serve
  that purpose and MUST be justified in its feature specification.
- **Language and platform**: Kotlin only, Android, `minSdk` 26, `targetSdk` at the level
  currently required by Google Play. Java and Kotlin targets MUST be 17 or later.
- **UI**: Jetpack Compose, Material 3, Navigation Compose.
- **Architecture**: Clean Architecture, MVI, multi-module Gradle build.
- **Async**: Kotlin Coroutines and Flow.
- **Dependency injection**: Hilt, with KSP for annotation processing in new modules.
- **Networking**: Retrofit and OkHttp.
- **AI providers**: OpenRouter and Hugging Face Inference, free-tier models by default, accessed
  through Retrofit under Principles VII, VIII, and X.
- **Backend services**: Firebase Auth and Firestore for accounts and history.
- **Testing**: JUnit, MockK, Turbine, and `kotlinx-coroutines-test`.
- **Dependencies**: a new library MUST be justified in the feature plan and MUST NOT duplicate a
  capability already provided by the stack above.

## Development Workflow and Quality Gates

- Work happens on a feature branch created from `development`. `master` receives changes only
  by merge from `development`.
- Commit messages MUST follow the Conventional Commits format (`feat:`, `fix:`, `refactor:`,
  `docs:`, `test:`, `chore:`).
- Before a change is merged, the debug build, all unit tests, and Android Lint MUST pass with no
  new warnings in changed files.
- Each feature plan MUST include a Constitution Check against the ten Core Principles. Any
  violation MUST be recorded with its justification and the simpler alternative that was
  rejected.
- Each migration change MUST leave the app buildable and its existing flows working. Rewrites
  that span several features in one change MUST NOT be merged.
- Local IDE state, build output, and secret files MUST NOT be committed.

## Governance

- This constitution takes precedence over other project conventions. Where the README or
  existing code conflicts with it, the constitution applies.
- **Amendments**: a change is made by editing this file in a dedicated commit that states what
  changed and why. The project maintainer approves amendments.
- **Versioning**: the constitution uses semantic versioning. MAJOR for removing or redefining a
  principle, MINOR for adding a principle or section or materially expanding guidance, PATCH for
  clarifications and wording.
- **Compliance**: every specification, plan, and code review MUST verify compliance with the
  Core Principles. Complexity beyond what a principle allows MUST be justified in writing.
- **Migration**: code written before version 2.0.0 is not required to comply immediately. XML
  screens and Compose screens MAY coexist during migration. A screen MUST be migrated as a
  whole (UI, state management, navigation, and tests together) when a feature materially changes
  it, and legacy code for that screen MUST be deleted in the same change.
- `README.md` is the runtime guidance document for project status and stack overview.

**Version**: 2.1.0 | **Ratified**: 2026-11-15 | **Last Amended**: 2026-11-16
