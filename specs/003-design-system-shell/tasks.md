---

description: "Task list for spec 003: Design System and App Shell"
---

# Tasks: Design System and App Shell

**Input**: Design documents from `/specs/003-design-system-shell/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/design-system-api.md,
contracts/app-shell.md, quickstart.md

**Tests**: Required for every rule with behaviour (constitution IX; plan and quickstart §1 list
them): contrast, motion, button, loading timing, retry mapping, string parity. Visual states are
covered by `@AlphaPreviews` and the gallery. Tasks marked **MANUAL** need a person or a device.

**Organization**: Tasks are grouped by user story so each story can be built and checked alone.

**Size**: 53 tasks, above the roadmap's guide of about 40. Not split, decided 2026-11-17: 8 tasks
are manual or verification checks, many others are single small files, and spec 004 needs all
four stories, so a split would add a branch and a merge without making either half shippable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: The user story the task belongs to (US1 to US4)
- Paths are relative to the repository root

## Conventions for every task

- Path shorthands:
  - `DS` = `core/designsystem/src/main/kotlin/com/example/alpha_ai/core/designsystem`
  - `UIS` = `core/ui/src/main/kotlin/com/example/alpha_ai/core/ui/state`
  - `SHELL` = `app/src/main/kotlin/com/example/alpha_ai/shell`
  - `DBG` = `app/src/debug/kotlin/com/example/alpha_ai/shell`
  - `REL` = `app/src/release/kotlin/com/example/alpha_ai/shell`
  - Tests use the same path under `src/test/kotlin/`, written `DS-TEST`, `UIS-TEST`.
- Terminal builds need JDK 17: `$env:JAVA_HOME = "$HOME\.jdks\jbr-17.0.14"`.
- Commit after each task or logical group (Conventional Commits). The app must build and
  `./gradlew testDebugUnitTest` must pass at every commit.
- Feature code uses only `AlphaTheme.*` for colours, type, spacing, shapes, and motion: no
  `Color(0x…)`, literal `sp`, or literal `dp` outside `DS/theme/`.
- Every visible text comes from a string resource present in both `values/` and `values-ar/`.
- Legacy screens must look and behave the same in English (FR-021). Their only allowed change is
  replacing a literal text with an `@string` reference that has the same English text.
- `AGP 9.3.0-alpha04` is the owner's choice; do not change it.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Libraries, fonts, and build wiring that every later task needs.

- [X] T001 Add to `gradle/libs.versions.toml`: version `navigationCompose = "2.10.2"`; libraries `androidx-navigation-compose` (`androidx.navigation:navigation-compose`, `version.ref = "navigationCompose"`), `androidx-lifecycle-runtime-compose` (`androidx.lifecycle:lifecycle-runtime-compose`, `version.ref = "lifecycle"`), `androidx-compose-material-icons-core` (`androidx.compose.material:material-icons-core`, no version, from the Compose BOM). Change no existing entry
- [X] T002 [P] Download `IBMPlexSansArabic-{Regular,Medium,SemiBold,Bold}.ttf` and `OFL.txt` from `https://github.com/google/fonts/tree/main/ofl/ibmplexsansarabic` into a scratch folder, verify each file size matches the repository listing (235 924, 242 100, 244 616, 246 992 bytes), then copy them to `core/designsystem/src/main/res/font/ibm_plex_sans_arabic_{regular,medium,semibold,bold}.ttf` and `core/designsystem/fonts/OFL.txt`
- [X] T003 [P] Update `core/designsystem/build.gradle.kts`: `implementation(libs.androidx.compose.material.icons.core)`, `implementation(libs.androidx.lifecycle.runtime.compose)`, `testImplementation(libs.junit)`; add `lint { error += "HardcodedText" }`
- [X] T004 [P] Update `core/ui/build.gradle.kts`: add `testImplementation(libs.kotlinx.coroutines.test)` if any test needs it, and `lint { error += "HardcodedText" }`
- [X] T005 Update `app/build.gradle.kts`: apply `alias(libs.plugins.kotlin.serialization)`; add `implementation(projects.core.designsystem)`, `implementation(projects.core.ui)`, `implementation(libs.androidx.navigation.compose)`, `implementation(libs.androidx.lifecycle.runtime.compose)`, `implementation(libs.androidx.activity.compose)`, `implementation(libs.androidx.compose.material.icons.core)`, `implementation(libs.kotlinx.serialization.json)`; inside `android { }` add `androidResources { generateLocaleConfig = true }`. Create `app/src/main/res/resources.properties` with `unqualifiedResLocale=en-US`. Confirm `./gradlew assembleDebug assembleRelease` passes

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The theme and an empty frame with a gallery to render things in. Every story needs
both.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T006 [P] Create `DS/theme/Spacing.kt`: `@Immutable data class AlphaSpacing` with `none 0, xxs 2, xs 4, sm 8, md 12, lg 16, xl 24, xxl 32, xxxl 48` dp and `minTouchTarget = 48.dp`, plus `internal val LocalAlphaSpacing = staticCompositionLocalOf { AlphaSpacing() }`
- [X] T007 [P] Create `DS/theme/Shape.kt`: `internal val AlphaShapes = Shapes(extraSmall = RoundedCornerShape(4.dp), small = 8.dp, medium = 12.dp, large = 16.dp, extraLarge = 28.dp)`
- [X] T008 [P] Create `DS/theme/Color.kt`: `internal val LightColors` and `DarkColors` (`lightColorScheme`/`darkColorScheme`) with exactly the values in research.md §1 table, surface-container tones from the neutral palette, and `@Immutable data class AlphaColors(val scheme: ColorScheme, val brandAccent: Color = Color(0xFF2196F3), val isDark: Boolean)` with `LocalAlphaColors`. No dynamic colour
- [X] T009 [P] Create `DS/theme/Type.kt`: `internal val PlexSansArabic = FontFamily(Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal), Medium, SemiBold, Bold)` and `internal val AlphaTypography` using the sizes and weights in data-model.md "AlphaTypography", line height ≈ 1.4× for body and label, all in `sp`
- [X] T010 [P] Create `DS/theme/ReducedMotion.kt`: `fun reducedMotionFrom(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f`; `@Composable fun rememberReducedMotion(): Boolean` reading `Settings.Global.ANIMATOR_DURATION_SCALE` (default 1f) and re-reading on `Lifecycle.Event.ON_RESUME` via `LocalLifecycleOwner`; `val LocalReducedMotion = staticCompositionLocalOf { false }`
- [X] T011 Create `DS/theme/Motion.kt`: `@Immutable data class AlphaMotion(val reducedMotion: Boolean)` with `durationShort = 150`, `durationMedium = 250`, `durationLong = 400`, easings `easingStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)`, `easingEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)`, `easingEmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)`, and `fun <T> enter()`, `exit()`, `change()` returning `FiniteAnimationSpec<T>` that are `snap()` when `reducedMotion` is true (depends on T010)
- [X] T012 Create `DS/theme/Theme.kt`: `@Composable fun AlphaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` wrapping `MaterialTheme(colorScheme, AlphaTypography, AlphaShapes)` and providing `LocalAlphaSpacing`, `LocalAlphaColors`, `LocalAlphaMotion` (from `rememberReducedMotion()`), plus `object AlphaTheme { colors; typography; spacing; shapes; motion }` exactly as in contracts/design-system-api.md "Theme" (depends on T006–T011)
- [X] T013 [P] Create `DS/preview/AlphaPreviews.kt` with the `@AlphaPreviews` multi-preview annotation exactly as in contracts/design-system-api.md "Previews" (Light·EN, Dark·EN, Light·AR, Dark·AR, Font 200% at `widthDp = 360`)
- [X] T014 Create `core/designsystem/src/main/res/values/strings.xml` and `values-ar/strings.xml` with `ds_back` (Back / رجوع), `ds_retry` (Retry / إعادة المحاولة), `ds_cancel` (Cancel / إلغاء), `ds_loading` (Loading / جارٍ التحميل), `ds_in_progress` (%1$s, in progress / %1$s، قيد التنفيذ), `ds_char_count` (%1$d / %2$d)
- [X] T015 Add `Theme.AlphaAi.Compose` to `app/src/main/res/values/themes.xml` (parent `Theme.Material3.DayNight.NoActionBar`, `android:windowBackground` = surface colour; matching entry in `values-night/themes.xml`). Leave `Theme.AlphaAi` unchanged
- [X] T016 Create `SHELL/MainActivity.kt` (`@AndroidEntryPoint class MainActivity : AppCompatActivity()`, `enableEdgeToEdge()`, `setContent { AlphaApp() }`) and `SHELL/AlphaApp.kt` (`AlphaTheme { NavHost(navController, startDestination = ShellDestinations.startRoute) { with(ShellDestinations) { register(navController) } } }`, per contracts/app-shell.md "Root composable"). Register `MainActivity` in `app/src/main/AndroidManifest.xml` with `android:exported="false"` and `android:theme="@style/Theme.AlphaAi.Compose"`. Keep `SplashActivity` as the only launcher in `main`
- [X] T017 Create `DBG/ShellDestinations.kt` (`startRoute = GalleryRoute`; `register` adds `composable<GalleryRoute>` and `composable<GallerySectionRoute>`), `DBG/gallery/GalleryRoutes.kt` (`@Serializable data object GalleryRoute`, `@Serializable data class GallerySectionRoute(val section: GallerySection)`, `enum class GallerySection { Colors, Typography, Spacing, Shapes, Motion, Buttons, TextFields, Cards, TopBar, States, Language }`), and `DBG/gallery/GalleryScreen.kt` (a `LazyColumn` with item keys listing every section; tapping one navigates to `GallerySectionRoute`; each section screen shows a top bar with back and a placeholder until its story fills it). Every section screen MUST scroll vertically (`LazyColumn` with keys or `Modifier.verticalScroll`) so nothing is cut off at 200% font (FR-010). Add an `@AlphaPreviews` function for `GalleryScreen` and for the section placeholder (constitution IV)
- [X] T018 Create `app/src/debug/AndroidManifest.xml` with an `<activity-alias android:name=".GalleryLauncher" android:targetActivity="com.example.alpha_ai.shell.MainActivity" android:exported="true" android:label="@string/gallery_launcher_name">` holding the `MAIN`/`LAUNCHER` intent filter, and `app/src/debug/res/values{,-ar}/strings.xml` with `gallery_launcher_name` (Alpha-AI Gallery / معرض Alpha-AI) and one label per `GallerySection`
- [X] T019 Create `REL/ShellDestinations.kt`: `startRoute = LegacyEntryRoute` (`@Serializable data object` in the same file) whose `composable` starts `SplashActivity` in a `LaunchedEffect(Unit)` and finishes the activity. Confirm `./gradlew assembleDebug assembleRelease testDebugUnitTest` passes and the debug build shows two launcher icons

