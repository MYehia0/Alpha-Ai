# Implementation Plan: Design System and App Shell

**Branch**: `003-design-system-shell` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-design-system-shell/spec.md`

## Summary

Give Alpha-AI one visual language and one frame for screens, without changing anything users
see today.

- `:core:designsystem` holds the Material 3 theme built from the brand blue (`#1976D2`), the IBM
  Plex Sans Arabic type scale, spacing, shape, and motion tokens, and the shared components
  (buttons, text fields, card, top bar, loading, empty, error), each with previews in light,
  dark, English, Arabic, and 200% font.
- `:core:ui` adds the screen-state model and the mapping from AI failures (spec 002) to an error
  state with or without retry.
- `:app` gets a single `MainActivity` with a type-safe Navigation Compose host. In debug builds a
  second launcher icon opens the component gallery inside it; in release nothing opens it yet.
  `SplashActivity` stays the launcher, so the existing flow is unchanged.
- Every user-visible string in the app gets an Arabic translation, after the hard-coded texts in
  legacy layouts are moved into string resources.

Details and alternatives are in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17 target, AGP 9.3.0-alpha04 (owner's choice)

**Primary Dependencies**: Compose BOM 2026.09.00 with Material 3; Navigation Compose 2.10.2
(new); `lifecycle-runtime-compose` 2.11.0 (new); `material-icons-core` from the BOM (new);
AppCompat 1.8.0; kotlinx.serialization 1.13.0; Hilt 2.60.1

**Storage**: N/A

**Testing**: JUnit 4, `kotlinx-coroutines-test`, Turbine on the JVM; Compose `@Preview`s for
visual states; manual device checks in [quickstart.md](quickstart.md)

**Target Platform**: Android, minSdk 26, targetSdk 34, compileSdk 37

**Project Type**: mobile app (multi-module Gradle)

**Performance Goals**: animations at 60 fps on a mid-range device; the gallery opens in under
1 s on the emulator

**Constraints**: no visible change to the legacy flow; fonts bundled (offline); release build
contains no gallery; added APK size ≈ 1 MB (fonts) plus libraries

**Scale/Scope**: 11 components, 5 token groups, 1 activity, 3 routes, about 80 strings × 2
languages

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|---|---|---|
| I. Clean Architecture | Tokens and components in `:core:designsystem` (no domain dependency); screen state in `:core:ui` (depends on `:core:domain` for `AiFailure`); the shell in `:app`. No new dependency breaks DR-1 to DR-3. | Pass |
| II. Modular build | Uses the existing `alpha.android.library` + `alpha.android.compose` convention plugins; new libraries in `libs.versions.toml`. | Pass |
| III. MVI | The gallery has no business logic and no ViewModel; `ScreenState` is the shared shape for later MVI states. | Pass |
| IV. Compose-first and design system | All new UI in Compose and Material 3; tokens only from `:core:designsystem`; previews for every component; light/dark, Arabic/English, font scaling; strings from resources with `values-ar`. No new XML layouts. | Pass |
| V. Navigation | Single activity hosting Navigation Compose with `@Serializable` routes; destinations added as `NavGraphBuilder` extensions; arguments are enums/primitives. `SplashActivity` stays the launcher until spec 004 (the start destination from session state is spec 004's job). | Pass, see Complexity Tracking |
| VI. Motion and UX | Motion tokens reused everywhere; reduced motion honoured; loading/empty/error building blocks; 48 dp targets; content descriptions; WCAG AA enforced by a test. | Pass |
| VII. Security | No secrets, no network, no permissions. `MainActivity` is `exported="false"`; the gallery alias exists only in the debug manifest. | Pass |
| VIII. Networking | Not touched. | N/A |
| IX. Tests | JVM tests for every rule with behaviour (contrast, motion, button, loading timing, retry mapping, string parity). Compose UI tests are a SHOULD for critical flows, which arrive in specs 004 and 006. | Pass |
| X. AI integration | Only consumes `AiFailure` and its messages from spec 002. | N/A |
| Dependencies | Three new libraries, each justified in research §9 and §11; none duplicates an existing one. | Pass |

**Post-design re-check (after Phase 1)**: the data model and contracts keep every rule above.
The only deviation is the temporary second activity path, recorded below.

## Project Structure

### Documentation (this feature)

```text
specs/003-design-system-shell/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── design-system-api.md
│   └── app-shell.md
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks
```

### Source Code (repository root)

```text
core/designsystem/
├── build.gradle.kts                      # + material-icons-core
└── src/
    ├── main/kotlin/com/example/alpha_ai/core/designsystem/
    │   ├── theme/        Color.kt, Type.kt, Spacing.kt, Shape.kt, Motion.kt,
    │   │                 ReducedMotion.kt, Contrast.kt, Theme.kt
    │   ├── component/    Buttons.kt, TextFields.kt, Card.kt, TopBar.kt,
    │   │                 LoadingState.kt, EmptyState.kt, ErrorState.kt, ButtonRules.kt,
    │   │                 LoadingVisibility.kt
    │   └── preview/      AlphaPreviews.kt
    ├── main/res/font/    ibm_plex_sans_arabic_{regular,medium,semibold,bold}.ttf
    ├── main/res/values{,-ar}/strings.xml
    └── test/kotlin/…     ContrastTest, MotionTokensTest, ButtonRulesTest, LoadingVisibilityTest
core/designsystem/fonts/OFL.txt           # font licence

core/ui/
└── src/
    ├── main/kotlin/com/example/alpha_ai/core/ui/state/  ScreenState.kt, AiFailureStates.kt,
    │                                                    ScreenStateHost.kt
    └── test/kotlin/…/state/                             ScreenStateTest.kt

app/
├── build.gradle.kts                      # + designsystem, ui, navigation, serialization plugin,
│                                         #   generateLocaleConfig
└── src/
    ├── main/
    │   ├── AndroidManifest.xml           # + MainActivity (exported=false)
    │   ├── kotlin/…/alpha_ai/shell/      MainActivity.kt, AlphaApp.kt
    │   └── res/
    │       ├── resources.properties      # unqualifiedResLocale=en-US
    │       ├── values/strings.xml        # + moved hard-coded texts
    │       ├── values-ar/strings.xml     # new: every string
    │       ├── values/themes.xml         # + Theme.AlphaAi.Compose
    │       └── layout/*.xml              # literals → @string (text unchanged)
    ├── debug/
    │   ├── AndroidManifest.xml           # gallery launcher alias
    │   ├── kotlin/…/alpha_ai/shell/      ShellDestinations.kt, gallery/GalleryScreen.kt,
    │   │                                 gallery/sections/*.kt
    │   └── res/values{,-ar}/strings.xml
    ├── release/kotlin/…/alpha_ai/shell/  ShellDestinations.kt (LegacyEntryRoute)
    └── test/kotlin/…                     StringsParityTest.kt

gradle/libs.versions.toml                 # + navigation-compose, lifecycle-runtime-compose,
                                          #   material-icons-core
```

**Structure Decision**: tokens and visual components in `:core:designsystem` (no dependency
on domain), the `AiFailure`-aware state model in `:core:ui`, the shell and the debug-only
gallery in `:app`. Legacy code under `app/src/main/java` changes only where hard-coded texts
move into string resources.

## Implementation Notes

- **Font and theme for legacy screens**: legacy XML screens keep `Theme.AlphaAi` and Inter; only
  Compose screens use the new theme. `Theme.AlphaAi.Compose` (parent
  `Theme.Material3.DayNight.NoActionBar` from the Material Components library already in the
  build) only sets window colours so there is no flash before Compose draws.
- **Edge-to-edge**: `MainActivity` calls `enableEdgeToEdge()`; the shell `Scaffold` applies
  window insets so content never sits under the status or navigation bar.
- **Translations**: Modern Standard Arabic, reviewed by the owner before merge. Product names
  (Alpha-AI, OCR) stay in Latin script.
- **String parity test**: parses each module's `values/strings.xml` and `values-ar/strings.xml`
  and fails on any name present in one and missing in the other.
- **Lint**: `HardcodedText` raised to error in `:core:designsystem` and `:core:ui`.

## Complexity Tracking

| Deviation | Why needed | Simpler alternative rejected because |
|---|---|---|
| Two entry activities for one spec cycle (`SplashActivity` launcher + `MainActivity`) against constitution V's single-activity rule | FR-021 requires the existing flow to be unchanged until spec 004 migrates it | Making `MainActivity` the launcher and forwarding to `SplashActivity` adds a visible extra hop and changes the start-up path; spec 004 removes `SplashActivity` and makes `MainActivity` the only launcher |
| A debug-only second launcher icon | Reviewers need to open the gallery on a device (FR-017) without a user-facing entry (FR-022) | A hidden gesture or deep link is harder to find and test; a release entry would break FR-022 |
