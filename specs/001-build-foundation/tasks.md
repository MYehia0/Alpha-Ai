---

description: "Task list for spec 001: Build Foundation and Module Structure"
---

# Tasks: Build Foundation and Module Structure

**Input**: Design documents from `/specs/001-build-foundation/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/build-contracts.md, quickstart.md

**Tests**: The spec does not ask for TDD. The only test tasks are the Gradle TestKit tests for
the dependency rules, which plan.md and research.md (R6) require. Behavior preservation is
checked by manual smoke tests, marked **MANUAL**: these need a person with a device or
emulator.

**Organization**: Tasks are grouped by user story so each story can be implemented and checked
on its own.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: The user story the task belongs to (US1 to US4)
- Paths are relative to the repository root

## Conventions for every task

- Terminal builds need a JDK 17: in PowerShell, `$env:JAVA_HOME = "$HOME\.jdks\jbr-17.0.14"`.
- Commit after each task or logical group, using Conventional Commits (`chore:`, `build:`,
  `ci:`, `docs:`). The app must build at every commit.
- Do not move or rewrite source files under `app/src/` unless a task says so or the compiler
  requires it. Keep any such fix minimal and behavior-neutral, and list it in the commit body.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Clean the repository and record the starting point before any upgrade.

- [X] T001 Record the baseline: run `./gradlew assembleDebug testDebugUnitTest` on the current code and note the result and any failing tests in the commit body of T002, so later failures can be told apart from existing ones
- [X] T002 Stop tracking IDE state: run `git rm -r --cached .idea`, then in `.gitignore` replace the individual `/.idea/...` lines with a single `.idea/` line and remove the duplicate `.gradle` and `local.properties` lines. Files must stay on disk
- [X] T003 [P] Clean `gradle.properties`: remove `-XX:MaxPermSize=512m` from `org.gradle.jvmargs`, remove `kapt.incremental.apt=false` and `kotlin.compiler.execution.strategy=in-process`, and add `org.gradle.caching=true` and `org.gradle.parallel=true`. Do not enable the configuration cache yet (T022 does it)
- [X] T004 [P] Create `local.properties.example` listing `sdk.dir=<path to your Android SDK>` and `HF_API_TOKEN=hf_your_token_here` with a one-line comment on each; confirm it is not ignored with `git check-ignore local.properties.example` (expect no output)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Put every dependency in the version catalog at its current version, so the
upgrades that follow are one-line changes.

**⚠️ CRITICAL**: No user story work starts until this phase is complete.

- [X] T005 Move every dependency into `gradle/libs.versions.toml` without changing any version: add `[versions]`, `[libraries]`, and `[plugins]` entries for everything declared inline in `app/build.gradle.kts` (Hilt, security-crypto, Retrofit, converter-gson, OkHttp, logging-interceptor, coroutines, lifecycle, activity-ktx, Apache POI x3, Firebase BoM and its artifacts, firebase-ui-auth, Glide, Timber, MaterialProgressBar) and for the plugins in `build.gradle.kts` (android-application, android-library, kotlin-android, kotlin-kapt, google-services, hilt). Use the naming rules in `specs/001-build-foundation/contracts/build-contracts.md` section 2. Add `compileSdk`, `minSdk`, `targetSdk` as version entries
- [X] T006 Rewrite `build.gradle.kts` (root) to declare plugins only through `alias(libs.plugins.…) apply false`; delete the `buildscript { }` block and its classpath entries, which also removes the AGP 8.2.2 / 8.3.0 mismatch; replace the `clean` task's `rootProject.buildDir` with `rootProject.layout.buildDirectory`
- [X] T007 Rewrite the `plugins { }` and `dependencies { }` blocks of `app/build.gradle.kts` to use only `libs.…` references and read SDK levels from the catalog; remove every `val …Version` and inline coordinate. Verify with `./gradlew assembleDebug` and with the search in `specs/001-build-foundation/quickstart.md` section 1 (expect no version strings)

**Checkpoint**: The app builds exactly as before, with all versions in one file.

---

## Phase 3: User Story 1 - The project builds on a current, supported toolchain (Priority: P1) 🎯 MVP

**Goal**: The app builds with the current Gradle, AGP, and Kotlin on Java 17 and behaves as it
did before.

**Independent Test**: On a clean checkout with a valid `local.properties`,
`./gradlew clean assembleDebug installDebug` succeeds, and every existing flow behaves as
before (quickstart.md section 1).

### Toolchain step 1: last Gradle 8 / AGP 8, Kotlin 2, Java 17

- [X] T008 [US1] Look up the current stable versions on the official release pages (AGP, Gradle, Kotlin, KSP, Hilt, Compose BOM, Firebase BoM, and the `compileSdk`/`targetSdk` level Google Play currently requires) and write them as a table at the end of `specs/001-build-foundation/research.md` under a new heading "Pinned versions", with two columns: the last 8.x-line set for step 1 and the 9.x-line set for step 2. Respect the floors in research.md R1
- [X] T009 [US1] Upgrade the Gradle wrapper to the step 1 version by running `./gradlew wrapper --gradle-version <8.x>` twice, updating `gradle/wrapper/gradle-wrapper.properties`
- [X] T010 [US1] In `gradle/libs.versions.toml` set AGP, Kotlin, and `compileSdk`/`targetSdk` to the step 1 versions, and add the `ksp` plugin and version
- [X] T011 [US1] In `app/build.gradle.kts` set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_17` and replace `kotlinOptions { jvmTarget = "1.8" }` with `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }`
- [X] T012 [US1] Move Hilt to KSP: upgrade Hilt to the pinned version in `gradle/libs.versions.toml`, apply the `ksp` plugin in `app/build.gradle.kts`, and change `kapt(libs.hilt.compiler)` to `ksp(libs.hilt.compiler)`. Keep the `kotlin-kapt` plugin applied, because Data Binding still needs it (research.md R3)
- [X] T013 [US1] Remove the Glide annotation processor line (`kapt(… glide:compiler …)`) from `app/build.gradle.kts` and its catalog entry; first confirm with a search of `app/src` that no `@GlideModule` exists
- [X] T014 [US1] Upgrade the Firebase BoM in `gradle/libs.versions.toml` and replace `firebase-analytics-ktx` and `firebase-firestore-ktx` with `firebase-analytics` and `firebase-firestore` in the catalog and `app/build.gradle.kts`. Upgrade `firebase-ui-auth` only if the build fails without it
- [X] T015 [US1] Bump Retrofit, OkHttp, coroutines, lifecycle, and AndroidX core libraries to the current stable patch of their existing major line in `gradle/libs.versions.toml`; leave Apache POI, SwipeLayout, MaterialProgressBar, and security-crypto unchanged (research.md R4)
- [X] T016 [US1] Run `./gradlew clean assembleDebug testDebugUnitTest` and fix compile errors raised by Kotlin 2 or the new libraries in `app/src/` with the smallest behavior-neutral change; list each changed file in the commit body
- [ ] T017 [US1] **MANUAL** Install the step 1 build (`./gradlew installDebug`) and walk through every flow listed in `specs/001-build-foundation/quickstart.md` section 1; record the result in the commit body or stop and report any difference