**Checkpoint**: the gallery opens from the debug icon and lists every section; the normal icon
still opens the legacy splash.

---

## Phase 3: User Story 1 - One visual language in light and dark themes (Priority: P1) 🎯 MVP

**Goal**: every colour, text style, spacing step, shape, and motion style is defined, readable in
both themes, and shown in the gallery.

**Independent Test**: open the gallery's Colors, Typography, Spacing, Shapes, and Motion sections
in light and dark mode; `ContrastTest` and `MotionTokensTest` pass.

### Tests for User Story 1

- [ ] T020 [P] [US1] Create `DS/theme/Contrast.kt`: `fun contrastRatio(foreground: Color, background: Color): Double` (WCAG 2.1 relative luminance) and `internal val ContrastPairs: List<ContrastPair>` (`name`, `fg`, `bg`, `minimum` = 4.5 for text or 3.0 for borders/icons) built from both schemes, covering every row of research.md §1 table
- [ ] T021 [P] [US1] Create `DS-TEST/theme/ContrastTest.kt`: asserts every `ContrastPairs` entry meets its minimum in light and dark (SC-002); asserts `contrastRatio(white, #1976D2)` ≥ 4.5; asserts `brandAccent` appears in no text pair (FR-001)
- [ ] T022 [P] [US1] Create `DS-TEST/theme/MotionTokensTest.kt`: with `reducedMotion = true`, `enter`, `exit`, `change` are `SnapSpec`; with `false` they are `TweenSpec` with the token durations; `reducedMotionFrom(0f)` is true and `reducedMotionFrom(1f)` is false (FR-006)

