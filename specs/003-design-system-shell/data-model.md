# Data Model: Design System and App Shell

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-11-17

This feature stores no data. Its "entities" are the design tokens, component states, and
navigation routes that later specs build on. All types below are immutable (`@Immutable` or
`@Stable` where Compose needs to know) so recomposition can be skipped (constitution IV).

## 1. Design tokens (`:core:designsystem`, package `…core.designsystem.theme`)

### AlphaColors

The Material 3 `ColorScheme` (light and dark), plus one extra token.

| Field | Type | Rule |
|---|---|---|
| `scheme` | `ColorScheme` | Values from [research.md §1](research.md#1-color-palette-and-contrast). `primary` is `#1976D2` in light. |
| `brandAccent` | `Color` | `#2196F3`. Decorative use only: never behind or as text. |
| `isDark` | `Boolean` | Chosen from `isSystemInDarkTheme()`; no dynamic color. |

**Validation**: `ContrastPairs` lists every foreground/background pair used together. A unit test
asserts each is ≥ 4.5:1 (text) or ≥ 3:1 (borders, icons, large text) in both themes (SC-002).

### AlphaTypography

Material 3 `Typography` with one `FontFamily` (IBM Plex Sans Arabic 400/500/600/700).

| Style group | Sizes (large / medium / small, sp) | Weight |
|---|---|---|
| display | 57 / 45 / 36 | Regular |
| headline | 32 / 28 / 24 | Regular |
| title | 22 / 16 / 14 | Medium |
| body | 16 / 14 / 12 | Regular |
| label | 14 / 12 / 11 | Medium |

Line height ≈ 1.4× font size for body and label (Arabic needs the extra room).

### AlphaSpacing

| Token | dp |
|---|---|
| `none` | 0 |
| `xxs` | 2 |
| `xs` | 4 |
| `sm` | 8 |
| `md` | 12 |
| `lg` | 16 (screen gutter) |
| `xl` | 24 |
| `xxl` | 32 |
| `xxxl` | 48 |

Also `minTouchTarget = 48.dp` (FR-016).

### AlphaShapes

Material 3 `Shapes`: extraSmall 4, small 8, medium 12, large 16, extraLarge 28 dp.

### AlphaMotion

| Field | Value |
|---|---|
| `durationShort` | 150 ms |
| `durationMedium` | 250 ms |
| `durationLong` | 400 ms |
| `easingStandard` | `CubicBezierEasing(0.2, 0, 0, 1)` |
| `easingEmphasizedDecelerate` | `CubicBezierEasing(0.05, 0.7, 0.1, 1)` |
| `easingEmphasizedAccelerate` | `CubicBezierEasing(0.3, 0, 0.8, 0.15)` |
| `reducedMotion` | `Boolean`, from `LocalReducedMotion` |

Helpers `enter<T>()`, `exit<T>()`, `change<T>()` return a `FiniteAnimationSpec<T>`; when
`reducedMotion` is true each returns `snap()` (FR-006).

### AlphaTheme

`AlphaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` wraps `MaterialTheme` and
provides `LocalAlphaSpacing`, `LocalAlphaMotion`, `LocalAlphaColors`. Accessors:
`AlphaTheme.colors`, `.typography`, `.spacing`, `.shapes`, `.motion`.

## 2. Component states (`:core:designsystem`, package `…core.designsystem.component`)

### Button

| State | Source | Behaviour |
|---|---|---|
| Normal | `enabled = true, busy = false` | Clickable |
| Pressed | interaction source | Material ripple |
| Disabled | `enabled = false` | Not clickable, 38% content alpha |
| Busy | `busy = true` | Shows a progress indicator in place of the icon, label kept for screen readers ("…, in progress"); **not clickable** (FR-012) |

Variants: `AlphaPrimaryButton`, `AlphaSecondaryButton` (outlined), `AlphaTextButton`,
`AlphaIconButton` (requires a non-empty `contentDescription`).

Rule tested on the JVM: `buttonAcceptsClick(enabled, busy) = enabled && !busy`.

### TextField

| State | Source |
|---|---|
| Empty | `value.isEmpty()` and not focused |
| Focused | interaction source |
| Filled | `value.isNotEmpty()` |
| Disabled | `enabled = false` |
| Error | `errorMessage != null`; message shown below and set as the field's `error()` semantics (FR-013) |

`AlphaTextField` is single-line. `AlphaMultilineTextField` takes `minLines` (default 3) and
`maxLines` (default 8), then scrolls; with `maxLength` it shows a `count / max` supporting text.

### Card

`AlphaCard(onClick: (() -> Unit)? = null)`: `surface` colour, outline border, `shapes.medium`,
`spacing.lg` padding. Clickable only when `onClick` is set.

### Top bar

`AlphaTopBar(title, onBack: (() -> Unit)? = null, actions)`: title single line with ellipsis;
back uses `Icons.AutoMirrored.Filled.ArrowBack` with the localized "Back" description.

### Screen state presentation

```text
ScreenState<T>
├── Loading(cancellable: Boolean)
├── Empty(messageRes: Int, actionLabelRes: Int?)
├── Error(messageRes: Int, retryable: Boolean)
└── Content(value: T)
```

Located in `:core:ui` (package `…core.ui.state`) because it uses `AiFailure` from
`:core:domain`. Rules:

- `AiFailure.isRetryable()` returns the domain's `AiFailure.canRetry` (spec 002), so the two can
  never disagree: `true` for `NoConnection`, `TimedOut`, `ServiceBusy`, `InvalidResponse`,
  `Unknown`; `false` for `ServiceUnavailable` (no key or credit, so retrying cannot help) and
  `InvalidInput` (FR-015). New failure types are classified once, in `canRetry`.
- `AiFailure.toErrorState()` builds `Error` from `messageRes()` (spec 002) and `isRetryable()`.

The visual blocks `AlphaLoadingState`, `AlphaEmptyState`, `AlphaErrorState` live in
`:core:designsystem` and take plain parameters (message, labels, callbacks).
`AlphaScreenStateHost` in `:core:ui` switches between them for a `ScreenState`.

### Loading visibility

`loadingVisible(elapsedMs, finishedAtMs)`: hidden before 400 ms; work that ends before 400 ms never
shows it; once shown it stays until at least 900 ms after the start (400 ms delay + 500 ms
minimum), even when finished (FR-014, edge case). `loadingHoldMs(finishedAtMs)` is how long a host
keeps the indicator after the work ends. Both are pure functions, JVM-tested. Times are measured
from the start of the work; `finishedAtMs` is null while it runs.

## 3. Navigation (`:app`, package `…alpha_ai.shell`)

| Route | Type | Source set | Purpose |
|---|---|---|---|
| `GalleryRoute` | `@Serializable data object` | `debug` | Component gallery (FR-017, FR-022) |
| `GallerySectionRoute(section: GallerySection)` | `@Serializable data class` | `debug` | One section in detail; argument is an enum (constitution V: small primitives only) |
| `LegacyEntryRoute` | `@Serializable data object` | `release` | Starts `SplashActivity`, finishes `MainActivity` |

`ShellDestinations` (one object per source set) provides `startRoute: Any` and
`NavGraphBuilder.register(navController)`. `AlphaApp` calls it, so `main` code never names a
debug-only route.

**State across rotation and restore** (FR-020): `NavHost` saves its back stack; screen state is
held with `rememberSaveable` or a ViewModel.

## 4. Strings

| Location | Content |
|---|---|
| `app/src/main/res/values/strings.xml` | existing 60 strings + about 17 moved from hard-coded text |
| `app/src/main/res/values-ar/strings.xml` | Arabic for every string above (SC-005) |
| `core/designsystem/src/main/res/values{,-ar}/strings.xml` | component texts: "Back", "Retry", "Cancel", "Loading", "%1$d / %2$d" |
| `app/src/debug/res/values{,-ar}/strings.xml` | gallery labels and the "Alpha-AI Gallery" launcher name |
| `core/ui/src/main/res/values{,-ar}/` | unchanged (failure messages from spec 002) |
