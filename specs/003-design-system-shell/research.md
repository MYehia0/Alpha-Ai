# Research: Design System and App Shell

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-11-17

Each section records a decision, why it was made, and what else was considered. Versions were
checked against Google Maven and Maven Central on 2026-11-17.

## 1. Color palette and contrast

**Decision**: a Material 3 color scheme built by hand from the brand blue, defined in
`:core:designsystem` and checked by a JVM unit test that computes the WCAG contrast ratio of
every foreground and background pair the components use.

| Role | Light | Dark | Contrast light / dark |
|---|---|---|---|
| primary / onPrimary | `#1976D2` / `#FFFFFF` | `#A1C9FF` / `#00315C` | 4.6 / 7.7 |
| primaryContainer / onPrimaryContainer | `#D3E4FF` / `#001C38` | `#004784` / `#D3E4FF` | 13.3 / 7.3 |
| secondary / onSecondary | `#535F70` / `#FFFFFF` | `#BBC7DB` / `#253140` | 6.5 / 7.7 |
| secondaryContainer / onSecondaryContainer | `#D7E3F7` / `#101C2B` | `#3B4858` / `#D7E3F7` | 13.3 / 7.2 |
| surface, background / onSurface | `#FFFFFF` / `#1A1C1E` | `#121316` / `#E3E2E6` | 17.1 / 14.4 |
| surfaceVariant / onSurfaceVariant | `#DFE2EB` / `#43474E` | `#43474E` / `#C3C6CF` | 7.2 / 5.5 |
| surface / primary (text buttons, links) | `#FFFFFF` / `#1976D2` | `#121316` / `#A1C9FF` | 4.6 / 10.9 |
| surface / outline (borders, 3:1 needed) | `#FFFFFF` / `#73777F` | `#121316` / `#8D9199` | 4.5 / 5.9 |
| error / onError | `#BA1A1A` / `#FFFFFF` | `#FFB4AB` / `#690005` | 6.5 / 7.7 |
| errorContainer / onErrorContainer | `#FFDAD6` / `#410002` | `#93000A` / `#FFDAD6` | 13.3 / 7.2 |
| surface / error (field error text) | `#FFFFFF` / `#BA1A1A` | `#121316` / `#FFB4AB` | 6.5 / 10.9 |

The light accent `#2196F3` (brand blue) is exposed as a separate `brandAccent` token for the
logo and decorative icons only. It measures 3.1:1 on white, so the contrast test asserts it is
never paired with text.

The surface-container roles (`surfaceContainerLowest` … `surfaceContainerHighest`) are tonal
steps of the neutral palette. Primary-coloured text is only placed on `surface`, `background`,
and `surfaceContainerLowest`; cards use `surface` with an outline so that text buttons inside
them keep 4.5:1.

**Rationale**: the owner chose `#1976D2` (clarification 2026-11-17). Material 3's own tonal
values for this hue give a dark-theme primary (`#A1C9FF`) that passes easily on dark surfaces.
A test turns SC-002 into a build failure instead of a manual check.

**Alternatives considered**: Material Theme Builder export (gives the same tones but not the
exact brand primary); dynamic color (rejected by the owner, clarification 2026-11-17).

## 2. Typeface

**Decision**: bundle IBM Plex Sans Arabic as four static TTF files in
`core/designsystem/src/main/res/font/`: Regular 400, Medium 500, SemiBold 600, Bold 700
(about 236–247 KB each, 970 KB total), taken from the `google/fonts` repository
(`ofl/ibmplexsansarabic`) with its `OFL.txt` licence beside them. One `FontFamily` serves all
text styles.

**Rationale**: the family contains both Arabic and Latin glyphs drawn to match (clarification
2026-11-17). Bundled files work offline and render the same on every device (FR-003).
Downloadable Google Fonts would need Play Services and a network on first use.

**Alternatives considered**: the variable font (not published for this family); downloadable
fonts (offline requirement); keeping the thin/light weights (no text style uses them).

**Licence**: SIL Open Font License 1.1. It allows bundling in an app; the licence text ships in
the repository and is listed in the app's open-source notices in spec 015.

## 3. Typography scale and large font sizes

**Decision**: the Material 3 type scale (display, headline, title, body, label; large, medium,
small each) with sizes in `sp` and line heights about 1.4× for body text, since Arabic script
needs more vertical room than Latin. Components never fix a height that contains text: they set
`heightIn(min = …)` and let text wrap. Single-line titles (top bar) use
`maxLines = 1` with an ellipsis.

**Rationale**: `sp` follows the system font size up to 200% (FR-010). Minimum heights instead of
fixed heights are what keep buttons and fields from cutting text at large sizes.

**Alternatives considered**: capping font scaling (fails FR-010).

## 4. Spacing, shapes, and elevation

**Decision**: a 4 dp base spacing scale exposed as `AlphaTheme.spacing`: `none 0`, `xxs 2`,
`xs 4`, `sm 8`, `md 12`, `lg 16`, `xl 24`, `xxl 32`, `xxxl 48`. Shapes map to Material 3
`Shapes`: extraSmall 4, small 8, medium 12, large 16, extraLarge 28 dp. The 16 dp screen gutter
is `spacing.lg`.

**Rationale**: a small fixed set (FR-004) and the Material 3 shape slots so stock Material
components pick them up.

## 5. Motion tokens and "remove animations"