### Implementation for User Story 1

- [ ] T023 [P] [US1] Create `DBG/gallery/sections/ColorsSection.kt`: one swatch row per colour role showing its name, on-colour sample text, and measured contrast; `brandAccent` shown as "decorative only"
- [ ] T024 [P] [US1] Create `DBG/gallery/sections/TypographySection.kt`: every text style with its name and an English plus an Arabic sample line
- [ ] T025 [P] [US1] Create `DBG/gallery/sections/SpacingShapesSection.kt`: bars for each spacing token and boxes for each shape token, used by both the Spacing and Shapes sections
- [ ] T026 [P] [US1] Create `DBG/gallery/sections/MotionSection.kt`: a toggle that animates a box's size, colour, and visibility using `AlphaTheme.motion.enter/exit/change`, and a line stating whether reduced motion is on
- [ ] T027 [US1] Wire T023–T026 into `DBG/gallery/GalleryScreen.kt` for their `GallerySection` values. Each of T023–T026 MUST include an `@AlphaPreviews` function for its section (constitution IV)
- [ ] T028 [US1] **MANUAL** On the emulator, check quickstart.md §4 (Colors, Type, Spacing, Shapes, Motion) in light and dark, switching the system theme while the gallery is open, and §6 (remove animations). Record the result in this task

