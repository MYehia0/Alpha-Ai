# Implementation Plan: Onboarding and Authentication

**Branch**: `004-onboarding-auth` | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-onboarding-auth/spec.md`

## Summary

Replace the legacy splash, onboarding, login, and register screens with a Compose flow in a new
`:feature:auth` module. Make `MainActivity` the launcher, and decide where the user starts before
the first frame.

- **Domain** (`:core:domain/auth`) gets the auth models, the classified `AuthFailure`, form
  validation, the `AuthRepository` and `AppPreferencesRepository` interfaces, and eight use cases.
- **Data** (`:core:data/auth`) implements the repository on Firebase Auth and the existing
  Firestore `users/{uid}` document, maps every SDK error to `AuthFailure`, and erases v1's stored
  password on first launch.
- **Storage** (`:core:datastore`) gets an encrypted session store (DataStore with an AES-GCM key
  in the Android Keystore) and a plain preferences store for "introduction completed".
- **Presentation** (`:feature:auth`) has four MVI screens (introduction, sign-in, sign-up,
  password reset) and an `authGraph`.
- **Shell** (`:app`):
  - The splash screen API holds the first frame while `StartViewModel` resolves the start
    destination.
  - The legacy `HomeActivity` stays as "home" until spec 005. It gets a temporary
    `LegacyHomeSessionBridge` that wires the existing (currently dead) sign-out button, shows the
    one-time "confirmation link sent" message, and revalidates the session.
- **Removed**: `UserProvider`, the old `AuthRepository`, `PreferencesDataSource`, the four legacy
  screens and their layouts, `firebase-ui-auth`, `security-crypto`, and device backup.

Details and the alternatives considered are in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin 2.4.21, Java 17 target, AGP 9.3.0-alpha04

**Primary Dependencies**:
- Already in use: Compose BOM 2026.09.00 with Material 3; Navigation Compose 2.10.2; Hilt 2.60.1;
  kotlinx.serialization 1.13.0; Firebase BOM 35.2.0 (`firebase-firestore`).
- Now declared directly: `firebase-auth` (BOM), `kotlinx-coroutines-play-services`.
- New: `androidx.datastore` (DataStore and DataStore Preferences); `androidx.core:core-splashscreen`;
  `androidx.hilt:hilt-lifecycle-viewmodel-compose`; Compose `ui-test-junit4` and
  `ui-test-manifest` (BOM).
- Removed: `firebase-ui-auth` and `androidx.security:security-crypto`.

The new libraries use their latest stable versions, recorded in `libs.versions.toml` (research §16).

**Storage**: Firebase Auth (accounts and session), Firestore `users/{uid}` (name), an encrypted
DataStore file `session.enc` (profile cache and flags), and a Preferences DataStore
`app_preferences` (introduction and cleanup flags)

**Testing**:
- JVM: JUnit 4, `kotlinx-coroutines-test`, Turbine, and fakes from `:core:testing`.
- On a device: the Keystore cipher test and one Compose UI test for sign-in.
- Manual scenarios in [quickstart.md](quickstart.md).

**Target Platform**: Android, minSdk 26, targetSdk 34, compileSdk 37

**Project Type**: mobile app (multi-module Gradle)

**Performance Goals**: start decision under 100 ms after `Application.onCreate` (local reads
only); home visible within 2 s of a cold start on a mid-range phone (SC-002); 60 fps page
transitions in the introduction

**Constraints**:
- The start decision needs no network (FR-002), and no screen is drawn before it (FR-001).
- No password is persisted (FR-022), and no auth data is logged (FR-024).
- No legacy layout file changes.
- Existing v1 accounts and sessions keep working.

**Scale/Scope**: 4 screens, 4 routes, 8 use cases, 2 stores, 1 new module, 1 new convention
plugin, about 70 strings × 2 languages; 16 legacy Kotlin files and 6 layouts deleted

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Status |
|---|---|---|
| I. Clean Architecture | Models, interfaces, and use cases in `:core:domain` (pure Kotlin). Firebase only in `:core:data`. ViewModels see use cases and domain types only. The Firestore DTO is mapped at the data boundary. | Pass |
| II. Modules and Hilt | New `:feature:auth` depends only on `:core:*`. A new `alpha.android.feature` convention plugin. Bindings are provided through Hilt and implementations are `internal`. `TimeSource` and dispatchers are injected. `UserProvider` (a mutable `object`) and the manual `PreferencesDataSource` are deleted. | Pass |
| III. MVI | Each screen has a `UiState`, `Intent`, and `Effect`, one `StateFlow`, and `onIntent`. State is collected with `collectAsStateWithLifecycle`. DataStore exposes cold `Flow`s, one-shot operations are `suspend`, and `CancellationException` is rethrown by the error mapper. No `LiveData`. | Pass |
| IV. Compose and design system | All four screens are built in Compose from `AlphaTextField`, `AlphaPrimaryButton`, `AlphaTextButton`, `AlphaTopBar`, and the theme tokens. Previews cover the loading, content, and error states. Light/dark, RTL/LTR, and font scaling are covered. Every string has Arabic. No new XML layouts. | Pass |
| V. Navigation | `@Serializable` routes with primitive or enum arguments, and an `authGraph` composed in `:app`. Navigation runs from effects. The start destination is resolved before the first frame through the splash screen API. | Pass, see Complexity Tracking (legacy home is a second activity) |
| VI. Motion and UX | Pager motion uses the design-system motion tokens and honours reduced motion. Every operation has loading, error, and success states. Sign-in, sign-up, and reset show progress and offer Cancel while a request runs. Double submits are blocked. Controls have 48 dp targets and content descriptions. | Pass |
| VII. Security | No password stored. v1's stored password is erased. Session cache encrypted with a Keystore AES key. Firebase's own token store uses the SDK exception added in constitution 2.3.0 (app-private, never copied, excluded from backup and transfer, cleared by sign-out). Backup is disabled and data extraction is excluded. No email, token, or password in logs. The intent extra `start_reason` is validated against an enum. `MainActivity` export is now required as the launcher. | Pass |
| VIII. Errors | Firebase calls return `AuthResult` with a classified `AuthFailure`. Every failure maps to a localized message, with retry where it helps. Failures the spec deliberately does not show (offline refresh, failed confirmation email, pending profile sync) are logged by case name through Timber, never silently dropped. No HTTP here (Firebase SDK). | Pass |
| IX. Tests | JVM tests cover every use case, the error mapper, both repositories (with fakes, including the cancellation clean-up), the session serializer and the preferences store (plus the Keystore cipher on a device), the legacy home redirect policy, and every ViewModel (state and effect sequences). A Compose UI test covers sign-in. Fakes live in `:core:testing`. No live Firebase in tests. | Pass, see Complexity Tracking (legacy home back fix) |
| X. AI integration | Not touched. | N/A |
| Dependencies | Each new library is justified in research §16. Two libraries are removed. None duplicates an existing capability. | Pass |
| Migration rule | Splash, onboarding, login, and register are migrated whole and their legacy code deleted in this change. `HomeActivity` is not materially changed (behaviour is added in code; no layout changes). | Pass |

**Post-design re-check (after Phase 1)**: the data model and contracts keep every rule above.
The two recorded deviations are below; both concern the legacy home and end with spec 005.

## Project Structure

### Documentation (this feature)

```text
specs/004-onboarding-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── auth-screens.md
│   └── app-entry.md
├── checklists/requirements.md
└── tasks.md              # /speckit-tasks
```

### Source Code (repository root)

```text
build-logic/convention/src/main/kotlin/
└── AndroidFeatureConventionPlugin.kt        # new: alpha.android.feature

