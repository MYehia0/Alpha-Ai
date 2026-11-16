# Build Contracts: Build Foundation and Module Structure

**Date**: 2026-11-16 | **Plan**: [plan.md](../plan.md)

The project exposes no API to other systems. These are the interfaces that developers and the
automated check rely on. Later specs may add to them but must not break them.

## 1. Convention plugins

A module's build file applies these by ID. It must not repeat what they configure.

| Plugin ID | For | Configures |
|---|---|---|
| `alpha.android.application` | `:app` | Android application plugin, `compileSdk`, `minSdk` 26, `targetSdk`, Java 17, lint rules |
| `alpha.android.library` | Android `:core:*` and `:feature:*` modules | Android library plugin, same SDK and Java settings, lint rules, dependency rules DR-1 to DR-3 |
| `alpha.android.compose` | modules with Compose UI | Compose compiler plugin, Compose BOM, `buildFeatures.compose = true` |
| `alpha.jvm.library` | `:core:common`, `:core:domain` | Kotlin JVM plugin, Java 17, dependency rules DR-1 to DR-3 |
| `alpha.hilt` | modules that provide or inject dependencies | Hilt plugin, KSP, Hilt runtime and compiler |

A violated dependency rule fails configuration with a message of this form:

```text
Dependency rule DR-1 violated: ':core:data' must not depend on ':app'.
Core modules must not depend on the app or on feature modules. See specs/001-build-foundation/data-model.md.
```

## 2. Version catalog

- File: `gradle/libs.versions.toml`.
- Every library, plugin, and SDK level is declared here and nowhere else.
- No module build file contains a version string or a `group:name:version` coordinate.
- Naming: libraries in kebab-case grouped by owner (`androidx-core-ktx`, `retrofit-core`,
  `firebase-firestore`); plugins in kebab-case (`android-application`, `kotlin-jvm`, `ksp`,
  `hilt`).

## 3. Secrets

| Key | Read from, in order | When absent |
|---|---|---|
| `HF_API_TOKEN` | Gradle property or environment variable `HF_API_TOKEN`; then `local.properties` | Build continues and prints one warning naming the key and `local.properties` |

- The value is exposed to app code as `BuildConfig.HF_API_TOKEN`, unchanged from today.
- `local.properties.example` is tracked and lists every key with a placeholder.
- `local.properties` and `api_keys.properties` stay git-ignored.

## 4. Commands

These work from the repository root with `JAVA_HOME` set to a JDK 17. The automated check runs
the same commands.

| Purpose | Command |
|---|---|
| Build the debug app | `./gradlew assembleDebug` |
| Run all unit tests | `./gradlew testDebugUnitTest test` |
| Run lint | `./gradlew lintDebug` |
| Build and test one module | `./gradlew :core:domain:build` |
| Test the convention plugins | `./gradlew -p build-logic test` |

## 5. Automated check

- **File**: `.github/workflows/ci.yml`.
- **Runs on**: every push to any branch; every pull request into `development` or `master`.
- **Result**: one status named `CI`. It passes only if the secret scan, debug build, unit
  tests, and lint all pass.
- **On failure**: test and lint reports are attached to the run as artifacts.
- **Secrets**: none required. The check must pass with no secret configured, and must not
  print the value of any secret it is given.
- **Time budget**: a result within 15 minutes of the push.
