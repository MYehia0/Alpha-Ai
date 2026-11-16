# Implementation Plan: Build Foundation and Module Structure

**Branch**: `001-build-foundation` | **Date**: 2026-11-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-build-foundation/spec.md`

## Summary

Bring the build up to the current toolchain, split out an empty set of shared modules with
dependency rules the build enforces, keep secrets out of the repository, and add an automated
check on every push. The app's behavior does not change.

Technical approach, from [research.md](research.md):

- Upgrade in two steps: first to the last Gradle 8 / AGP 8 with Kotlin 2 and Java 17, then to
  Gradle 9 / AGP 9 with built-in Kotlin.
- Move Hilt from kapt to KSP. Keep a kapt processor in `:app` only, for Data Binding, until
  the XML screens are migrated.
- Declare every dependency in `gradle/libs.versions.toml`.
- Add a `build-logic` included build with five convention plugins, which also enforce the
  module dependency rules.
- Create eight empty `:core:*` modules.
- Enable Compose in `:app`, `:core:designsystem`, and `:core:ui`, with no composables yet.
- Read `HF_API_TOKEN` from the environment or `local.properties`, warning when it is absent.
- Add a GitHub Actions workflow: secret scan, debug build, unit tests, lint.

## Technical Context

**Language/Version**: Kotlin 2.3.x (floor 2.3.20), Java and Kotlin target 17

**Primary Dependencies**: Gradle 9.x, Android Gradle Plugin 9.x, KSP 2.3.x (floor 2.3.6),
Hilt (floor 2.59.2), Compose BOM (floor 2026.02.01). Exact patch versions are pinned by the
first implementation task from the official release pages.

**Storage**: N/A

**Testing**: JUnit 4, MockK, Turbine, `kotlinx-coroutines-test` (added to the catalog and
exposed by `:core:testing`); Gradle TestKit for the convention-plugin rules

**Target Platform**: Android, `minSdk` 26, `compileSdk` 37, `targetSdk` 34. `targetSdk` is
deliberately not raised in this spec, because doing so changes run-time behavior (see
"Pinned versions" in research.md); it is raised in the design-system spec

**Project Type**: mobile-app (multi-module Android)

**Performance Goals**: automated check result within 15 minutes of a push; a single `:core`
module builds faster than the full project

**Constraints**: no change visible to end users; no source moved out of `:app` unless the
build requires it; no secret needed to build; each commit leaves the app building

**Scale/Scope**: 56 Kotlin files, 30 XML layouts, 1 existing module, 8 new modules, 5
convention plugins, 1 workflow

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Clean Architecture | Pass | `:core:domain` is a Kotlin JVM module, so it cannot import Android, Firebase, Retrofit, or Compose |
| II. Modularization and Hilt DI | Pass | Creates the `:core:*` modules, convention plugins, and the catalog the principle requires; dependency rules are enforced by the build |
| III. MVI with Coroutines and Flow | N/A | No screen or ViewModel is added or changed |
| IV. Compose-First UI | Pass | Compose is enabled; no new XML, Data Binding, or View Binding is added |
| V. Type-Safe Navigation | N/A | No navigation change |
| VI. Motion and UX Quality | N/A | No UI change |
| VII. Android Security | Pass, with one item deferred | Secrets come from git-ignored files or the environment; CI scans for credentials. Release hardening (R8, cleartext, backup rules) is left to spec 015, as the roadmap sets out |
| VIII. Resilient Networking | N/A | No networking change |
| IX. Tested Business Logic | Pass | No business logic is added. The dependency-rule checks in `build-logic` ship with tests. `:core:testing` and the test libraries are put in place |
| X. Provider-Agnostic AI | N/A | No AI integration change; spec 002 covers it |
| Technology constraints | Pass, with one deviation | Java and Kotlin 17, KSP for Hilt. Deviation: a kapt processor remains in `:app` for Data Binding (see Complexity Tracking) |
| Workflow and quality gates | Pass | Feature branch from `development`; Conventional Commits; build, tests, and lint gate every push; `.idea/` stops being tracked |

**Result**: gate passes. One deviation is justified below.

**Post-design re-check (after Phase 1)**: unchanged. The design adds no library outside the
constitution's stack apart from build tooling (KSP, Compose compiler plugin, gitleaks in CI),
and no rule is weakened.

## Project Structure

### Documentation (this feature)

```text
specs/001-build-foundation/
├── plan.md              # This file
├── research.md          # Phase 0: decisions and alternatives
├── data-model.md        # Phase 1: module graph and dependency rules
├── quickstart.md        # Phase 1: validation steps
├── contracts/
│   └── build-contracts.md   # Plugin IDs, catalog rules, secrets, commands, CI
├── checklists/
│   └── requirements.md
└── tasks.md             # Created by /speckit-tasks
```

### Source Code (repository root)

```text
.github/
└── workflows/
    └── ci.yml                    # NEW: secret scan, build, unit tests, lint

