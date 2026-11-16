# Structure Model: Build Foundation and Module Structure

**Date**: 2026-11-16 | **Plan**: [plan.md](plan.md)

This feature stores no user data. Its "model" is the module graph and the rules on it.

## Modules

| Module | Type | Purpose | May depend on |
|---|---|---|---|
| `:app` | Android application | Entry point, navigation host, DI wiring, and all existing code until later specs move it | any `:core:*`, any `:feature:*` |
| `:core:common` | Kotlin JVM | Pure Kotlin helpers shared by every layer: result types, dispatcher qualifiers | nothing in the project |
| `:core:domain` | Kotlin JVM | Use cases, domain models, repository interfaces | `:core:common` |
| `:core:network` | Android library | HTTP clients, provider services, interceptors | `:core:common` |
| `:core:datastore` | Android library | Local preferences and encrypted session storage | `:core:common` |
| `:core:data` | Android library | Repository implementations, mappers | `:core:common`, `:core:domain`, `:core:network`, `:core:datastore` |
| `:core:designsystem` | Android library, Compose | Theme, tokens, reusable components | nothing in the project |
| `:core:ui` | Android library, Compose | Shared screen-level building blocks, MVI base types | `:core:common`, `:core:domain`, `:core:designsystem` |
| `:core:testing` | Android library | Shared fakes, test dispatchers, test rules | `:core:common`, `:core:domain` |
| `:feature:*` (none yet) | Android library, Compose | One user-facing capability | `:core:*` except `:core:data`, `:core:network`, `:core:datastore` |

Namespaces follow `com.example.alpha_ai.core.<name>`. The application ID does not change in
this feature.

## Dependency rules

The build fails, naming the rule, when any of these is violated:

| ID | Rule | Enforced by |
|---|---|---|
| DR-1 | A `:core:*` module must not depend on `:app` or on a `:feature:*` module | convention plugin check |
| DR-2 | A `:feature:*` module must not depend on another `:feature:*` module | convention plugin check |
| DR-3 | `:core:domain` may depend only on `:core:common` | convention plugin check |
| DR-4 | `:core:domain` and `:core:common` must not use Android, Compose, Retrofit, or Firebase | module type (Kotlin JVM, no such libraries on the classpath) |
| DR-5 | No circular dependency between modules | Gradle |

The "May depend on" column above is the intended graph. Only DR-1 to DR-5 are enforced by the
build in this feature; the finer rules (for example a feature not using `:core:network`
directly) are reviewed by hand until a later spec needs them enforced.

## Module contents in this feature

Each `:core:*` module is created with:

- a `build.gradle.kts` that applies one convention plugin and declares its namespace;
- an empty main source set (Android libraries also get a minimal manifest if the tooling
  requires one);
- a `README.md` giving its purpose and allowed dependencies (FR-010).

No source file is moved out of `:app` in this feature.

## Configuration values

| Name | Kind | Supplied by | Required |
|---|---|---|---|
| `HF_API_TOKEN` | secret | `local.properties`, or environment variable / Gradle property of the same name | no; the build warns when absent |
| `sdk.dir` | machine path | `local.properties` (written by Android Studio) or `ANDROID_HOME` | yes |
| `JAVA_HOME` | machine path | environment, for terminal builds | yes, JDK 17 |
