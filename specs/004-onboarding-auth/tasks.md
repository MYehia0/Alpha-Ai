---

description: "Task list for spec 004, onboarding and authentication"
---

# Tasks: Onboarding and Authentication

**Input**: Design documents from `/specs/004-onboarding-auth/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/app-entry.md,
contracts/auth-screens.md, quickstart.md

**Tests**: Required. Constitution IX requires JVM tests for use cases, ViewModels, repositories,
and mappers (success and at least one failure path). The roadmap's "Done when" requires
ViewModel and use case tests. Each test task sits next to the code it covers.

**Organization**: tasks are grouped by user story so each story can be built and checked on its
own. Package root for new code is `com.example.alpha_ai`; `…` in a path stands for
`src/main/kotlin/com/example/alpha_ai` (or `src/test/kotlin/…` for tests) inside the named module.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: can run in parallel (different files, no dependency on an unfinished task)
- **[Story]**: the user story the task belongs to (US1 to US5 from spec.md)

## Rules that apply to every task

- Every user-visible string goes in `values/strings.xml` **and** `values-ar/strings.xml` of the
  module that owns it (Modern Standard Arabic). No hard-coded text.
- Composables use only `:core:designsystem` tokens and components (`AlphaTextField`,
  `AlphaPrimaryButton`, `AlphaTextButton`, `AlphaTopBar`, `AlphaIconButton`, `AlphaTheme`).
  Every screen composable has `@Preview`s for content, loading, and error via `AlphaPreviews`.
- No Timber call receives an email, name, uid, password, token, or Firebase exception message;
  log only the `AuthFailure` case name (FR-024). No `android.util.Log`.
- `CancellationException` is always rethrown, never mapped to a failure.
- Implementation classes are `internal`; dispatchers and `TimeSource` are injected.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: dependencies, the feature convention plugin, and the empty `:feature:auth` module.

- [ ] T001 Add to `gradle/libs.versions.toml`, using the latest stable version of each: `androidx-datastore` (`androidx.datastore:datastore`), `androidx-datastore-preferences`, `androidx-core-splashscreen`, `androidx-hilt-lifecycle-viewmodel-compose` (`androidx.hilt:hilt-lifecycle-viewmodel-compose`), `firebase-auth` (`com.google.firebase:firebase-auth`, version from the BOM), `kotlinx-coroutines-play-services` (version ref `coroutines`), `androidx-compose-ui-test-junit4` and `androidx-compose-ui-test-manifest` (versions from the Compose BOM), `androidx-test-runner`. Do not remove anything yet (T045 does)
- [ ] T002 Create `build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt` (`alpha.android.feature`): applies `alpha.android.library`, `alpha.android.compose`, `alpha.hilt`, and `org.jetbrains.kotlin.plugin.serialization`; adds `implementation` of `:core:common`, `:core:domain`, `:core:designsystem`, `:core:ui`, `androidx-navigation-compose`, `androidx-lifecycle-runtime-compose`, `androidx-hilt-lifecycle-viewmodel-compose`, `kotlinx-serialization-json`, `timber`; `testImplementation` of `:core:testing`; `androidTestImplementation` of `androidx-compose-ui-test-junit4`, `androidx-test-runner`, and `debugImplementation` of `androidx-compose-ui-test-manifest`. Register it in `build-logic/convention/build.gradle.kts` as id `alpha.android.feature`
- [ ] T003 Create module `:feature:auth`: `include(":feature:auth")` in `settings.gradle.kts`; `feature/auth/build.gradle.kts` applying `alpha.android.feature` with namespace `com.example.alpha_ai.feature.auth`, `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`, and `lint { error += "HardcodedText" }`; empty `feature/auth/src/main/res/values/strings.xml` and `values-ar/strings.xml`; add `implementation(projects.feature.auth)` and `implementation(projects.core.datastore)` to `app/build.gradle.kts`; add `"feature/auth/src/main/res"` to the module list in `app/src/test/java/com/example/alpha_ai/StringsParityTest.kt`
- [ ] T004 [P] Wire module dependencies: `core/datastore/build.gradle.kts` + `alias(libs.plugins.kotlin.serialization)`, `implementation(libs.androidx.datastore)`, `implementation(libs.androidx.datastore.preferences)`, `implementation(libs.kotlinx.serialization.json)`, `implementation(libs.kotlinx.coroutines.core)`, `implementation(libs.timber)`, test deps (`junit`, `kotlinx-coroutines-test`), `androidTestImplementation(libs.androidx.test.junit)` + runner, and `testInstrumentationRunner`; `core/data/build.gradle.kts` + `implementation(platform(libs.firebase.bom))`, `implementation(libs.firebase.auth)`, `implementation(libs.firebase.firestore)`, `implementation(libs.kotlinx.coroutines.play.services)`; `app/build.gradle.kts` + `implementation(libs.androidx.core.splashscreen)`, `testImplementation(projects.core.testing)`, `testImplementation(libs.kotlinx.coroutines.test)`, `testImplementation(libs.turbine)`. Run `./gradlew assembleDebug` to confirm the build still passes

**Checkpoint**: the project builds with the empty module; the existing app still runs unchanged.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: domain types, storage, the Firebase repository, shared fakes, messages, and the
navigation skeleton that every story uses.

**⚠️ CRITICAL**: no user story work can begin until this phase is complete.

### Domain (`core/domain`, package `core.domain.auth`)

- [ ] T005 [P] Create `core/domain/…/core/domain/auth/AuthModels.kt` per data-model.md: `data class AuthUser(id: String, name: String?, email: String, emailConfirmed: Boolean)`; `sealed interface AuthFailure` with `NoConnection`, `InvalidCredentials`, `EmailAlreadyInUse`, `WeakPassword`, `InvalidEmail`, `TooManyAttempts`, `AccountDisabled`, `SessionExpired`, `Unknown`; `sealed interface AuthResult<out T> { Success(value), Failure(failure: AuthFailure) }`; `enum class SignInReason { SessionExpired, AccountDisabled, SignedOut }`; `sealed interface StartDestination { Introduction; data class SignIn(reason: SignInReason?); Home }`; `enum class CooldownAction { PasswordReset, EmailConfirmation }`
- [ ] T006 [P] Create `core/domain/…/core/domain/auth/Forms.kt`: `object Email { fun normalize(raw) = raw.trim().lowercase(Locale.ROOT); fun isValid(email) }` (valid = "one `@`, non-empty local part, a domain with at least one dot, no spaces"); `enum class FormField { Name, Email, Password, ConfirmPassword }`; `enum class FieldError { Empty, InvalidFormat, TooShort, Mismatch }`; `SignUpForm`, `SignInForm`, `PasswordResetForm` each with `validate(): Map<FormField, FieldError>` using the rules "name not blank after trim → else `Empty`; email not blank → `Empty`, valid → `InvalidFormat`; password ≥ 8 chars → else `TooShort`; confirm equals password → else `Mismatch`" (sign-in: email not blank and valid, password not blank; reset: email not blank and valid). Count password length in Unicode code points; `const val MIN_PASSWORD_LENGTH = 8`
- [ ] T007 [P] Create `core/domain/src/test/kotlin/…/core/domain/auth/FormsTest.kt`: every rule of T006, including trimmed/uppercase email equality, a 7- and 8-code-point password (one Arabic), whitespace-only name, mismatch, and a fully valid form returning an empty map
- [ ] T008 [P] Create `core/domain/…/core/domain/auth/AuthRepository.kt` and `AppPreferencesRepository.kt` with exactly the signatures in data-model.md "Repository interfaces" (`currentUser(): AuthUser?`, `observeSignedIn(): Flow<Boolean>`, `signIn`, `createAccount`, `saveProfile(name)`, `sendEmailConfirmation`, `sendPasswordReset(email)` — "unknown email = Success", `refreshSession`, `signOut()` — "local; never fails", `consumeConfirmationNotice(): String?`; `isIntroductionCompleted`, `setIntroductionCompleted`), plus `core/domain/…/core/domain/auth/LegacyCleanup.kt` (`interface LegacyCleanup { suspend fun runOnce() }` — "erases v1's stored password; never throws")
- [ ] T009 [P] Create `core/domain/…/core/domain/auth/ActionCooldown.kt` (`@Singleton class ActionCooldown @Inject constructor(timeSource: TimeSource.WithComparableMarks)`, `WINDOW = 60.seconds`, `start(action)`, `remaining(action): Duration` returning `Duration.ZERO` when free) and `core/domain/src/test/kotlin/…/auth/ActionCooldownTest.kt` with `TestTimeSource` (free at start, 60 s after start, 1 s left at 59 s, free at 60 s, actions independent)

### Local storage (`core/datastore`)

- [ ] T010 Create in `core/datastore/…/core/datastore/`: `SessionData.kt` (`@Serializable data class SessionData(userId: String? = null, name: String? = null, email: String? = null, emailConfirmed: Boolean = false, profileSyncPending: Boolean = false, confirmationNoticePending: Boolean = false)` with `EMPTY`); `SessionCipher.kt` (interface `encrypt(ByteArray): ByteArray`, `decrypt(ByteArray): ByteArray`); `KeystoreSessionCipher.kt` (AES-256-GCM, key alias `alpha_session_key` in `AndroidKeyStore`, generated on first use with purpose encrypt/decrypt, no user authentication; output `iv (12 bytes) || ciphertext+tag`); `SessionSerializer.kt` (DataStore `Serializer<SessionData>`; decrypt or JSON failure → `SessionData.EMPTY` and a Timber warning with no data); `SessionStore.kt` (public interface `data: Flow<SessionData>`, `update(transform)`, `clear()`; internal DataStore impl on file `files/datastore/session.enc`)
- [ ] T011 [P] Create `core/datastore/…/core/datastore/AppPreferencesStore.kt` (public interface + internal Preferences DataStore impl named `app_preferences`, keys `introduction_completed` and `legacy_cleanup_done`, both `Boolean` default `false`), `core/datastore/…/core/datastore/di/DataStoreModule.kt` (Hilt `@Singleton` bindings for `SessionStore`, `AppPreferencesStore`, `SessionCipher`), and `core/datastore/src/test/kotlin/…/core/datastore/AppPreferencesStoreTest.kt` on a temporary-folder DataStore (both keys default to `false`; set/read round trip; values persist across a new store instance on the same file)
- [ ] T012 [P] Create `core/datastore/src/test/kotlin/…/core/datastore/SessionSerializerTest.kt` with a fake XOR `SessionCipher`: round trip of a full `SessionData`; garbage bytes → `EMPTY`; cipher throwing → `EMPTY`; the encrypted bytes do not contain the email in plain text. Create `core/datastore/src/androidTest/kotlin/…/core/datastore/KeystoreSessionCipherTest.kt`: encrypt/decrypt round trip, two encryptions of the same input differ (fresh IV), tampered byte → exception

### Data (`core/data`, package `core.data.auth`)

- [ ] T013 Create `core/data/…/core/data/auth/FirebaseAuthDataSource.kt`: an internal interface over the SDK (`currentUser`, `authStateChanges(): Flow<Boolean>` from `AuthStateListener` via `callbackFlow`, `signIn` and `createUser` returning an internal `AuthTask<T>` — `interface AuthTask<T> { suspend fun await(): T; fun whenDone(block: (Result<T>) -> Unit) }`, the Firebase version wrapping the SDK `Task` with `suspendCancellableCoroutine` so `whenDone` still fires after the awaiting coroutine is cancelled — `reload`, `sendEmailVerification`, `sendPasswordResetEmail`, `signOut`, `readProfile(uid)`, `writeProfile(UserProfileDto)`), its Firebase implementation using `await()`, and `UserProfileDto.kt` (`users/{uid}` with fields `uid`, `uName`, `uEmail`, the v1 shape). Before each email call, set `FirebaseAuth.setLanguageCode` to the current app locale (`ar` or `en`)
- [ ] T014 [P] Create `core/data/…/core/data/auth/FirebaseErrorMapper.kt` mapping `(kind, errorCode, operation)` to `AuthFailure` exactly per research.md §7 (with `ERROR_USER_NOT_FOUND` → `InvalidCredentials` for sign-in, → `SessionExpired` for refresh, → success for password reset) plus a small adapter that extracts kind and code from a `Throwable` and rethrows `CancellationException`; and `core/data/src/test/kotlin/…/auth/FirebaseErrorMapperTest.kt` covering every row of the table and the per-operation cases
- [ ] T015 Create `core/data/…/core/data/auth/FirebaseAuthRepository.kt` implementing `AuthRepository` with `FirebaseAuthDataSource`, `SessionStore`, `FirebaseErrorMapper`, and an injected `@IoDispatcher`: `signIn` caches the profile (name from Firestore, best effort); `saveProfile` writes `users/{uid}` and the cache, setting `profileSyncPending` on failure; `sendEmailConfirmation` sets `confirmationNoticePending` on success; `refreshSession` reloads, updates `emailConfirmed`, and retries a pending profile sync; `signOut` calls the SDK sign-out and `SessionStore.clear()` and never throws; a cache whose `userId` differs from the current uid is discarded. Cancellation per data-model.md "Cancellation": `signIn` and `createAccount(email, password, name)` await their `AuthTask`; on `CancellationException` they register `task.whenDone { … }` and rethrow; a late success launches clean-up in the injected `@ApplicationScope` scope — for sign-in `signOut()`, for create-account write `users/{uid}` with the name (best effort) and then `signOut()`. Also create `DataStoreAppPreferencesRepository.kt` (a read failure returns `false`, a write failure is logged by case name; neither throws)
- [ ] T016 Add an `@ApplicationScope` qualifier to `core/common/src/main/kotlin/com/example/alpha_ai/core/common/Dispatchers.kt` (next to `@IoDispatcher`). Create `core/data/…/core/data/auth/di/AuthModule.kt` providing `FirebaseAuth`, `FirebaseFirestore`, `TimeSource.WithComparableMarks` (`TimeSource.Monotonic`), an `@ApplicationScope @Singleton CoroutineScope` (`SupervisorJob()` plus the injected `@IoDispatcher`; it lives as long as the process, so it is a lifecycle-tied scope, not `GlobalScope`), and binding `AuthRepository`, `AppPreferencesRepository`, `FirebaseAuthDataSource`. In the same change delete `provideFirebaseAuth` and `provideFirebaseFirestore` from `app/src/main/java/com/example/alpha_ai/di/FirebaseModule.kt` so Hilt has one binding each (the legacy `FirebaseAuthService` keeps working until T044)
- [ ] T017 Create `core/data/src/test/kotlin/…/auth/FirebaseAuthRepositoryTest.kt` with a fake `FirebaseAuthDataSource` and an in-memory `SessionStore`: sign-in success caches the profile; sign-in network failure → `NoConnection`; profile write failure sets `profileSyncPending` and still succeeds; refresh retries the pending sync; unknown email on reset → `Success`; sign-out clears the store even when the SDK throws; a cache for another uid is ignored; cancellation with a fake `AuthTask` and a `TestScope` as the application scope — sign-in cancelled then completing successfully → signed out; create-account cancelled then completing → profile written with the name, then signed out; cancelled then failing → nothing happens; `CancellationException` is rethrown to the caller. Also create `core/data/src/test/kotlin/…/auth/DataStoreAppPreferencesRepositoryTest.kt` with an in-memory `AppPreferencesStore`: introduction defaults to not completed; `setIntroductionCompleted` then reads `true`; a store read failure returns `false` rather than throwing (constitution IX: success and failure path)
- [ ] T018 [P] Create `core/testing/…/core/testing/auth/FakeAuthRepository.kt` (scriptable results per call, records calls, `MutableStateFlow` for `observeSignedIn`) and `FakeAppPreferencesRepository.kt`

### Shared presentation (`feature/auth`)

- [ ] T019 [P] Create `feature/auth/…/feature/auth/common/AuthFailureMessages.kt` (`@StringRes` for every `AuthFailure`, texts from data-model.md "AuthFailure" table), `FieldErrorMessages.kt` (`(FormField, FieldError) → @StringRes`), `PasswordField.kt` (wraps `AlphaTextField`; hidden by default; show/hide `AlphaIconButton` with a content description), strings in `feature/auth/src/main/res/values{,-ar}/strings.xml`, and `feature/auth/src/test/kotlin/…/common/MessagesCoverageTest.kt` asserting every `AuthFailure` case and every `FieldError` has a message
- [ ] T020 Create `feature/auth/…/feature/auth/navigation/AuthRoutes.kt` (`IntroductionRoute`, `SignInRoute(reason: SignInReason? = null)`, `SignUpRoute`, `PasswordResetRoute(email: String = "")`, all `@Serializable`) and `AuthGraph.kt` (`fun NavGraphBuilder.authGraph(navController, onAuthenticated: () -> Unit)`, empty for now; stories add destinations). Change `app/src/main/kotlin/com/example/alpha_ai/shell/AlphaApp.kt` to `AlphaApp(startRoute: Any, onAuthenticated: () -> Unit, navController = rememberNavController())` calling `register(navController)` then `authGraph(navController, onAuthenticated)`, and update its caller in `MainActivity.kt` to pass `startRoute = ShellDestinations.startRoute` and `onAuthenticated = {}` for now (T025 replaces both)

**Checkpoint**: domain, storage, and data compile and their tests pass (`./gradlew :core:domain:test :core:data:testDebugUnitTest :core:datastore:testDebugUnitTest :feature:auth:testDebugUnitTest`).

---

## Phase 3: User Story 1 - Returning user goes straight to home (Priority: P1) 🎯 MVP

**Goal**: `MainActivity` is the launcher, decides the start destination before the first frame,
sends a signed-in user to the legacy home with no flash, erases v1's stored password, and signs
the user out if the session became invalid.

**Independent Test**: install the v1 build, sign in, install this build over it, and open the
app 20 times, online and offline: home is the first screen every time and
`alpha_ai_secure_prefs.xml` is gone (quickstart scenarios 8, 9, 16, 17).

- [ ] T021 [P] [US1] Create `core/data/…/core/data/auth/LegacyAuthCleanup.kt`: once (guarded by `legacy_cleanup_done`), `context.deleteSharedPreferences("alpha_ai_secure_prefs")` and delete Keystore alias `_androidx_security_master_key_` if present, never touching the Firebase session; it implements the domain `LegacyCleanup` interface from T008 and is bound in `core/data/…/auth/di/AuthModule.kt`. Test in `core/data/src/test/kotlin/…/auth/LegacyAuthCleanupTest.kt` with fakes: runs once, sets the flag, a failure still sets the flag and does not throw
- [ ] T022 [US1] Create `core/domain/…/core/domain/auth/usecase/ResolveStartDestinationUseCase.kt` ("LegacyCleanup.runOnce(); currentUser != null → mark introduction completed; Home; introductionCompleted → SignIn(null); otherwise → Introduction") and `RefreshSessionUseCase.kt` (calls `refreshSession`; `NoConnection` and `Unknown` → returns "still signed in" and logs the case name through Timber, so the failure is not silently dropped (constitution VIII); `SessionExpired` / `AccountDisabled` → `signOut()` and returns that reason). Tests in `core/domain/src/test/kotlin/…/auth/usecase/` covering all three start outcomes, the "signed in marks introduction completed" rule, and each refresh outcome, using the `:core:testing` fakes
- [ ] T023 [US1] Create `app/src/main/kotlin/com/example/alpha_ai/shell/StartViewModel.kt` (`@HiltViewModel`; state `Resolving | Resolved(StartDestination)`; resolves once; after a 2 s timeout decides from `AuthRepository.currentUser()` alone — `Home` if present, otherwise `SignIn(null)` — so a slow disk never sends a signed-in user to sign-in (FR-002); accepts an optional `start_reason` string from `SavedStateHandle` and uses `SignIn(reason)` only if it equals a `SignInReason` name, ignoring anything else) and `app/src/test/java/com/example/alpha_ai/shell/StartViewModelTest.kt` (each destination; timeout with virtual time both with and without a current user; valid and invalid `start_reason`)
- [ ] T024 [US1] Add `Theme.AlphaAi.Starting` (parent `Theme.SplashScreen`; `windowSplashScreenBackground` = `@color/shell_window_background`; `windowSplashScreenAnimatedIcon` = `@mipmap/icon_app_4`; `postSplashScreenTheme` = `@style/Theme.AlphaAi.Compose`) to `app/src/main/res/values/themes.xml`. In `app/src/main/AndroidManifest.xml` make `MainActivity` `exported="true"` with the MAIN/LAUNCHER intent filter and theme `Theme.AlphaAi.Starting`, and remove the launcher intent filter from `SplashActivity` (set `exported="false"`; it is deleted in T044)
- [ ] T025 [US1] Rewrite `app/src/main/kotlin/com/example/alpha_ai/shell/MainActivity.kt` per contracts/app-entry.md "MainActivity start sequence": `installSplashScreen()` with `setKeepOnScreenCondition { state is Resolving }`; launched through the `GalleryLauncher` alias component → `AlphaApp(startRoute = ShellDestinations.startRoute)` without resolving; `Home` → start `HomeActivity` with `ActivityOptionsCompat.makeCustomAnimation(this, 0, 0)` and `finish()` without `setContent`; `Introduction` / `SignIn(reason)` → `setContent { AlphaApp(startRoute = IntroductionRoute or SignInRoute(reason), onAuthenticated = { start HomeActivity; finish() }) }`. In `app/src/release/kotlin/com/example/alpha_ai/shell/ShellDestinations.kt` delete `LegacyEntryRoute`, leave `register` empty, and make `startRoute` `SignInRoute()`; in debug keep `GalleryRoute`
- [ ] T026 [US1] Create `app/src/main/kotlin/com/example/alpha_ai/shell/LegacyHomeRedirectPolicy.kt`, a pure Kotlin class implementing the table in contracts/app-entry.md "LegacyHomeRedirectPolicy" (`onSignOutConfirmed()`, `onRefreshResult(…)`, `onSignedInChanged(Boolean)` → `Redirect(SignInReason) | Stay`; at most one `Redirect` per instance; `onSignedInChanged(false)` returns `Stay` while a sign-out is in progress), with `app/src/test/java/com/example/alpha_ai/shell/LegacyHomeRedirectPolicyTest.kt` (each row; sign-out followed by `onSignedInChanged(false)` yields exactly one `Redirect(SignedOut)`; refresh `SessionExpired` followed by `onSignedInChanged(false)` yields one redirect; a new policy instance after a redirect can redirect again). The policy and the bridge are unscoped (no scope annotation), so each `HomeActivity` gets new instances (contracts/app-entry.md "Lifetime"). Then create `app/src/main/kotlin/com/example/alpha_ai/shell/LegacyHomeSessionBridge.kt` (contracts/app-entry.md) with `attach(activity, signOutButton, snackbarAnchor)`: on every `onStart` run `RefreshSessionUseCase` and pass the result to the policy; collect `observeSignedIn()` with `repeatOnLifecycle(STARTED)` into the policy; on `Redirect(reason)` start `MainActivity` with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` and extra `start_reason = reason.name`. Annotate `app/src/main/java/com/example/alpha_ai/ui/main/home/HomeActivity.kt` with `@AndroidEntryPoint`, field-inject the bridge, and call `attach` in `onCreate` with `binding`'s app bar `btn_logout` and root (no layout changes). In the same file fix FR-021: `showTasksFragment()` must not call `addToBackStack(null)` for the tab selected at start, so one back press on home closes the app (no automated test, see plan Complexity Tracking; checked by quickstart scenario 8). Add the sign-in notices for `SessionExpired`, `AccountDisabled`, `SignedOut` to `feature/auth` strings (shown by sign-in in T036)
- [ ] T027 [US1] Backup (FR-023): in `app/src/main/AndroidManifest.xml` set `android:allowBackup="false"` and remove `android:fullBackupContent`; replace `app/src/main/res/xml/data_extraction_rules.xml` with rules excluding every domain (`root`, `file`, `database`, `sharedpref`, `external`) under both `<cloud-backup>` and `<device-transfer>`; delete `app/src/main/res/xml/backup_rules.xml`