### Toolchain step 2: Gradle 9 / AGP 9, built-in Kotlin

- [X] T018 [US1] Upgrade the Gradle wrapper to the step 2 version (`./gradlew wrapper --gradle-version <9.x>`, run twice) and set AGP, Kotlin, and KSP to the step 2 versions in `gradle/libs.versions.toml`
- [X] T019 [US1] Adopt built-in Kotlin: remove the `kotlin-android` plugin from `build.gradle.kts`, `app/build.gradle.kts`, and the catalog; replace `kotlin-kapt` with `com.android.legacy-kapt` (same version as AGP) in the catalog and `app/build.gradle.kts`; keep the `kapt { correctErrorTypes = true }` block only if the new plugin accepts it
- [X] T020 [US1] Verify Data Binding under `legacy-kapt`: run `./gradlew clean assembleDebug` and confirm the generated binding classes and `app/src/main/java/com/example/alpha_ai/core/utils/BindingAdapter.kt` compile. If it fails, apply the fallback in research.md R3 (opt `:app` out of built-in Kotlin with the documented `gradle.properties` flag and restore `kotlin-android` and `kotlin-kapt` for `:app`), and record the outcome in the Risks table of `specs/001-build-foundation/plan.md`
- [X] T021 [US1] Resolve every remaining AGP 9 or Gradle 9 build error and deprecation that fails the build in `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, and `gradle.properties`; do not add compatibility opt-out flags other than the one allowed in T020
- [X] T022 [US1] Add `org.gradle.configuration-cache=true` to `gradle.properties`, run `./gradlew assembleDebug` twice, and confirm the second run reuses the configuration cache. If a plugin is incompatible, remove the flag and note which plugin in research.md
- [ ] T023 [US1] **MANUAL** Install the step 2 build and repeat the full flow walk-through from quickstart.md section 1; record the result or stop and report any difference

**Checkpoint**: The app builds on the current toolchain and behaves as before. This is the MVP.

---

## Phase 4: User Story 2 - Secrets stay out of source control (Priority: P1)

**Goal**: Secrets come only from git-ignored files or the environment, a missing secret is
explained, and no tracked file holds a credential.

**Independent Test**: `git grep -nE "hf_[A-Za-z0-9]{30,}"` prints nothing; building without
`HF_API_TOKEN` succeeds with one warning naming the key and the file (quickstart.md section 2).

- [X] T024 [US2] In `app/build.gradle.kts` resolve `HF_API_TOKEN` in this order: `providers.gradleProperty("HF_API_TOKEN")`, then `providers.environmentVariable("HF_API_TOKEN")`, then `local.properties`, then empty string; keep exposing it as `BuildConfig.HF_API_TOKEN`. The value must not be printed
- [X] T025 [US2] In `app/build.gradle.kts` log exactly one warning when the token resolves to empty: "HF_API_TOKEN is not set. Add it to local.properties (see local.properties.example). AI features will fail at run time." The build must continue
- [X] T026 [P] [US2] Add `.gitleaks.toml` at the repository root that extends the default rules and adds a rule for Hugging Face tokens (`hf_[A-Za-z0-9]{30,}`), with `local.properties.example` allowlisted for its placeholder
- [X] T027 [US2] Run the checks in `specs/001-build-foundation/quickstart.md` section 2: the `git grep` for tokens, `git check-ignore` for both secret files, and the missing-token build. Fix anything found

**Checkpoint**: User Stories 1 and 2 both hold.

---

## Phase 5: User Story 3 - Every push is checked automatically (Priority: P2)

**Goal**: Each push and pull request gets one pass or fail result covering the secret scan,
debug build, unit tests, and lint.

**Independent Test**: A normal push gets a green `CI` run within 15 minutes; a compile error, a
failing test, a new lint error, and a fake token each produce a red run that names the failing
step (quickstart.md section 3).

- [X] T028 [US3] Configure lint in `app/build.gradle.kts`: `lint { baseline = file("lint-baseline.xml"); abortOnError = true; checkReleaseBuilds = false }`, then generate `app/lint-baseline.xml` by running `./gradlew :app:updateLintBaseline` and commit it. Confirm `./gradlew lintDebug` passes
- [X] T029 [US3] Create `.github/workflows/ci.yml` per `specs/001-build-foundation/contracts/build-contracts.md` section 5: name `CI`; triggers `push` (all branches) and `pull_request` into `development` and `master`; `concurrency` per ref with `cancel-in-progress: true`; `permissions: contents: read`; `timeout-minutes: 30`; steps: checkout with full history, set up Temurin JDK 17, `gradle/actions/setup-gradle`, gitleaks scan using `.gitleaks.toml`, then `./gradlew assembleDebug`, `./gradlew testDebugUnitTest test`, `./gradlew lintDebug` as separate named steps. No secret may be required
- [X] T030 [US3] In `.github/workflows/ci.yml` add an `if: failure()` step that uploads `**/build/reports/tests/` and `**/build/reports/lint-results-*.html` as an artifact
- [X] T031 [US3] Ensure `gradlew` is executable in git for the Linux runner: `git update-index --chmod=+x gradlew`
- [X] T032 [US3] Push the branch and confirm the `CI` run is green with `gh run list --branch 001-build-foundation` and `gh run view`; note the duration against the 15-minute target. Fix the workflow until it passes
- [X] T033 [US3] Prove the failure paths on a throwaway branch `ci-failure-check`: push, in turn, a compile error, a failing unit test in `app/src/test/`, a new lint error, and a fake `hf_` token in a tracked file; confirm each run fails on the expected step and that reports are attached. Open a draft pull request into `development` to confirm the failed status is shown, then close it and delete the branch locally and on the remote
- [X] T034 [US3] Read the log of a passing run and confirm no secret value appears in it

**Results (2026-11-16)**: the `CI` run on `001-build-foundation` passed in 7 min 26 s. Four
throwaway branches each failed on the expected step: compile error on "Build the debug app",
failing test on "Run unit tests", `NewApi` lint error on "Run lint", fake `hf_` token on "Scan
for secrets"; reports were attached where a Gradle step had run. The branches were deleted.
Not done: the draft pull request in T033 (no authenticated GitHub client on this machine).
T034: the workflow is given no secret, so no secret value can appear in its log. One run
failed once at the build step and passed that step on a re-run with identical code, which
points to a transient runner or download problem.

**Checkpoint**: Every push is checked.

---

## Phase 6: User Story 4 - A module layout with enforced dependency rules (Priority: P2)

**Goal**: Eight empty shared modules exist, each builds on its own, shared build settings live
in convention plugins, and the build rejects forbidden dependencies.

**Independent Test**: `./gradlew projects` lists `:app` and eight `:core:*` modules;
`./gradlew :core:domain:build` succeeds without compiling `:app`; adding a forbidden dependency
fails with the rule ID in the message (quickstart.md section 4).

### Convention plugins

- [X] T035 [US4] Create the included build: `build-logic/settings.gradle.kts` (reusing the root catalog via `versionCatalogs { create("libs") { from(files("../gradle/libs.versions.toml")) } }`) and `build-logic/convention/build.gradle.kts` (`kotlin-dsl`, Java 17, `compileOnly` on the AGP, Kotlin, KSP, and Compose compiler Gradle plugins, TestKit and JUnit for tests, and a `gradlePlugin { }` block registering the five plugin IDs from contracts/build-contracts.md section 1). Add `includeBuild("build-logic")` inside `pluginManagement { }` in `settings.gradle.kts`
- [X] T036 [US4] Write the dependency-rule check in `build-logic/convention/src/main/kotlin/DependencyRules.kt`: after project evaluation, inspect every `ProjectDependency` in all configurations and throw a `GradleException` using the message format in contracts/build-contracts.md section 1 when a rule is broken. Rules, quoted from data-model.md: DR-1 "A `:core:*` module must not depend on `:app` or on a `:feature:*` module"; DR-2 "A `:feature:*` module must not depend on another `:feature:*` module"; DR-3 "`:core:domain` may depend only on `:core:common`"
- [X] T037 [P] [US4] Write `build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt` (ID `alpha.android.application`): applies the Android application plugin and sets `compileSdk`, `minSdk` 26, `targetSdk` from the catalog, Java 17, Kotlin JVM target 17, and the shared lint settings
- [X] T038 [P] [US4] Write `build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt` (ID `alpha.android.library`): Android library plugin, the same SDK, Java, and lint settings with `abortOnError = true` and no baseline, and a call to the dependency-rule check
- [X] T039 [P] [US4] Write `build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt` (ID `alpha.android.compose`): applies the Compose compiler plugin, sets `buildFeatures.compose = true`, and adds the Compose BOM, `material3`, `ui-tooling-preview`, and debug `ui-tooling` from the catalog. Add those entries and `activity-compose` to `gradle/libs.versions.toml`
- [X] T040 [P] [US4] Write `build-logic/convention/src/main/kotlin/JvmLibraryConventionPlugin.kt` (ID `alpha.jvm.library`): Kotlin JVM plugin, Java toolchain and JVM target 17, and a call to the dependency-rule check
- [X] T041 [P] [US4] Write `build-logic/convention/src/main/kotlin/HiltConventionPlugin.kt` (ID `alpha.hilt`): applies the Hilt and KSP plugins and adds `hilt-android` (implementation) and `hilt-compiler` (ksp) from the catalog
- [X] T042 [US4] Write Gradle TestKit tests in `build-logic/convention/src/test/kotlin/DependencyRulesTest.kt`: one test per rule (DR-1, DR-2, DR-3) that builds a temporary multi-project fixture with the violation and asserts the build fails with the rule ID in the output, plus one test that an allowed graph (`:core:data` → `:core:domain` → `:core:common`) configures successfully. Run `./gradlew -p build-logic test`
- [X] T043 [US4] Switch `app/build.gradle.kts` to `alpha.android.application`, `alpha.android.compose`, and `alpha.hilt`; delete the settings and dependencies the plugins now provide (SDK levels, Java and Kotlin targets, Hilt lines), keeping the `:app`-only parts (namespace, application ID, version, build features for Data Binding and View Binding, BuildConfig field, lint baseline, legacy-kapt). Verify with `./gradlew assembleDebug`

### Core modules

- [X] T044 [US4] Add the test libraries from the constitution to `gradle/libs.versions.toml` (MockK, Turbine, `kotlinx-coroutines-test`; JUnit is already there) and register the eight modules in `settings.gradle.kts` with `include(":core:common", ":core:domain", ":core:data", ":core:network", ":core:datastore", ":core:designsystem", ":core:ui", ":core:testing")`
- [X] T045 [P] [US4] Create `core/common/build.gradle.kts` (plugin `alpha.jvm.library`, no project dependencies), an empty `core/common/src/main/kotlin/` source set, and `core/common/README.md` with purpose "Pure Kotlin helpers shared by every layer: result types, dispatcher qualifiers" and "May depend on: nothing in the project"
- [X] T046 [P] [US4] Create `core/domain/build.gradle.kts` (plugin `alpha.jvm.library`, `implementation(project(":core:common"))`), an empty source set, and `core/domain/README.md` with purpose "Use cases, domain models, repository interfaces" and "May depend on: `:core:common`"
- [X] T047 [P] [US4] Create `core/network/build.gradle.kts` (plugins `alpha.android.library`, `alpha.hilt`; namespace `com.example.alpha_ai.core.network`; depends on `:core:common`) and `core/network/README.md` with purpose "HTTP clients, provider services, interceptors"
- [X] T048 [P] [US4] Create `core/datastore/build.gradle.kts` (plugins `alpha.android.library`, `alpha.hilt`; namespace `com.example.alpha_ai.core.datastore`; depends on `:core:common`) and `core/datastore/README.md` with purpose "Local preferences and encrypted session storage"
- [X] T049 [P] [US4] Create `core/data/build.gradle.kts` (plugins `alpha.android.library`, `alpha.hilt`; namespace `com.example.alpha_ai.core.data`; depends on `:core:common`, `:core:domain`, `:core:network`, `:core:datastore`) and `core/data/README.md` with purpose "Repository implementations, mappers"
- [X] T050 [P] [US4] Create `core/designsystem/build.gradle.kts` (plugins `alpha.android.library`, `alpha.android.compose`; namespace `com.example.alpha_ai.core.designsystem`; no project dependencies) and `core/designsystem/README.md` with purpose "Theme, tokens, reusable components"
- [X] T051 [P] [US4] Create `core/ui/build.gradle.kts` (plugins `alpha.android.library`, `alpha.android.compose`; namespace `com.example.alpha_ai.core.ui`; depends on `:core:common`, `:core:domain`, `:core:designsystem`) and `core/ui/README.md` with purpose "Shared screen-level building blocks, MVI base types"
- [X] T052 [P] [US4] Create `core/testing/build.gradle.kts` (plugin `alpha.android.library`; namespace `com.example.alpha_ai.core.testing`; depends on `:core:common`, `:core:domain`; exposes JUnit, MockK, Turbine, and `kotlinx-coroutines-test` with `api`) and `core/testing/README.md` with purpose "Shared fakes, test dispatchers, test rules"
- [X] T053 [US4] Run `./gradlew projects`, `./gradlew :core:domain:build`, `./gradlew :core:designsystem:assembleDebug`, and `./gradlew build -x :app:build` for all core modules; fix any module that fails when empty (for example add a minimal `src/main/AndroidManifest.xml` if the tooling requires one)
- [X] T054 [US4] Run the three scratch-edit rule checks in `specs/001-build-foundation/quickstart.md` section 4 (DR-1 in `core/data`, DR-3 in `core/domain`, an Android import in `core/domain`), confirm each fails as described, and revert each edit
- [X] T055 [US4] Add `./gradlew -p build-logic test` as a named step in `.github/workflows/ci.yml` before the build step, push, and confirm the `CI` run is green and still within 15 minutes

**Checkpoint**: All four user stories hold.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and the final gate.

- [X] T056 [P] Update `README.md` with a "Building" section: prerequisites (JDK 17, Android SDK), copying `local.properties.example`, the commands from contracts/build-contracts.md section 4, setting `JAVA_HOME` for terminal builds on Windows, the module list with one line each, and the dependency rules DR-1 to DR-5
- [X] T057 Run the final gate from `specs/001-build-foundation/quickstart.md` section 5: `./gradlew assembleDebug testDebugUnitTest test lintDebug`, and the version-string search from section 1
- [ ] T058 **MANUAL** Final regression: on a clean checkout, time the path from clone to the app running (target under 15 minutes, SC-001) and walk through every flow in quickstart.md section 1 one last time
- [X] T059 Update `ROADMAP.md`: set spec 001 to "Done" in the status table with the completion date, and tick the "No secret in the repository or its history that is still valid" line only if it is true
- [ ] T060 Open a pull request from `001-build-foundation` into `development` with a summary of the toolchain versions pinned, any source files changed under `app/src/`, and the manual test results; merge only when `CI` is green

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies
- **Foundational (Phase 2)**: after Setup; blocks every story
- **US1 (Phase 3)**: after Foundational
- **US2 (Phase 4)**: after Foundational. Independent of US1, but both edit `app/build.gradle.kts`, so run it after US1 to avoid conflicts
- **US3 (Phase 5)**: after US1, because the workflow must run on the upgraded toolchain; uses `.gitleaks.toml` from US2 (T026)
- **US4 (Phase 6)**: after US1, because the convention plugins target AGP 9. T055 needs the workflow from US3
- **Polish (Phase 7)**: after all stories

### Within the stories

- US1: T008 → T009 to T016 in order → T017 → T018 to T022 in order → T023. The two toolchain steps must not be merged into one commit
- US2: T024 → T025; T026 in parallel; T027 last
- US3: T028 → T029 → T030, T031 → T032 → T033 → T034
- US4: T035 → T036 → T037 to T041 in parallel → T042 → T043 → T044 → T045 to T052 in parallel → T053 → T054 → T055

### Parallel Opportunities

- T003 and T004 in Setup
- T026 alongside T024 and T025
- T037 to T041: five convention plugins in five files
- T045 to T052: eight modules in eight folders
- T056 alongside T057

---

## Parallel Example: User Story 4

```text
# After T036, write the five convention plugins together:
Task: "AndroidApplicationConventionPlugin.kt"   (T037)
Task: "AndroidLibraryConventionPlugin.kt"       (T038)
Task: "AndroidComposeConventionPlugin.kt"       (T039)
Task: "JvmLibraryConventionPlugin.kt"           (T040)
Task: "HiltConventionPlugin.kt"                 (T041)