core/domain/src/main/kotlin/com/example/alpha_ai/core/domain/auth/
├── AuthUser.kt, AuthFailure.kt, AuthResult.kt, StartDestination.kt
├── Email.kt, Forms.kt                       # normalization, validation, FormField, FieldError
├── AuthRepository.kt, AppPreferencesRepository.kt, LegacyCleanup.kt
├── ActionCooldown.kt
└── usecase/  ResolveStartDestination, CompleteIntroduction, SignIn, SignUp,
              RequestPasswordReset, SignOut, RefreshSession, ConsumeConfirmationNotice
core/domain/src/test/kotlin/…/auth/          # FormsTest, EmailTest, ActionCooldownTest, one test per use case

core/datastore/src/
├── main/kotlin/…/core/datastore/
│   ├── SessionStore.kt, SessionData.kt, SessionSerializer.kt
│   ├── SessionCipher.kt, KeystoreSessionCipher.kt
│   ├── AppPreferencesStore.kt
│   └── di/DataStoreModule.kt
├── test/kotlin/…                            # SessionSerializerTest (fake cipher), AppPreferencesStoreTest
└── androidTest/kotlin/…                     # KeystoreSessionCipherTest

core/data/src/
├── main/kotlin/…/core/data/auth/
│   ├── FirebaseAuthRepository.kt            # implements AuthRepository
│   ├── DataStoreAppPreferencesRepository.kt
│   ├── FirebaseAuthDataSource.kt            # interface + internal Firebase impl
│   ├── FirebaseErrorMapper.kt, UserProfileDto.kt
│   ├── LegacyAuthCleanup.kt
│   └── di/AuthModule.kt                     # FirebaseAuth, FirebaseFirestore, bindings, TimeSource
└── test/kotlin/…/auth/                      # FirebaseErrorMapperTest, FirebaseAuthRepositoryTest,
                                             # DataStoreAppPreferencesRepositoryTest, LegacyAuthCleanupTest