**Checkpoint**: a signed-in user (v1 or console-created) reaches home with no flash, online and
offline; a disabled account is sent to sign-in once US3 exists. Signed-out starts show the auth
graph, whose screens arrive in US2 and US3.

---

## Phase 4: User Story 2 - New user is introduced once, signs up, and reaches home (Priority: P1)

**Goal**: three-page skippable introduction shown once; sign-up with validation; home with a
one-time "confirmation link sent" message.

**Independent Test**: fresh install → introduction → skip → create account → home with the
message once; reopen → no introduction (quickstart scenarios 1–7).

- [ ] T028 [P] [US2] Create `core/domain/…/auth/usecase/CompleteIntroductionUseCase.kt`, `SignUpUseCase.kt` (research.md §8: normalize the email with `Email.normalize` (FR-020) → validate → `createAccount(email, password, name.trim())` → `saveProfile(name.trim())`, failure kept as pending and logged by case name → `sendEmailConfirmation`, failure logged by case name and otherwise ignored → `Success`; returns `FieldErrors(map)` or `AuthResult`; reports progress so the ViewModel knows when the account exists), and `ConsumeConfirmationNoticeUseCase.kt`, with tests in `core/domain/src/test/kotlin/…/auth/usecase/` (`"  Ali@Example.COM "` reaches `createAccount` as `"ali@example.com"`; field errors stop before any call; `EmailAlreadyInUse` passes through; profile failure still succeeds; confirmation-email failure still succeeds; notice returned once, then `null`; `CompleteIntroductionUseCase` sets the flag, and a store write failure is logged and does not throw)
- [ ] T029 [US2] Create `feature/auth/…/feature/auth/introduction/IntroductionContract.kt` and `IntroductionViewModel.kt` per contracts/auth-screens.md "Introduction" (`page` 0..2, `pageCount = 3`; intents `NextPage`, `PreviousPage`, `GoToPage(n)`, `Skip`, `Finish`; effect `ToSignIn` after `CompleteIntroductionUseCase`), and `feature/auth/src/test/kotlin/…/introduction/IntroductionViewModelTest.kt` (page bounds, Skip and Finish both complete and emit `ToSignIn` once; a failed save still emits `ToSignIn`, so at worst the user sees the introduction once more)
- [ ] T030 [US2] Create `feature/auth/…/feature/auth/introduction/IntroductionScreen.kt`: `HorizontalPager` with three pages (grammar correction in Arabic and English; the supporting tools; history and getting started) each illustrated by one icon from `material-icons-core` (`Icons.Filled.Edit`, `Icons.Filled.Build`, `Icons.Filled.DateRange`) drawn large inside a circle tinted with `MaterialTheme.colorScheme.primaryContainer`, with `contentDescription = null` because the page text describes it (the owner may swap in final artwork later without changing the screen's structure), page indicator, Skip on every page, "Get started" on the last; `BackHandler` enabled when `page > 0`; page motion from design-system motion tokens and reduced motion honoured; previews. Register `composable<IntroductionRoute>` in `AuthGraph.kt`, navigating on `ToSignIn` with `navigate(SignInRoute()) { popUpTo<IntroductionRoute> { inclusive = true } }`
- [ ] T031 [US2] Create `feature/auth/…/feature/auth/signup/SignUpContract.kt` and `SignUpViewModel.kt` per contracts/auth-screens.md "Sign-up" (name and email in `SavedStateHandle`, passwords never; editing a field clears its error and the failure; `Submit` ignored while `submitting`; `cancellable` true only while the account is being created; `Cancel` cancels the job and keeps every field — nothing more, because the repository (T015) signs out a request that completes late; effects `Authenticated`, `ToSignIn`), and `feature/auth/src/test/kotlin/…/signup/SignUpViewModelTest.kt` (field errors, success → `Authenticated`, `EmailAlreadyInUse` → failure shown and text kept, double submit sends once, rotation-equivalent state retained, Cancel during account creation → not submitting, fields kept, no `Authenticated`, the repository call was cancelled; Cancel hidden after the account exists)
- [ ] T032 [US2] Create `feature/auth/…/feature/auth/signup/SignUpScreen.kt` (name, email, `PasswordField` ×2 with the hint "At least 8 characters", IME Next/Done, error under each field, failure message with a "Sign in" action for `EmailAlreadyInUse`, submit with progress and a Cancel text button while `cancellable`; scrollable at 200% font; previews) and register `composable<SignUpRoute>` in `AuthGraph.kt` (`Authenticated` → `onAuthenticated()`, `ToSignIn` → `popBackStack()`)
- [ ] T033 [US2] Extend `app/src/main/kotlin/com/example/alpha_ai/shell/LegacyHomeSessionBridge.kt`: on `onStart`, call `ConsumeConfirmationNoticeUseCase`; when it returns an email, show the existing Material snackbar (long duration) "We sent a confirmation link to %1$s" from new strings in `app/src/main/res/values{,-ar}/strings.xml`

**Checkpoint**: a new user can go from first launch to home; the introduction never shows again.
Sign-up is reachable once sign-in exists (US3); until then test it by temporarily starting at
`SignUpRoute` in a debug run.

---

## Phase 5: User Story 3 - Existing user signs in (Priority: P1)

**Goal**: sign-in with clear, localized errors; the bottom of the auth back stack.

**Independent Test**: with an existing account, sign in and reach home; check the wrong
password, unknown email, offline, and double-tap cases (quickstart scenarios 11–13).

- [ ] T034 [P] [US3] Create `core/domain/…/auth/usecase/SignInUseCase.kt` (normalize email, validate `SignInForm`, `signIn`; returns `FieldErrors` or `AuthResult<AuthUser>`) with `core/domain/src/test/kotlin/…/auth/usecase/SignInUseCaseTest.kt` (normalization, field errors stop before the call, `InvalidCredentials`, `NoConnection`, success)
- [ ] T035 [US3] Create `feature/auth/…/feature/auth/signin/SignInContract.kt` and `SignInViewModel.kt` per contracts/auth-screens.md "Sign-in" (state `email`, `password`, `passwordVisible`, `fieldErrors`, `submitting`, `cancellable`, `failure`, `notice: SignInReason?` from the route argument; intents `EmailChanged`, `PasswordChanged`, `TogglePasswordVisibility`, `Submit`, `Cancel`, `ForgotPassword`, `CreateAccount`, `DismissNotice`; effects `Authenticated`, `ToPasswordReset(email)`, `ToSignUp`; `Cancel` cancels the request job and keeps the fields; the late-success sign-out is the repository's job, T015), and `feature/auth/src/test/kotlin/…/signin/SignInViewModelTest.kt` asserting state and effect sequences for success, each failure, double submit, Cancel while submitting (no `Authenticated`, fields kept, the repository call was cancelled), notice dismissal, and `ForgotPassword` carrying the typed email
- [ ] T036 [US3] Create `feature/auth/…/feature/auth/signin/SignInScreen.kt` (notice banner for `notice`; email, `PasswordField`; one message for `InvalidCredentials`; `TooManyAttempts` message with a "Reset password" action; "Forgot password" and "Create account" text buttons; submit with progress and a Cancel text button while `cancellable`; previews) and register `composable<SignInRoute>` in `AuthGraph.kt` (`Authenticated` → `onAuthenticated()`, `ToSignUp` → `navigate(SignUpRoute)`; `ToPasswordReset` wired in T040)
- [ ] T037 [US3] Create `feature/auth/src/androidTest/kotlin/…/signin/SignInScreenTest.kt` (Compose UI test on the stateless screen): an empty submit shows the field error; `submitting = true` disables the button; an `InvalidCredentials` failure shows its message; password is hidden until the toggle is tapped

**Checkpoint**: Stories 1–3 work together: first launch → introduction → sign-in ↔ sign-up →
home → relaunch home.

---

## Phase 6: User Story 4 - User resets a forgotten password (Priority: P2)

**Goal**: request a reset email from sign-in, with the same confirmation for any email and a
60 s wait between sends.

**Independent Test**: from sign-in, request a reset for an existing and an unknown email, follow
the link, sign in with the new password (quickstart scenarios 14–15).

- [ ] T038 [P] [US4] Create `core/domain/…/auth/usecase/RequestPasswordResetUseCase.kt` (normalize, validate `PasswordResetForm`, check `ActionCooldown.remaining(PasswordReset)` → `CoolingDown(remaining)`, `sendPasswordReset`, start the cooldown on success; returns `Sent | CoolingDown | FieldErrors | Failure`) with `core/domain/src/test/kotlin/…/auth/usecase/RequestPasswordResetUseCaseTest.kt` using `TestTimeSource` (sent, cooling down at 30 s, free at 60 s, failure does not start the cooldown)
- [ ] T039 [US4] Create `feature/auth/…/feature/auth/reset/PasswordResetContract.kt` and `PasswordResetViewModel.kt` per contracts/auth-screens.md "Password reset" (email prefilled from `PasswordResetRoute.email`; `sent`; `cooldownSeconds` counting down once per second with `delay(1.seconds)` until 0; `Cancel` while `submitting` cancels the job and keeps the email), and `feature/auth/src/test/kotlin/…/reset/PasswordResetViewModelTest.kt` (prefill, sent state, countdown on virtual time, submit disabled during cooldown, `NoConnection` failure, Cancel while submitting → not submitting and not `sent`)
- [ ] T040 [US4] Create `feature/auth/…/feature/auth/reset/PasswordResetScreen.kt` (email field; after sending, the same confirmation text for any email asking to check the inbox and spam folder, and "Back to sign in"; submit reads "Send again in %1$d s" during cooldown; a Cancel text button while `cancellable`; previews), register `composable<PasswordResetRoute>` in `AuthGraph.kt` (`ToSignIn` → `popBackStack()`), and wire sign-in's `ToPasswordReset(email)` → `navigate(PasswordResetRoute(email))`

**Checkpoint**: a forgotten password can be reset end to end.

---

## Phase 7: User Story 5 - User signs out (Priority: P2)

**Goal**: the home sign-out button works, with confirmation, and leaves nothing behind.

**Independent Test**: sign out from home, press back, reopen: sign-in each time; offline
sign-out works (quickstart scenario 10 and the storage checks).

- [ ] T041 [P] [US5] Create `core/domain/…/auth/usecase/SignOutUseCase.kt` (calls `AuthRepository.signOut()`; never fails) with `core/domain/src/test/kotlin/…/auth/usecase/SignOutUseCaseTest.kt` (calls sign-out; introduction status untouched)
- [ ] T042 [US5] Extend `app/src/main/kotlin/com/example/alpha_ai/shell/LegacyHomeSessionBridge.kt`: `btn_logout` click → confirmation dialog through the existing `showMaterial3Dialog` extension ("Sign out?", "Sign out", "Cancel", strings in `app/src/main/res/values{,-ar}/strings.xml`, with a content description for the icon button set in code); confirm → call `LegacyHomeRedirectPolicy.onSignOutConfirmed()` **before** `SignOutUseCase`, so the auth-state observer from T026 does not open a second `MainActivity` with `SessionExpired` → then open `MainActivity` once with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK` and `start_reason = SignedOut`

**Checkpoint**: all five stories work.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T043 Audit logging across `core/data/…/auth`, `core/datastore`, `feature/auth`, and `app/…/shell` for FR-024: no Timber call takes an email, name, uid, password, token, or exception message; no `android.util.Log` or `println` in new code
- [ ] T044 Delete the legacy auth code: `app/src/main/java/com/example/alpha_ai/ui/splash/`, `ui/onBoarding/`, `ui/auth/login/`, `ui/auth/register/`, `core/common/UserProvider.kt`, `data/models/User.kt`, `data/local/PreferencesDataSource.kt`, `data/remote/firebase/FirebaseAuthService.kt`, `data/repository/AuthRepository.kt`, `di/DataSourceModule.kt`, `di/FirebaseModule.kt`, `di/RepositoryModule.kt`; layouts `activity_splash.xml`, `activity_on_boarding.xml`, `activity_login.xml`, `content_login.xml`, `activity_register.xml`, `content_register.xml`; their four `<activity>` entries in `AndroidManifest.xml`; `OnBoarding`, `Login`, `Register` from `NavigationDestination` in `core/common/UiEvent.kt`; and the commented `UserProvider` imports in `HistoryFragment.kt` and `WordsViewModel.kt`
- [ ] T045 Remove `firebase-ui-auth` and `androidx-security-crypto` from `gradle/libs.versions.toml` and `app/build.gradle.kts`; run `./gradlew :app:lintDebug` and delete the auth strings (in `values` and `values-ar`) and drawables it reports as `UnusedResources`
- [ ] T046 [P] Update docs: `README.md` (auth status and the new module), the "Entry points" section of `specs/003-design-system-shell/contracts/app-shell.md` (point to `specs/004-onboarding-auth/contracts/app-entry.md`), and spec 004's status row in `ROADMAP.md`
- [ ] T047 Run the automated checks in `specs/004-onboarding-auth/quickstart.md` (unit tests, lint, connected tests) and every manual scenario plus the start-up time check (SC-002, `TotalTime` ≤ 2000 ms) and the storage and backup checks, in English and Arabic, light and dark, and at the largest font; record the results in the quickstart file

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none. T002 → T003; T001 before T002–T004.
- **Foundational (Phase 2)**: after Setup; blocks every story. T005 → T006, T008; T008 → T013–T018;
  T010 → T011, T012, T015; T013 → T015 → T016 → T017; T005 → T019, T020.
- **US1 (Phase 3)**: after Foundational. T021 → T022 → T023 → T025; T024 before T025; T026
  after T022.
- **US2 (Phase 4)**, **US3 (Phase 5)**: after Foundational; independent of each other at the
  code level. US2's sign-up is reached from sign-in in the running app, so US3 makes US2
  reachable without a debug start route.
- **US4 (Phase 6)**: after US3 (it is opened from sign-in, T040 edits sign-in's wiring).
- **US5 (Phase 7)**: after US1 (it extends the bridge from T026).
- **Polish (Phase 8)**: after all stories. T044 must follow US2 and US3 (the new screens replace
  the deleted ones). T045 after T044.

### Within each story

Use case and its test → ViewModel and its test → screen and graph registration → shell or
bridge wiring.

### Parallel opportunities

- Setup: T004 alongside T002/T003.
- Foundational: T005, T006/T007, T008, T009, T018 (after T008), T019 together; T010–T012 in
  parallel with T013–T014.
- After Foundational: US1, US2, and US3 can be built in parallel; their files do not overlap
  except `AuthGraph.kt` (T030, T032, T036, T040 each add one `composable` block) and
  `LegacyHomeSessionBridge.kt` (T026 → T033 → T042, in that order).
- Every use case task marked [P] (T021, T028, T034, T038, T041) can start as soon as Phase 2 is done.

---

## Parallel Example: User Story 2

```text
Task: "T028 CompleteIntroduction, SignUp, ConsumeConfirmationNotice use cases + tests in core/domain"
Task: "T029 IntroductionContract + IntroductionViewModel + test in feature/auth"
# then, in parallel:
Task: "T030 IntroductionScreen + graph registration"
Task: "T031 SignUpContract + SignUpViewModel + test"
```

## Parallel Example: Foundational

```text
Task: "T005 AuthModels.kt"          Task: "T009 ActionCooldown + test"
Task: "T006 Forms.kt"               Task: "T010 SessionStore and cipher"
Task: "T008 repository interfaces"  Task: "T014 FirebaseErrorMapper + test"
```

---

## Implementation Strategy

### MVP first (User Story 1)

1. Phases 1 and 2.
2. Phase 3 (US1): returning users get the instant, flash-free start, and v1's stored password
   is erased. This alone fixes the most serious security problem in the current flow.
3. Validate with a v1-signed-in account (quickstart 8, 9, 17).

### Incremental delivery

1. US3 (sign-in) next, so the signed-out path is usable, then US2 (introduction and sign-up).
   Both are P1; building sign-in first means sign-up is reachable without a debug start route.
2. US4 (reset) and US5 (sign-out).
3. Polish: delete the legacy code only when every replacement works, then run the full
   quickstart.

The branch is merged once, after Phase 8 (one spec, one merge). Commit after each task or
logical group with Conventional Commits.

### Size

47 tasks, slightly above the roadmap's guideline of about 40. The overrun is the test tasks the
constitution requires and the legacy deletion; splitting would leave a branch with two
start-up paths, so the spec stays whole. If you want it smaller, move T027 (backup) and
T044–T045 (deletion) into a follow-up spec.

---

## Notes

- [P] = different files, no dependency on an unfinished task.
- `AuthGraph.kt` and `LegacyHomeSessionBridge.kt` are edited by several tasks; do those tasks in
  ID order.
- Tests use fakes from `:core:testing` and never call live Firebase.
- Stop at any checkpoint to validate a story on a device.
