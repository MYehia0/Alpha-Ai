# Research: Build Foundation and Module Structure

**Date**: 2026-11-16 | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

## Current state (measured in the repository)

| Item | Now |
|---|---|
| Gradle wrapper | 8.4 |
| Android Gradle Plugin (AGP) | 8.2.2 on the classpath, 8.3.0 in the catalog (inconsistent) |
| Kotlin | 1.9.22 |
| Java target | 1.8 |
| Annotation processing | kapt for Hilt 2.48, Glide 4.16, and Data Binding |
| SDK levels | `compileSdk` 34, `targetSdk` 34, `minSdk` 26 |
| Modules | `:app` only |
| Source | 56 Kotlin files, 0 Java files, 30 XML layouts, 2 test files |
| UI | Data Binding and View Binding, one Kotlin `@BindingAdapter` file |
| Version catalog | 9 libraries; about 25 more are declared inline in `app/build.gradle.kts` |
| Secrets | `HF_API_TOKEN` read from `local.properties` into `BuildConfig` (done in Phase 0) |
| CI | none |
| Local JDK | JetBrains Runtime 17.0.14 at `~/.jdks/jbr-17.0.14`; no Java on `PATH` |

## R1. Toolchain versions

**Decision**: Target the AGP 9 line with Gradle 9, Kotlin 2.3, and KSP 2.3. Use these as floors
and pin the latest stable patch of each when the upgrade task runs:

| Tool | Floor | Source of the floor |
|---|---|---|
| AGP | 9.0 | AGP 9 is the current major line; it requires Gradle 9 |
| Gradle | 9.0 | required by AGP 9 |
| Kotlin Gradle plugin | 2.3.20 | built into AGP 9 ("built-in Kotlin") |
| KSP | 2.3.6 | minimum named in Google's AGP 9 migration guidance |
| Hilt | 2.59.2 | minimum named in Google's AGP 9 migration guidance |
| Compose BOM | 2026.02.01 or later | Compose 1.12 / Material 3 1.4 are stable as of October 2026 |
| JDK | 17 | constitution; also the minimum for Gradle 9 and AGP 9 |
| `compileSdk` / `targetSdk` | level currently required by Google Play | constitution |

**Rationale**: The plan prompt asks for current stable versions. Exact patch numbers move
monthly, so the first implementation task reads the official release pages and pins them in
`gradle/libs.versions.toml`. The floors above are the versions confirmed by search on
2026-11-16; I could not confirm the newest patch of each.

**Alternatives considered**:
- *Stay on the last AGP 8.x*: lower risk, but AGP 8 is the previous major line and would need a
  second migration soon. Rejected as the end state, kept as a stepping stone (see R2).
- *Pin exact versions in this plan*: rejected, because the search results were several months
  old for some tools.

## R2. Upgrade path

**Decision**: Upgrade in two steps, each leaving the app building and running:

1. Gradle 8.4 → latest 8.x, AGP 8.2 → latest 8.x, Kotlin 1.9 → 2.x, Java 8 → 17, Hilt and
   Glide from kapt to KSP, every dependency into the catalog.
2. Gradle 9, AGP 9, built-in Kotlin, `kotlinOptions` → `kotlin { compilerOptions { } }`.

**Rationale**: AGP 8.2 → 9 crosses a major version with removed APIs. Doing it through the
last 8.x release surfaces deprecation warnings first, and a failure can be attributed to one
step. This also satisfies the constitution rule that each migration change leaves the app
buildable.

**Alternatives considered**: a single jump to AGP 9. Rejected: failures from the Kotlin 2
compiler, KSP, Java 17, and AGP 9 would arrive together.

## R3. kapt, KSP, and Data Binding

**Decision**:
- Hilt moves to KSP.
- The Glide annotation processor is removed: no `@GlideModule` exists in the code, so the
  processor does nothing.
- Data Binding keeps a kapt processor in `:app` only, because the app has a Kotlin
  `@BindingAdapter` and Data Binding has no KSP processor. Under AGP 9 this means replacing
  `kotlin-kapt` with `com.android.legacy-kapt`.
- No `:core` module uses kapt.