build-logic/                      # NEW: included build
├── settings.gradle.kts
└── convention/
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin/          # alpha.android.application, alpha.android.library,
        │                         # alpha.android.compose, alpha.jvm.library, alpha.hilt,
        │                         # and the dependency-rule check
        └── test/kotlin/          # Gradle TestKit tests for DR-1 to DR-3

app/                              # EXISTING: all current code stays here
├── build.gradle.kts              # CHANGED: convention plugins, catalog references, KSP
├── lint-baseline.xml             # NEW
└── src/

core/                             # NEW: eight empty modules, each with build.gradle.kts
├── common/                       #   and README.md
├── domain/
├── data/
├── network/
├── datastore/
├── designsystem/
├── ui/
└── testing/

gradle/
├── libs.versions.toml            # CHANGED: every version and dependency
└── wrapper/                      # CHANGED: Gradle 9

build.gradle.kts                  # CHANGED: plugin declarations from the catalog only
settings.gradle.kts               # CHANGED: includeBuild("build-logic"), core modules
gradle.properties                 # CHANGED: obsolete flags removed, caches enabled
local.properties.example          # NEW
.gitignore                        # CHANGED: ignore .idea/ entirely
README.md                         # CHANGED: setup and terminal build instructions
```

**Structure Decision**: multi-module Android project with an included `build-logic` build for
convention plugins, following the layout in the constitution (Principle II). Only `:app` and
`:core:*` exist after this feature; `:feature:*` modules arrive with the specs that migrate
each feature. Module purposes and allowed dependencies are in [data-model.md](data-model.md).

## Implementation Sequence

Each step ends with the app building and is committed separately.

1. **Hygiene**: stop tracking `.idea/`, clean `gradle.properties`, add
   `local.properties.example`.
2. **Catalog**: move every dependency and plugin into `gradle/libs.versions.toml` at its
   current version. No version changes yet.
3. **Toolchain step 1**: latest Gradle 8 and AGP 8, Kotlin 2, Java 17; Hilt to KSP; remove the
   Glide processor; replace the Firebase `-ktx` artifacts. Build, run, and smoke-test.
4. **Toolchain step 2**: Gradle 9 and AGP 9, built-in Kotlin, `compilerOptions` DSL,
   `legacy-kapt` for Data Binding. Build, run, and smoke-test.
5. **Lint baseline** for `:app`; `abortOnError = true`.
6. **Convention plugins** in `build-logic`, with the dependency-rule check and its tests;
   switch `:app` to them.
7. **Core modules**: create the eight modules with READMEs; enable Compose in the three
   Compose modules.
8. **Secrets**: environment-variable fallback and the missing-token warning.
9. **CI workflow**, then the deliberate-failure checks from [quickstart.md](quickstart.md).
10. **Documentation**: README setup section; full manual regression of the app; update
    `ROADMAP.md` status.

## Risks

| Risk | Mitigation |
|---|---|
| Data Binding with a Kotlin `@BindingAdapter` may not work under `legacy-kapt` (not confirmed by a source) | Verified during implementation: Data Binding works under `legacy-kapt` on AGP 9.5.1. The fallback was not needed |
| Kotlin 2 compiler reports new errors in existing code | Fix only what blocks compilation; keep each fix minimal and behavior-neutral |
| A library has no version compatible with AGP 9 | Replace with the nearest equivalent only where required, and record it in this plan |
| Firebase BoM upgrade changes Auth UI behavior | Upgrade `firebase-ui-auth` only if the build breaks; test sign-in and sign-up by hand |
| No automated UI tests exist, so "unchanged behavior" rests on a manual pass | Run the full flow list in quickstart section 1 after steps 3, 4, and 10 |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `targetSdk` stays at 34 (the constitution asks for the level Google Play requires) | Raising it enables edge-to-edge and other behavior changes on every existing screen | Raising it now would break FR-019 (no visible change). It is raised in the design-system spec and must be raised before any Play release |
| A kapt processor remains in `:app` (the constitution names KSP for annotation processing; the plan prompt says KSP replaces kapt) | Data Binding has no KSP processor, and the 30 existing XML screens use it with a Kotlin `@BindingAdapter` | Removing Data Binding now would rewrite existing screens, which the spec forbids. The constitution requires KSP "in new modules", and no new module uses kapt; it is removed from `:app` when the last XML screen is migrated |