**Decision**: `AlphaTheme.motion` exposes named durations (`short 150 ms`, `medium 250 ms`,
`long 400 ms`) and Material 3 easings (`standard`, `emphasizedDecelerate`,
`emphasizedAccelerate`), plus helper specs `enter`, `exit`, `change`. A
`LocalReducedMotion` value is read once from `Settings.Global.ANIMATOR_DURATION_SCALE` (0 means
"remove animations") and refreshed on resume. When it is on, every helper returns `snap()`.

**Rationale**: FR-005 and FR-006, constitution VI. Compose already scales its own animation
clock with the animator duration scale, but reading it explicitly also covers effects that do
not use that clock (for example a delayed loading indicator) and makes the rule testable.

**Alternatives considered**: relying only on Compose's built-in scaling (not testable, and
does not cover `delay`-based logic).

## 6. Loading state that does not flash

**Decision**: `AlphaLoadingState` becomes visible only after 400 ms; once shown it stays at
least 500 ms. The timing logic is a pure function (`loadingVisibility(elapsed, …)`) tested on
the JVM with virtual time.

**Rationale**: FR-014 and the edge case "a short operation shows no loading state".

## 7. Navigation frame

**Decision**: Navigation Compose `2.10.2` with `@Serializable` route objects
(kotlinx.serialization is already in the build). A new `MainActivity` (an
`AppCompatActivity`, see section 8) hosts `AlphaApp`, which owns the `NavHost` and a shared
`Scaffold`. Destinations are registered through `NavGraphBuilder` extension functions, one per
feature (`galleryDestination()` now; `authGraph()`, `homeGraph()` … later), which is the
pattern constitution V asks for.

The start destination depends on the build type:

- **debug**: the component gallery (`GalleryRoute`), reached from a second launcher icon
  "Alpha-AI Gallery" declared only in `app/src/debug/AndroidManifest.xml`.
- **release**: `LegacyEntryRoute`, which starts the legacy `SplashActivity` and finishes. In
  release nothing opens `MainActivity` yet, so this only guards against a stray launch.

`SplashActivity` stays the launcher, so the existing flow is untouched (FR-021). Spec 004 makes
`MainActivity` the launcher and replaces `LegacyEntryRoute` with the session-based start
destination.

**Rationale**: the frame exists and is exercised by the gallery without changing what users
see. Putting the gallery in the debug source set keeps it out of release builds (FR-022).

**Alternatives considered**: making `MainActivity` the launcher now and forwarding to
`SplashActivity` (adds a hop and a visible frame, against FR-021); Navigation 3
(`androidx.navigation3`, a different library from the one the constitution names; can be
reconsidered by a constitution amendment); a Hilt multibinding of destinations (more machinery
than three or four graphs need).

## 8. Language and right-to-left

**Decision**:

- Add `res/values-ar/strings.xml` in `:app` with all 60 existing strings, and move the 15
  distinct hard-coded texts found in layouts plus the two in `ERActivity` and `STTActivity`
  (`"Record"`, `"Stop"`) into string resources first, so every visible text is translatable.
  English text and layout stay the same.
- Enable `androidResources { generateLocaleConfig = true }` in `:app` with
  `res/resources.properties` (`unqualifiedResLocale=en-US`), so Android 13+ lists English and
  Arabic in the system's per-app language screen (FR-007).
- `MainActivity` is an `AppCompatActivity` so spec 005 can call
  `AppCompatDelegate.setApplicationLocales` and have it work on Android 8–12 too.
- `android:supportsRtl="true"` is already set. Compose mirrors layouts from
  `LocalLayoutDirection`; directional icons use the `AutoMirrored` Material icons, others do not
  mirror (FR-008).
- Mixed Arabic and English text relies on the platform's bidirectional algorithm; the gallery
  shows a sample to check it.

**Alternatives considered**: an in-app switch now (deferred to spec 005 by the owner);
translating only the new strings (fails FR-009 and SC-005).

## 9. Material icons

**Decision**: add `androidx.compose.material:material-icons-core` (BOM-managed) for the
`AutoMirrored` back arrow and the few icons the components need.

**Rationale**: Material 3 does not include icons. The core set is small; the extended set (a
very large artifact) is not needed.

## 10. Verification of the gallery

**Decision**: every component has `@Preview`s for its states, grouped by a multi-preview
annotation `@AlphaPreviews` (light/dark × English/Arabic, plus a 200% font preview). The gallery
screen renders the same states on a device. Logic that decides behaviour (busy button refuses
clicks, retry shown only for retryable failures, loading timing, contrast, motion fallback) is
covered by JVM unit tests.

**Rationale**: constitution IV requires previews; IX requires tests for logic. Screenshot or
Compose UI tests would need Robolectric or an emulator in CI. Constitution IX makes them a
SHOULD for critical flows only, and those flows arrive in specs 004 and 006.

**Alternatives considered**: Roborazzi screenshot tests (new dependency and CI setup; better
added in spec 014 alongside the accessibility pass).

## 11. Library versions

| Library | Version | Use |
|---|---|---|
| `androidx.navigation:navigation-compose` | 2.10.2 | type-safe routes and `NavHost` |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.11.0 (already the `lifecycle` version) | `collectAsStateWithLifecycle`, lifecycle-aware reduced-motion refresh |
| `androidx.compose.material:material-icons-core` | from Compose BOM 2026.09.00 | icons |
| `androidx.appcompat:appcompat` | 1.8.0 (already in the catalogue) | `MainActivity`, per-app locales |

No library duplicates an existing one. Each is added to `libs.versions.toml`.