**Rationale**: The plan prompt says "KSP replacing kapt". That is fully achievable for Hilt.
Data Binding is the one processor that cannot move; it disappears when the XML screens are
migrated to Compose in later specs. Google's guidance for AGP 9 is to use KSP wherever it is
supported and keep `legacy-kapt` only for processors that cannot move.

**To verify during implementation**: that Data Binding with a Kotlin `@BindingAdapter` works
under `com.android.legacy-kapt`. I did not find a source that states this directly. If it does
not work, the fallback is to opt out of built-in Kotlin in `:app` with the documented
`gradle.properties` flag until the XML screens are gone, and record that in the plan.

**Alternatives considered**: rewriting the binding adapters to remove Data Binding now.
Rejected: it changes existing screens, which the spec forbids (FR-019, FR-020).

## R4. Library upgrades forced by the toolchain

**Decision**: Upgrade a library only when the new toolchain requires it or when it is a
constitution-named part of the stack. Known cases:

| Library | Issue | Action |
|---|---|---|
| Firebase BoM 32.7.4 with `firebase-analytics-ktx`, `firebase-firestore-ktx` | The `-ktx` artifacts were folded into the main artifacts and removed from recent BoMs | Upgrade the BoM and switch to `firebase-analytics` and `firebase-firestore`; imports stay the same |
| `firebase-ui-auth` 8.0.2 | Old, tied to old Firebase Auth | Upgrade only if the new BoM breaks it |
| Hilt 2.48 | Too old for KSP 2 and AGP 9 | Upgrade to the floor in R1 |
| Retrofit 2.9, OkHttp 4.11, Coroutines 1.7, Lifecycle 2.7 | Work with the new toolchain | Move to the catalog; bump to the current stable patch of the same major line |
| Apache POI 5.2.3, SwipeLayout, MaterialProgressBar, `security-crypto` alpha | Work today | Move to the catalog unchanged |

**Rationale**: FR-019 requires unchanged behavior. Each extra upgrade adds regression risk with
no benefit to this spec.

**Alternatives considered**: upgrade everything to latest. Rejected for the reason above;
later specs replace most of these libraries when they migrate each feature.

## R5. Convention plugins

**Decision**: An included build at `build-logic/` with precompiled Kotlin convention plugins:

| Plugin ID | Applies |
|---|---|
| `alpha.android.application` | Android application, SDK levels, Java 17, lint settings |
| `alpha.android.library` | Android library, SDK levels, Java 17, lint settings, dependency rules |
| `alpha.android.compose` | Compose compiler plugin, Compose BOM, `buildFeatures.compose` |
| `alpha.jvm.library` | Kotlin JVM library, Java 17, dependency rules |
| `alpha.hilt` | Hilt plugin, KSP, Hilt dependencies |

**Rationale**: This is the standard layout used by Google's Now in Android sample and matches
the constitution ("build configuration shared by modules SHOULD live in convention plugins").
Five plugins cover every module type this spec creates.

**Alternatives considered**:
- *`buildSrc`*: any change invalidates the whole build cache. Rejected.
- *`subprojects { }` blocks in the root build file*: breaks project isolation and the
  configuration cache. Rejected.

## R6. Module types and enforced dependency rules

**Decision**:
- `:core:common` and `:core:domain` are Kotlin JVM libraries. They cannot use Android,
  Compose, Retrofit, or Firebase because those are not on their classpath. This enforces
  FR-009 through the compiler.
- `:core:data`, `:core:network`, `:core:datastore`, `:core:designsystem`, `:core:ui`, and
  `:core:testing` are Android libraries.
- The convention plugins check every project dependency after evaluation and fail the build
  with a message naming the rule when:
  - a `:core:*` module depends on `:app` or a `:feature:*` module;
  - a `:feature:*` module depends on another `:feature:*` module;
  - `:core:domain` depends on anything other than `:core:common`.
- A small test in `build-logic` (Gradle TestKit) proves each rule rejects a violation.

**Rationale**: A check written in the project's own convention plugins needs no new library
and gives an error message in the project's own terms. Gradle already rejects circular project
dependencies.