**Checkpoint**: the visual language is complete and testable on its own.

---

## Phase 4: User Story 2 - Arabic and English with correct direction and large text (Priority: P1)

**Goal**: every string in the app has Arabic; Arabic runs right to left; text survives 200% font.

**Independent Test**: switch the app language between English and Arabic (system per-app
language) and the font size between default and largest; walk the gallery; `StringsParityTest`
passes.

### Tests for User Story 2

- [ ] T029 [P] [US2] Create `app/src/test/kotlin/com/example/alpha_ai/StringsParityTest.kt`: for each of `app/src/main/res`, `app/src/debug/res`, `core/designsystem/src/main/res`, `core/ui/src/main/res`, parse `values/strings.xml` and `values-ar/strings.xml` (resolve paths from the project directory system property set in `app/build.gradle.kts` via `tasks.withType<Test> { systemProperty("rootDir", rootDir.path) }`) and fail listing any `<string>` or `<plurals>` name present in one and missing in the other; skip names marked `translatable="false"` (SC-005)

### Implementation for User Story 2

- [ ] T030 [US2] Move every hard-coded `android:text`, `android:hint`, and `android:contentDescription` literal in `app/src/main/res/layout/{activity_search_history,content_doc,content_er,content_ocr,content_stt,content_words,content_wr,fragment_nohistory,item_history}.xml` into `app/src/main/res/values/strings.xml` with the same English text . The Arabic literal hint "ترجمة" in `content_words.xml` keeps exactly that text in `values/strings.xml` (English users see it today, and FR-021 forbids changing it) and gets the same text in `values-ar/`. Reuse an existing string when its text matches exactly
- [ ] T031 [US2] Replace `"Record"` and `"Stop"` in `app/src/main/java/com/example/alpha_ai/ui/main/tasks/er/ERActivity.kt` and `…/stt/STTActivity.kt` with `getString(R.string.record)` / `getString(R.string.stop)` (add both strings)
- [ ] T032 [US2] Create `app/src/main/res/values-ar/strings.xml` with a Modern Standard Arabic translation of every string in `app/src/main/res/values/strings.xml`; keep format arguments (`%1$s`) and keep "Alpha-AI" and "OCR" in Latin script. Mark brand-only strings `translatable="false"` in `values/` instead of duplicating them
- [ ] T033 [P] [US2] Create `DBG/gallery/sections/LanguageSection.kt`: shows the current locale and layout direction, a row with a leading and trailing item, `Icons.AutoMirrored.Filled.ArrowBack` and `Icons.AutoMirrored.Filled.KeyboardArrowRight` (both mirror) next to `Icons.Filled.Settings` and `Icons.Filled.Search` (do not mirror; all four are in `material-icons-core`), a mixed sentence ("استخدم Alpha-AI لتصحيح النص"), a number sample formatted with the current locale, and a very long unbroken word and URL in a card that must wrap; add `@AlphaPreviews` for the section and wire it into `GalleryScreen.kt`
- [ ] T034 [US2] Confirm `aapt2 dump xmltree` of the debug APK lists `en-US` and `ar` in the generated `locale_config` (FR-007); record the command and output in this task
- [ ] T035 [US2] **MANUAL** quickstart.md §4 in Arabic (per-app language) light and dark, and §5 at maximum font size on a 360 dp wide emulator, across every gallery section done so far; walk the legacy flow in Arabic and note that legacy layouts are translated but keep their legacy styling. Record the result in this task

**Checkpoint**: language, direction, and font scaling work for the tokens and are proven for
every string.

---

## Phase 5: User Story 3 - Shared components with every state (Priority: P2)

**Goal**: buttons, text fields, card, top bar, and loading/empty/error states exist with every
state, previews, and a gallery page.

**Independent Test**: the gallery's Buttons, TextFields, Cards, TopBar, and States sections show
every state in both themes and both languages; `ButtonRulesTest`, `LoadingVisibilityTest`, and
`ScreenStateTest` pass.