core/testing/src/main/kotlin/…/auth/         # FakeAuthRepository, FakeAppPreferencesRepository

feature/auth/                                # new module
├── build.gradle.kts                         # alpha.android.feature
└── src/
    ├── main/kotlin/…/feature/auth/
    │   ├── navigation/AuthRoutes.kt, AuthGraph.kt
    │   ├── introduction/  IntroductionScreen, IntroductionViewModel, IntroductionContract
    │   ├── signin/        SignInScreen, SignInViewModel, SignInContract
    │   ├── signup/        SignUpScreen, SignUpViewModel, SignUpContract
    │   ├── reset/         PasswordResetScreen, PasswordResetViewModel, PasswordResetContract
    │   └── common/        AuthFailureMessages, FieldErrorMessages, PasswordField
    ├── main/res/values{,-ar}/strings.xml, drawable/ (introduction illustrations)
    ├── test/kotlin/…                        # one ViewModel test per screen, messages coverage test
    └── androidTest/kotlin/…                 # SignInScreenTest

app/
├── build.gradle.kts                         # + :feature:auth, :core:datastore, splashscreen;
│                                            # − firebase-ui-auth, security-crypto
└── src/
    ├── main/AndroidManifest.xml             # MainActivity launcher; legacy auth activities removed;
    │                                        # allowBackup=false; dataExtractionRules
    ├── main/kotlin/…/shell/  MainActivity, AlphaApp, StartViewModel, LegacyHomeSessionBridge,
    │                         LegacyHomeRedirectPolicy
    ├── main/res/values/themes.xml           # + Theme.AlphaAi.Starting
    ├── main/res/values{,-ar}/strings.xml    # + bridge strings; − dead auth strings
    ├── main/res/xml/data_extraction_rules.xml   # exclude everything; backup_rules.xml deleted
    ├── main/java/…/ui/main/home/HomeActivity.kt # @AndroidEntryPoint + bridge.attach(…);
    │                                        # initial tab not added to the back stack
    ├── release/kotlin/…/shell/ShellDestinations.kt  # LegacyEntryRoute removed (empty register)
    └── test/java/…/shell/StartViewModelTest.kt, LegacyHomeRedirectPolicyTest.kt