**Alternatives considered**:
- *A third-party module-graph plugin*: adds a dependency the constitution would require
  justifying, for three rules. Rejected.
- *Rely on code review*: the spec requires the build to reject violations (FR-008). Rejected.

## R7. Secrets

**Decision**:
- `HF_API_TOKEN` is resolved in this order: Gradle property or environment variable
  `HF_API_TOKEN`, then `local.properties`, then empty string.
- When it resolves to empty, the build logs one warning naming the key and the file, and
  continues. The app then fails AI calls with an authorization error at run time, as it does
  today.
- A tracked `local.properties.example` lists the required keys with placeholder values.
- `google-services.json` stays tracked. It is public client configuration; Firebase security
  comes from security rules and app restrictions, not from hiding this file.
- CI runs a secret scan (gitleaks) on each push, with a custom rule for the `hf_` token
  format, and fails the check on a finding.

**Rationale**: Continuing with a warning lets CI and forks build without any secret (FR-018)
and keeps the missing-secret case explained (FR-013). The environment-variable path lets CI
inject a token later without writing a file.

**Alternatives considered**:
- *Fail the build when the token is missing*: blocks CI on forks and contributors who only
  work on UI. Rejected.
- *The Secrets Gradle Plugin*: one more plugin for a single key. Rejected; revisit in spec 002
  when OpenRouter keys are added.
- *Scan with a hand-written grep*: misses other credential formats. Rejected.

**Note**: the scan covers the pushed tree, not old history, so the revoked tokens in history
do not fail it. A `.gitleaksignore` or an allowlist entry is added only if they do.

## R8. Automated check

**Decision**: One GitHub Actions workflow, `.github/workflows/ci.yml`:

- **Triggers**: `push` to any branch; `pull_request` into `development` or `master`.
  Concurrency group per branch, cancelling superseded runs.
- **Steps**: checkout, JDK 17 (Temurin), Gradle setup with caching, secret scan, then
  `assembleDebug`, `testDebugUnitTest` plus `test` for JVM modules, and `lintDebug`.
- **On failure**: upload the test and lint HTML reports as artifacts.
- **Permissions**: `contents: read` only. No secrets are required for the build.

**Rationale**: The GitHub repository is public, so Actions minutes are free. The three Gradle
tasks map directly to FR-016 and to the constitution's merge gate.

**Alternatives considered**: separate jobs for build, test, and lint. Rejected for now: three
jobs repeat the Gradle start-up and compile cost, and the project is small. Revisit if the
run exceeds the 15-minute target (SC-004).

## R9. Lint baseline

**Decision**: Generate `lint-baseline.xml` for `:app` once, after the toolchain upgrade, and
set `abortOnError = true`. New `:core` modules get no baseline, so they start clean.

**Rationale**: FR-021. The existing code has findings that later specs remove when they
migrate each screen; fixing them here would change existing code for no user benefit.

**Alternatives considered**: `abortOnError = false`. Rejected: lint would never fail, which
defeats FR-016.

## R10. Compose enablement

**Decision**: Apply `alpha.android.compose` to `:app`, `:core:designsystem`, and `:core:ui`.
Add the Compose BOM, Material 3, UI tooling, and Activity Compose to the catalog. No
composable is written in this spec.

**Rationale**: FR-004 asks that the project be ready for Compose without migrating a screen.
Since Kotlin 2.0 the Compose compiler is a Kotlin compiler plugin versioned with Kotlin, so
there is no separate compiler version to keep in step.

**Alternatives considered**: add a sample composable screen to prove it works. Rejected:
unused code in the app. A build of the three modules with the plugin applied is the proof.

## R11. Repository hygiene

**Decision**:
- Stop tracking `.idea/` entirely and ignore the whole folder.
- Clean `gradle.properties`: remove the obsolete `-XX:MaxPermSize`, `kapt.incremental.apt`,
  and in-process compiler settings; enable the configuration cache, build cache, and parallel
  execution.
- Fix the AGP version mismatch by declaring every plugin once, in the catalog.
- Document building from a terminal, including setting `JAVA_HOME` to a JDK 17.