### Tests for User Story 3

- [ ] T036 [P] [US3] Create `DS/component/ButtonRules.kt` (`fun buttonAcceptsClick(enabled: Boolean, busy: Boolean) = enabled && !busy`) and `DS-TEST/component/ButtonRulesTest.kt` covering all four combinations (FR-012)
- [ ] T037 [P] [US3] Create `DS/component/LoadingVisibility.kt` (`fun loadingVisible(elapsedMs: Long, shownAtMs: Long?, finished: Boolean, finishedAtMs: Long?): Boolean`: hidden before 400 ms; once shown, visible until at least 500 ms after `shownAtMs`, even when finished) and `DS-TEST/component/LoadingVisibilityTest.kt` covering: finished at 300 ms never shows; shown at 400 ms and finished at 450 ms stays until 900 ms; long operation stays visible (FR-014)
- [ ] T038 [P] [US3] Create `UIS/ScreenState.kt` and `UIS/AiFailureStates.kt` exactly as in contracts/design-system-api.md "Screen state" (`isRetryable()` true for `NoConnection`, `TimedOut`, `ServiceBusy`, `ServiceUnavailable`, `InvalidResponse`, `Unknown`; false for `InvalidInput`; no `else` branch), and `UIS-TEST/ScreenStateTest.kt` asserting both rules for every failure and every `InvalidInputReason`, and that `toErrorState().messageRes == messageRes()` (FR-015)

### Implementation for User Story 3

- [ ] T039 [P] [US3] Create `DS/component/Buttons.kt`: `AlphaPrimaryButton`, `AlphaSecondaryButton`, `AlphaTextButton`, `AlphaIconButton` with the parameters in contracts/design-system-api.md "Components"; min height `spacing.minTouchTarget`, label wraps; busy shows a 18 dp `CircularProgressIndicator`, keeps the label for semantics as `ds_in_progress`, and uses `buttonAcceptsClick` for `onClick`; `AlphaIconButton` requires a non-blank `contentDescription` and mirrors only when `mirrorInRtl`. `@AlphaPreviews` for normal, disabled, busy, long label
- [ ] T040 [P] [US3] Create `DS/component/TextFields.kt`: `AlphaTextField` and `AlphaMultilineTextField` (`minLines = 3`, `maxLines = 8`, `maxLength: Int?` showing `ds_char_count`); error message as supporting text and `Modifier.semantics { error(message) }` on the field, and the supporting text carries `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` so a newly shown error is spoken without moving focus (FR-013). `@AlphaPreviews` for empty, filled, disabled, error, near limit
- [ ] T041 [P] [US3] Create `DS/component/Card.kt` (`AlphaCard`: `surface` colour, outline border, `shapes.medium`, `spacing.lg` padding, clickable only with `onClick`) and `DS/component/TopBar.kt` (`AlphaTopBar`: `TopAppBar` with title `maxLines = 1` and ellipsis, back as `AlphaIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(ds_back))` when `onBack` is set). `@AlphaPreviews` for each, including a very long title
- [ ] T042 [P] [US3] Create `DS/component/LoadingState.kt`, `EmptyState.kt`, `ErrorState.kt` per the contract: loading uses `loadingVisible` with a clock from `withFrameMillis`, fades with `motion.enter/exit`, and shows `ds_cancel` only with `onCancel`; empty shows message, optional icon and action; error shows message and `ds_retry` only with `onRetry`, long messages wrap and retry stays visible. `@AlphaPreviews` for each
- [ ] T043 [US3] Create `UIS/ScreenStateHost.kt` (`AlphaScreenStateHost` per the contract, switching with `AnimatedContent` using `AlphaTheme.motion.change()`) with `@AlphaPreviews` for loading, empty, error (retryable and not), and content (depends on T038, T042)
- [ ] T044 [US3] Create `DBG/gallery/sections/ComponentsSections.kt` with the Buttons, TextFields, Cards, TopBar, and States sections showing every state from T039–T043 (text fields hold their text with `rememberSaveable`), and wire them into `GalleryScreen.kt`, with an `@AlphaPreviews` function per section (constitution IV). Replace each section's temporary top bar from T017 with `AlphaTopBar`
- [ ] T045 [US3] **MANUAL** quickstart.md §4–§5 for the component sections, and §7 (Accessibility Scanner on every section). Check the pressed state of every button by tapping it in the gallery (static previews cannot show it), and with TalkBack on, check that a field error is spoken as soon as it appears. Record the result in this task