Deleted from app/src/main:
  java/…/ui/splash/*, ui/onBoarding/*, ui/auth/login/*, ui/auth/register/*
  java/…/core/common/UserProvider.kt, data/models/User.kt, data/local/PreferencesDataSource.kt,
  data/remote/firebase/FirebaseAuthService.kt, data/repository/AuthRepository.kt,
  di/DataSourceModule.kt, di/FirebaseModule.kt, di/RepositoryModule.kt
  res/layout/activity_splash.xml, activity_on_boarding.xml, activity_login.xml,
  content_login.xml, activity_register.xml, content_register.xml
  res/xml/backup_rules.xml; unused auth strings and drawables (found by lint UnusedResources)
  NavigationDestination.OnBoarding/Login/Register in core/common/UiEvent.kt
```

**Structure Decision**:
- Domain logic lives in `:core:domain` because both `:feature:auth` and `:app` use it (start
  routing and the legacy home bridge).
- The Firebase implementation lives in `:core:data`, next to the AI repositories from spec 002.
- Encrypted storage lives in `:core:datastore`, the module reserved for it in spec 001.
- The screens live in `:feature:auth`, the first feature module.

## Implementation Notes

- **Settings.gradle**: `include(":feature:auth")`. `DependencyRules` already forbids
  feature-to-feature edges.
- **Splash theme**: `Theme.AlphaAi.Starting` (parent `Theme.SplashScreen`) uses
  `windowSplashScreenBackground` = the shell window colour, `windowSplashScreenAnimatedIcon` = the
  app icon, and `postSplashScreenTheme` = `Theme.AlphaAi.Compose`.
- **Opening the legacy home without a flash**: start `HomeActivity` with
  `ActivityOptionsCompat.makeCustomAnimation(context, 0, 0)` while the splash is still held, then
  `finish()`. `HomeActivity`'s window background matches the splash background.
- **Firebase language**: before each email (`sendPasswordResetEmail`,
  `sendEmailVerification`), call `auth.setLanguageCode` with the app's current locale (`ar` or
  `en`), so the email arrives in the user's language.
- **Logging**: no Timber call takes an email, name, uid, or exception message from Firebase.
  Log the `AuthFailure` case name only.
- **Saved state**: name and email go in `SavedStateHandle`; passwords never do (research §13).
- **Introduction illustrations**: vector drawables. Mirrored in RTL only where they show
  direction (`autoMirrored`).
- **Translations**: Modern Standard Arabic, reviewed by the owner before merge.
- **Lint**: `HardcodedText` is an error in `:feature:auth`, as in `:core:ui`.
- **Docs**: update `README.md` (status of auth) and the spec 003 app-shell contract's entry-point
  table, which [contracts/app-entry.md](contracts/app-entry.md) supersedes.
- **Size**: about 40 tasks are expected. If `/speckit-tasks` produces well over 40, move the
  legacy deletion and backup changes into a follow-up spec (research §17).

## Complexity Tracking

| Deviation | Why needed | Simpler alternative rejected because |
|---|---|---|
| A second activity (`HomeActivity`) remains after this spec, against constitution V's single-activity rule. `MainActivity` hands over to it with no frame drawn, and it returns to `MainActivity` on sign-out. | Spec 005 owns the home screen. The spec says this feature lands on the existing home (Assumptions). | A Compose home now would duplicate spec 005. Embedding the legacy home (Fragments, Data Binding) in Compose through `AndroidView` is risky for one cycle. The bridge and the second activity are deleted by spec 005. |
| The back-button fix in the legacy `HomeActivity` (the first tab was added to the fragment back stack, so back showed an empty screen instead of closing the app) has no automated test, although constitution IX asks a bug fix to include one. | `HomeActivity` is pre-2.0 code, which the governance migration clause exempts from immediate compliance. A test would need Hilt instrumented-test infrastructure (a custom runner and `hilt-android-testing`) for a screen spec 005 deletes. | Adding Hilt test infrastructure for one legacy screen costs more than the screen's remaining life. The fix is one line, checked by quickstart scenario 8, and spec 005's Compose home brings UI tests for the same back behaviour. |

Firebase's own token store was a deviation in the first draft of this plan. Constitution 2.3.0
(2026-11-18) added an explicit exception to Principle VII for tokens an approved SDK stores
itself, under the conditions this plan meets (research §3 and §11), so it is no longer a
deviation.