**Rationale**: The constitution forbids committing local IDE state. The Gradle settings to be
removed are leftovers that either do nothing on current JVMs or slow the build.

**Alternatives considered**: keep sharing some `.idea` files (code style). Rejected:
`kotlin.code.style=official` in `gradle.properties` already carries the code style.

## Pinned versions

Looked up on 2026-11-16 from the Google Maven, Maven Central, and Gradle release metadata.

| Component | Step 1 (last 8.x line) | Step 2 (9.x line, final) |
|---|---|---|
| Gradle | 8.14.7 | 9.9.1 |
| Android Gradle Plugin | 8.13.2 | 9.5.1 |
| Kotlin | 2.3.20 | 2.4.21 |
| KSP | 2.3.14 | 2.3.14 |
| Hilt | 2.58 | 2.60.1 |
| Compose BOM | not used | 2026.09.00 |
| Firebase BoM | 35.2.0 | 35.2.0 |
| `compileSdk` | 36 | 37 |
| `targetSdk` | 34 | 34 |
| `minSdk` | 26 | 26 |

Deviations from R1 and R4, and why:

- **`targetSdk` stays at 34.** Raising it to 35 or later turns on edge-to-edge layout and other
  run-time behavior changes, which would visibly change the existing screens and break FR-019
  ("the app keeps working unchanged"). It is raised in the design-system spec, where every
  screen is reworked for insets anyway. It must be raised before any Google Play release.
- **`compileSdk` is 36 in step 1 and 37 in step 2.** The newest AndroidX and Compose releases
  need `compileSdk` 37, which needs AGP 9.1 or later. Step 1 therefore held AndroidX at
  `core-ktx` 1.17.0, `appcompat` 1.7.1, `activity` 1.11.0, `lifecycle` 2.9.4. Step 2 uses
  `core-ktx` 1.19.1, `appcompat` 1.8.0, `activity` 1.13.0, `lifecycle` 2.11.0. `compileSdk`
  only selects the APIs the code compiles against; it does not change run-time behavior.
- **Kotlin 2.3.20 and Hilt 2.58 in step 1.** Hilt 2.59 and later need AGP 9, and Hilt 2.58
  cannot read Kotlin 2.4 metadata.
- **`material` stays at 1.11.0 and `firebase-ui-auth` at 8.0.2**: newer versions change
  component appearance or the sign-in API, and the build does not need them.
- **Step 2 pins the Kotlin Gradle plugin on the root `buildscript` classpath.** AGP 9.5.1
  bundles an older Kotlin compiler that cannot read libraries compiled with Kotlin 2.4
  (Firebase 35.x). Declaring `kotlin-gradle-plugin` 2.4.21 on the classpath is the documented
  way to choose the version used by built-in Kotlin.

### Outcomes recorded during implementation

- **Data Binding under `legacy-kapt` (R3)**: works on AGP 9.5.1. The generated binding classes
  and the Kotlin `@BindingAdapter` compile; the fallback was not needed.
- **Configuration cache (R11)**: enabled. A second `assembleDebug` reports "Configuration
  cache entry reused"; no plugin was incompatible.
- **Lint baseline (R9)**: 255 existing findings recorded in `app/lint-baseline.xml`. Two
  `TrustAllX509TrustManager` warnings inside the Apache POI jar were left out of the baseline
  because their location is an absolute path in the local Gradle cache; they stay visible as
  warnings and do not fail the build.
- **Missing-token warning (R7)**: printed by the `preBuild` task rather than at configuration
  time, so it still appears when the configuration cache is reused.

## Sources

- [Android Gradle plugin release notes](https://developer.android.com/build/releases/gradle-plugin)
- [AGP preview / AGP 9 notes](https://developer.android.com/build/releases/agp-preview?hl=en)
- [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin?authuser=2)
- [KSP, kapt, and legacy-kapt](https://developer.android.com/agents/skills/build/agp/agp-9-upgrade/references/ksp-kapt)
- [AGP 9 migration skill](https://developer.android.com/agents/skills/build/agp/agp-9-upgrade/skill)
- [Compose Bill of Materials](https://developer.android.com/develop/ui/compose/bom)