**Checkpoint**: the full component set is usable by spec 004.

---

## Phase 6: User Story 4 - One frame for every screen (Priority: P3)

**Goal**: the frame behaves predictably, release builds carry no gallery, and adding a screen is
documented and proven.

**Independent Test**: walk the legacy flow unchanged; open the gallery and leave it with back;
rotate and restore; check the release manifest.

- [ ] T046 [US4] In `SHELL/AlphaApp.kt`, wrap the `NavHost` in a `Scaffold` that applies `WindowInsets.safeDrawing` padding, and confirm system back pops inside the frame and finishes `MainActivity` at the start destination (FR-019) with no custom `BackHandler`
- [ ] T047 [US4] **MANUAL** quickstart.md §8 (rotation and "Don't keep activities" restore in a gallery section with typed text) and §3 (full legacy flow from the normal icon: splash, onboarding, register, sign in, home, each tool, history; English text identical to `development`). Record the result in this task (FR-020, FR-021, SC-006)
- [ ] T048 [US4] Confirm quickstart.md §2: the release APK manifest has no `GalleryLauncher`, one launcher (`SplashActivity`), and `MainActivity` with `exported="false"` (FR-022). Record the command and output in this task
- [ ] T049 [US4] **MANUAL** quickstart.md §9: add a throwaway route by following contracts/app-shell.md "Adding a screen", time it (target < 30 min, SC-007), then revert it. Record the time and any unclear step, and fix the contract text if a step was unclear

**Checkpoint**: all four stories work independently.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T050 Run `./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug`; fix every new lint warning in changed files (no new entries in `app/lint-baseline.xml`)
- [ ] T051 [P] Search changed Kotlin under `core/designsystem`, `core/ui`, `app/src/{main,debug,release}/kotlin` for `Color(0x`, `.sp`, and `.dp` outside `DS/theme/`; for literal user-visible text in Compose (`Text("`, `text = "`, `label = "`, `placeholder = "`, `contentDescription = "`), which lint's `HardcodedText` does not catch; and for `Log.` or `println`. Fix any hit (sample texts in the gallery's Typography and Language sections come from `app/src/debug/res` strings too)
- [ ] T052 [P] In the last commit before the merge, mark row 003 in `ROADMAP.md` section 6 as Done and note in section 1 that `values-ar` now exists
- [ ] T053 **MANUAL** Owner reviews the Arabic in `app/src/main/res/values-ar/strings.xml`, `core/designsystem/src/main/res/values-ar/strings.xml`, and `app/src/debug/res/values-ar/strings.xml` before merge

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none. T001 before T003–T005.
- **Foundational (Phase 2)**: needs Phase 1. T006–T010 in parallel; T011 after T010; T012 after
  T006–T011; T016 after T012 and T015; T017–T019 after T016.
- **US1 (Phase 3)** and **US2 (Phase 4)**: need Phase 2; they can run in parallel. T030–T032 do
  not even need Phase 2.
- **US3 (Phase 5)**: needs Phase 2; T042 uses US1's motion tokens (already in Phase 2). Its manual
  check T045 is best done after US2 so Arabic is available.
- **US4 (Phase 6)**: needs Phase 2; T047 and T048 are best run last, on the finished build.
- **Polish (Phase 7)**: after all stories.

### Within a story

- Tests and pure rules (T020–T022, T036–T038) before the composables that use them.
- Gallery wiring (T027, T033, T044) after the sections it wires.

## Parallel Opportunities

```text
Phase 2:  T006 Spacing | T007 Shape | T008 Color | T009 Type | T010 ReducedMotion | T013 Previews
US1:      T020 Contrast | T022 MotionTokensTest, then T023 | T024 | T025 | T026 sections
US2:      T029 parity test | T030 layout strings | T033 language section
US3:      T036 | T037 | T038 rules and tests, then T039 | T040 | T041 | T042 components
```

## Implementation Strategy

### MVP (User Story 1)

Phase 1 → Phase 2 → Phase 3. Stop and check: the gallery shows the full visual language in both
themes and the contrast test passes. Spec 004 could already use the theme.

### Incremental delivery

1. US1: tokens and theme.
2. US2: translations and direction (can overlap with US1).
3. US3: components, which spec 004 needs for its screens.
4. US4: frame checks and the "adding a screen" proof.
5. Polish, owner's translation review, then merge into `development` (never `master`).