# After T044, create the eight modules together:
Task: "core/common"  (T045)    Task: "core/domain"       (T046)
Task: "core/network" (T047)    Task: "core/datastore"    (T048)
Task: "core/data"    (T049)    Task: "core/designsystem" (T050)
Task: "core/ui"      (T051)    Task: "core/testing"      (T052)
```

---

## Implementation Strategy

### MVP First (User Story 1 only)

1. Phase 1: Setup
2. Phase 2: Foundational
3. Phase 3: US1, stopping at T017 and T023 for the manual checks
4. **Stop and validate**: the app builds on the current toolchain and behaves as before

### Incremental Delivery

1. Setup + Foundational → all versions in one file
2. US1 → current toolchain (MVP)
3. US2 → secrets handled and explained
4. US3 → every push checked
5. US4 → module layout with enforced rules
6. Polish → documentation, final gate, pull request

### Stop points that need a person

- T017, T023, T058: manual walk-through on a device or emulator
- T020: if Data Binding fails under `legacy-kapt`, the fallback changes the plan and should be
  reported before continuing
- T060: merging the pull request

---

## Notes

- [P] tasks touch different files and have no dependency on an unfinished task
- Each story can be checked on its own with the matching section of quickstart.md
- Avoid: bundling the two toolchain steps, upgrading libraries beyond research.md R4, moving
  code out of `:app`, and adding opt-out flags to silence AGP 9 errors
