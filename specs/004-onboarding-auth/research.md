# Research: Onboarding and Authentication

Decisions for spec 004. Each section gives the decision, why, and what was rejected.

## Findings in the current code (2026-11-18)

- Sign-in uses Firebase Auth (email and password) and a Firestore document `users/{uid}` with
  the fields `uid`, `uName`, `uEmail`. Accounts made by v1 must keep working, so both stay.
- "Remember me" stores the email **and the password** in `EncryptedSharedPreferences` file
  `alpha_ai_secure_prefs`, then signs in again with them on launch (`AuthRepository.attemptAutoLogin`).
  This breaks constitution VII and FR-022.
- `SplashViewModel` waits a fixed 2 s before deciding where to go, then opens onboarding,
  which shows for any signed-out user every time. Nothing records that onboarding was seen.
- The sign-out button in `layout_appbar.xml` (`btn_logout`, shown on home) has **no click
  handler**: sign-out does not work today.
- `UserProvider` is written by `SplashViewModel` only; every reader is commented out.
- `firebase-ui-auth` is declared but never used; it is only the route by which `firebase-auth`
  reaches the classpath. `security-crypto` is used only by `PreferencesDataSource`.
- `backup_rules.xml` and `data_extraction_rules.xml` are the generated samples (nothing is
  excluded) and `allowBackup="true"`.

## 1. Sign-in service

**Decision**: keep Firebase Auth with email and password, declared directly as
`firebase-auth` from the existing Firebase BOM; remove `firebase-ui-auth`.

**Rationale**: the constitution names Firebase Auth; v1 accounts live there; password reset and
confirmation emails are built in. Declaring `firebase-auth` directly removes an unused UI
library and its transitive dependencies.

**Alternatives**: FirebaseUI's prebuilt screens (not Compose, not in the design system, can't
meet FR-017's message list); a custom backend (no reason to move accounts).

**Firebase console settings** (manual, listed in quickstart): keep **email enumeration
protection** on (the default for projects created since September 2023). With it on, Firebase
answers a wrong email and a wrong password with the same `ERROR_INVALID_CREDENTIAL`, and
`sendPasswordResetEmail` succeeds for unknown emails, which is exactly FR-010 and FR-013. The
app treats both cases the same way anyway, so the behaviour is right even if it is turned off.
Set the email templates' language to follow the app (`FirebaseAuth.setLanguageCode` with the
current app language before each email is sent).

## 2. Layering

**Decision**:

| Layer | Module | Contents |
|---|---|---|
| Domain | `:core:domain` package `auth` | `AuthUser`, `AuthFailure`, `AuthResult`, `StartDestination`, form validation, `AuthRepository` and `AppPreferencesRepository` interfaces, use cases, `ActionCooldown` |
| Data | `:core:data` package `auth` | `FirebaseAuthRepository`, `FirebaseAuthDataSource` (thin wrapper over the SDK), `FirebaseErrorMapper`, `LegacyAuthCleanup`, Hilt bindings |
| Local storage | `:core:datastore` | `SessionStore` (encrypted), `AppPreferencesStore` (plain), `SessionCipher` |
| Presentation | `:feature:auth` (new) | Introduction, sign-in, sign-up, password reset: screens, MVI ViewModels, routes, `authGraph` |
| Shell | `:app` | `StartViewModel`, start routing, splash screen, the temporary bridge to the legacy home |

**Rationale**: matches the roadmap plan prompt and constitution I and II. The repository
interface sits in domain so the ViewModels never see Firebase. Use cases live in domain because
`:feature:auth` and `:app` (start routing, legacy home bridge) both need them.

**Alternatives**: a separate `:core:auth` module (one more module for one interface; rejected);
use cases inside `:feature:auth` (the shell and the legacy home bridge would then depend on a
feature module, which DR-2/DR-1 forbid for core and is awkward for `:app`).

## 3. What is stored on the phone, and how it is encrypted

**Decision**:

- **Firebase's own session** (its refresh token in its private storage) is the source of truth
  for "signed in". The app does not copy the token.
- **`SessionStore`** (our data) holds a cached profile: user id, name, email, email confirmed,
  plus two flags: `profileSyncPending` and `confirmationNoticePending`. It is a typed DataStore
  whose file is encrypted with AES-256-GCM using a key generated in the **Android Keystore**
  (`KeyGenParameterSpec`, purpose encrypt/decrypt, no user authentication, not exportable).
  The serializer stores `iv || ciphertext`; the plaintext is a kotlinx.serialization JSON object.
- **`AppPreferencesStore`** holds `introductionCompleted: Boolean` in a plain Preferences
  DataStore. It is not sensitive and must survive sign-out. Spec 005 adds language and theme
  here.
- No password is ever written anywhere (FR-022).

**Rationale**: constitution VII requires Keystore-backed encryption for persisted auth data.
`EncryptedSharedPreferences` (security-crypto) is deprecated upstream and stuck in alpha; a
small `Serializer` with a Keystore AES key needs only `androidx.datastore`. Keeping Firebase as
the source of truth means the start decision needs no network call (FR-002).

**Alternatives**: keep `EncryptedSharedPreferences` (deprecated); Tink (`tink-android`) for the
DataStore AEAD (a second crypto library for one file); storing the Firebase ID token ourselves
(duplicates what the SDK manages and refreshes).

**Decrypt failure** (key lost after a restore or a Keystore reset): the store is treated as
empty and rewritten; the user stays signed in through Firebase and the profile cache is refilled
from Firestore. The failure is logged through Timber without any data.

**Firebase's internal token store** is in app-private storage and not encrypted by the app;
the SDK offers no supported way to supply our own storage. Constitution 2.3.0 allows this under
Principle VII's SDK exception, whose conditions this design meets: the app never copies the
token, the storage is excluded from backup and device transfer (§11), and `signOut()` clears it.

## 4. Start destination before the first frame

**Decision**: `MainActivity` becomes the launcher. It calls `installSplashScreen()`
(`androidx.core:core-splashscreen`) and keeps the system splash on screen while
`StartViewModel` resolves `StartDestination` through `ResolveStartDestinationUseCase`:

1. Firebase has a current user → `Home`. Mark the introduction completed (a v1 user or a user
   who signed in before never sees it again, FR-003 and Story 5 scenario 3).
2. Else the introduction is completed → `SignIn`.
3. Else → `Introduction`.

Only local reads are involved (Firebase's cached user, one DataStore read), so the splash is
held for tens of milliseconds. If storage misbehaves, a 2 s timeout decides from Firebase's
cached user alone (a synchronous local read): `Home` if there is one, otherwise `SignIn`, so a
signed-in user is never sent to sign-in by a slow disk (FR-002).

For `Introduction` and `SignIn`, the `NavHost` is composed with that route as its start
destination, so nothing else is ever drawn. For `Home`, see §5.

**Rationale**: constitution V and FR-001. The splash screen API is the supported way to delay the
first frame on Android 12+, and the library backports it to API 26.

**Alternatives**: a Compose "loading" route that then navigates (draws a screen the user is
redirected away from, which is what FR-001 forbids); keeping `SplashActivity` (an extra
activity hop and the fixed 2 s delay); `ViewTreeObserver.OnPreDrawListener` by hand (what the
library already does).

## 5. Reaching the legacy home, and coming back from it

**Decision**: until spec 005 makes home a Compose destination, "Home" means the legacy
`HomeActivity`.

- **Start**: when the resolved destination is `Home`, `MainActivity` starts `HomeActivity`
  and finishes **without calling `setContent`**, with the splash still held, and with no
  transition animation. The first screen drawn is home.
- **After sign-in or sign-up**: the screen emits `UiEffect.Authenticated`; the shell starts
  `HomeActivity` and finishes `MainActivity`, so back on home closes the app (FR-021).
- **Sign-out and expired session**: `HomeActivity` starts `MainActivity` with
  `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, so nothing signed-in stays on the back
  stack (FR-016). An optional extra `start_reason` carries the enum name `SessionExpired`; the
  shell validates it against the enum and ignores anything else (constitution VII).

`HomeActivity` gets a small, temporary class in `:app`, `LegacyHomeSessionBridge`, injected with
Hilt (`@AndroidEntryPoint` on `HomeActivity`; field injection is allowed in entry points). It:

- wires the existing `btn_logout` to a confirmation dialog (the existing
  `showMaterial3Dialog` extension) and then `SignOutUseCase`;
- shows the one-time "confirmation link sent to <email>" message (existing Material snackbar)
  using `ConsumeConfirmationNoticeUseCase`;
- on `onStart`, runs `RefreshSessionUseCase` and, on `SessionExpired` or `AccountDisabled`,
  signs out and returns to `MainActivity` with the reason.

The decisions (which reason to send, and whether to send at all) live in a small pure class,
`LegacyHomeRedirectPolicy`, tested on the JVM. It also prevents a race: when the user signs out,
Firebase's auth-state listener reports "signed out" as well; the policy records that a sign-out
is in progress, so only one `MainActivity` is opened, with reason `SignedOut`, never a second
one with `SessionExpired`.

The legacy home also has a back-stack bug: `showTasksFragment()` adds the first tab with
`addToBackStack(null)`, so the first back press shows an empty container instead of closing the
app (FR-021). The bridge task removes that call for the initial tab. No layout file changes.
Spec 005 deletes the bridge with `HomeActivity`.

**Alternatives**: wrapping the legacy home in `AndroidView` inside Compose (Fragments and Data
Binding inside Compose, high risk for one spec); building a Compose home now (that is spec 005).

## 6. Revalidating the session

**Decision**: `RefreshSessionUseCase` calls `FirebaseUser.reload()` (via the repository) when
home is shown. Mapped results:

| Outcome | Action |
|---|---|
| Success | Update the cached profile (name from cache or Firestore, `emailConfirmed`) |
| `ERROR_USER_DISABLED` | `AccountDisabled`: sign out, show sign-in with the account-disabled message |
| `ERROR_USER_NOT_FOUND`, `ERROR_USER_TOKEN_EXPIRED`, `ERROR_INVALID_USER_TOKEN` | `SessionExpired`: sign out, show sign-in with "please sign in again" |
| Network error | Not shown; the user stays on home (FR-002). Logged by case name |
| Anything else | Not shown; logged by case name, without data |

Firebase also signs the user out by itself in these cases; the repository exposes an
`observeSignedIn(): Flow<Boolean>` built from `AuthStateListener` so the bridge reacts either way.

## 7. Error classification

**Decision**: `FirebaseErrorMapper` in `:core:data` maps exceptions to `AuthFailure` primarily by
`FirebaseAuthException.errorCode`, then by type:

| Firebase | `AuthFailure` |
|---|---|
| `FirebaseNetworkException` | `NoConnection` |
| `ERROR_INVALID_CREDENTIAL`, `ERROR_WRONG_PASSWORD`, `ERROR_USER_NOT_FOUND` (sign-in) | `InvalidCredentials` |
| `ERROR_INVALID_EMAIL` | `InvalidEmail` |
| `ERROR_EMAIL_ALREADY_IN_USE` (`FirebaseAuthUserCollisionException`) | `EmailAlreadyInUse` |
| `ERROR_WEAK_PASSWORD` (`FirebaseAuthWeakPasswordException`) | `WeakPassword` |
| `FirebaseTooManyRequestsException`, `ERROR_TOO_MANY_REQUESTS` | `TooManyAttempts` |
| `ERROR_USER_DISABLED` | `AccountDisabled` |
| `ERROR_USER_TOKEN_EXPIRED`, `ERROR_INVALID_USER_TOKEN` | `SessionExpired` |
| anything else | `Unknown` |

For a password reset, `ERROR_USER_NOT_FOUND` is mapped to success (FR-013). `CancellationException`
is rethrown, never mapped (constitution III). The mapper's input is an error code plus a coarse
kind, so it is tested on the JVM without constructing SDK types.

## 8. Sign-up sequence and partial failure

**Decision**: `SignUpUseCase`:

1. Normalize the email (FR-020) and validate the form (`SignUpForm.validate()`); stop with
   field errors if any (FR-007).
2. `createAccount(email, password)` → on failure, return the mapped `AuthFailure`.
3. `saveProfile(name)`: writes `users/{uid}` (`uid`, `uName`, `uEmail`, same shape as v1) and
   the cached profile. On failure, set `profileSyncPending` and continue (edge case: the account
   exists, the name is saved later). `RefreshSessionUseCase` retries a pending sync.
4. `sendEmailConfirmation()`: on success set `confirmationNoticePending`; on failure continue
   without the notice (FR-008).
5. Return success; the screen emits `Authenticated`.

Steps 3 and 4 log their failure by case name through Timber (constitution VIII), even though
the user is not told.

**Cancel** (constitution VI: operations over 1 s must be cancellable): while step 2 runs, Cancel
stops the coroutine and keeps the form. Firebase cannot abort a request already sent, so if the
account turns out to have been created, it is signed out locally and the user can sign in with
it. Once step 2 has succeeded, Cancel is hidden and steps 3 to 5 finish, so the user is never
left half signed up. Sign-in and reset cancel the same way; a sign-in that completes after
Cancel is signed out locally.

If the process dies between 2 and 3, Firebase already holds the session, so the next launch goes
home and the pending sync covers the profile (edge case "killed while creating").

## 9. Cooldowns (FR-014, and spec 005's resend)

**Decision**: `ActionCooldown` in domain, a `@Singleton` holding the last time per
`CooldownAction` (`PasswordReset`, `EmailConfirmation`), measured with an injected
`kotlin.time.TimeSource.WithComparableMarks` (monotonic). It answers `remaining(action)`. The
reset ViewModel shows a countdown that ticks with `delay(1.seconds)`, which runs on virtual time
in tests. It lives for the process; Firebase's own throttling covers the rest.

**Alternatives**: persisting the timestamp (wall-clock changes break it; not needed for 60 s);
per-ViewModel state (leaving and reopening the screen would reset the wait).

## 10. Legacy cleanup on upgrade (FR-022, Story 1 scenario 4)

**Decision**: `LegacyAuthCleanup` runs once, inside `ResolveStartDestinationUseCase`'s first
call, before anything else reads storage: `context.deleteSharedPreferences("alpha_ai_secure_prefs")`
and delete the Keystore alias `_androidx_security_master_key_` if present. A flag in
`AppPreferencesStore` (`legacyCleanupDone`) makes it run once. The Firebase session is not
touched, so a v1 user who was signed in goes straight home.

**Rationale**: deleting the file erases the stored password without needing to decrypt it, so
`security-crypto` can be removed in the same change.

## 11. Backup (FR-023, SC-005)

**Decision**: `android:allowBackup="false"`; replace the sample `data_extraction_rules.xml` with
rules that exclude every domain from both `cloud-backup` and `device-transfer` (Android 12+
ignores `allowBackup` for device-to-device transfer); delete `backup_rules.xml` and the
`fullBackupContent` attribute.

**Rationale**: nothing the app stores locally after this spec needs restoring (accounts and
history live in Firebase; the introduction flag is cheap to lose). Excluding everything is
simpler to verify than a per-file list and covers Firebase's token store.

**Alternatives**: keep backup on and exclude named files (easy to miss a file, such as the
Firebase store whose name is an SDK detail).

## 12. Feature module and navigation

**Decision**:

- New convention plugin `alpha.android.feature` = `alpha.android.library` + `alpha.android.compose`
  + `alpha.hilt` + the Kotlin serialization plugin, and it adds `:core:designsystem`, `:core:ui`,
  `:core:domain`, Navigation Compose, lifecycle-runtime-compose and
  `hilt-lifecycle-viewmodel-compose`. `:feature:auth` applies it.
- Routes (`@Serializable`): `IntroductionRoute`, `SignInRoute(reason: SignInReason? = null)`,
  `SignUpRoute`, `PasswordResetRoute(email: String = "")`. All arguments are primitives or
  enums (constitution V).
- `fun NavGraphBuilder.authGraph(navController, onAuthenticated: () -> Unit)` registers the four
  screens. Back rules (FR-021): the introduction is a single destination with an internal pager
  (back goes to the previous page; on page 1 the activity finishes); finishing or skipping it
  navigates to sign-in with `popUpTo<IntroductionRoute> { inclusive = true }`, so back on
  sign-in finishes the activity; sign-up and reset are pushed on top of sign-in.
- The debug gallery alias keeps working: `MainActivity` checks whether it was launched through
  the alias component and, if so, skips start resolution and starts at the gallery.

## 13. MVI and saved state

**Decision**: every screen has `XxxUiState` (immutable data class), `XxxIntent` (sealed),
`XxxEffect` (sealed); one `StateFlow` and one `Channel`-backed effect `Flow`; `onIntent`. Form
text survives rotation and theme or language changes because the ViewModel outlives them
(FR-019). Name and email are also kept in `SavedStateHandle` for process death; **password
fields are never put in `SavedStateHandle`**, because it is written to disk.

## 14. Messages and strings

**Decision**: `AuthFailure` → `@StringRes` mapping (`AuthFailureMessages`) lives in `:feature:auth`.
Field errors (`FieldError.Empty`, `InvalidFormat`, `TooShort`, `Mismatch`) map to their own
strings. Every string is in `values` and `values-ar`; `StringsParityTest` already scans every
module. The legacy-bridge strings (sign-out dialog, confirmation notice, session messages) are in
`:app`. About 70 new strings per language.

## 15. Testing

| Layer | Test | Tools |
|---|---|---|
| Domain | form validation, every use case success + failure, start resolution matrix, cooldown | JUnit, coroutines-test, `TestTimeSource` |
| Data | `FirebaseErrorMapper` table, `FirebaseAuthRepository` with a fake `FirebaseAuthDataSource` and fake stores, `DataStoreAppPreferencesRepository` with a fake store, `LegacyAuthCleanup` call order | JUnit, coroutines-test, Turbine |
| Datastore | session serializer round trip and decrypt-failure fallback with a fake `SessionCipher`; `AppPreferencesStore` defaults and round trip on a temporary file; the real Keystore cipher in one instrumented test | JUnit; AndroidJUnit4 on device |
| Shell | `StartViewModel`; `LegacyHomeRedirectPolicy` (reasons, sign-out race) | JUnit, Turbine |
| Presentation | every ViewModel: state and effect sequences per intent sequence, including double-submit and cooldown ticking | Turbine, `StandardTestDispatcher` |
| UI | sign-in screen: field error, loading disables submit, error message shown (Compose UI test, constitution IX SHOULD) | `ui-test-junit4` on device/emulator |

Shared fakes (`FakeAuthRepository`, `FakeAppPreferencesRepository`) live in `:core:testing`.

## 16. New dependencies

| Library | Why | Replaces / overlap |
|---|---|---|
| `com.google.firebase:firebase-auth` (BOM) | Already on the classpath transitively; now declared directly in `:core:data` | Replaces `firebase-ui-auth` (unused) |
| `firebase-firestore` (BOM) in `:core:data` | Profile document; already used by `:app` | — |
| `androidx.datastore:datastore` and `datastore-preferences` | Encrypted session store and plain app preferences | Replaces `security-crypto` (deprecated) |
| `androidx.core:core-splashscreen` | Hold the first frame until the start destination is known on API 26–36 | Replaces `SplashActivity` |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` | `hiltViewModel()` in feature screens | — |
| `androidx.compose.ui:ui-test-junit4`, `ui-test-manifest` (BOM) | The sign-in Compose UI test | — |
| `androidx.test:runner` | Instrumented test runner for the Keystore cipher test and the Compose UI test | `androidx.test.ext:junit` is already declared; the runner was not |
| `kotlinx-coroutines-play-services` | `Task.await()` for Firebase calls in `:core:data` (already on `:app`'s classpath transitively) | — |

Versions: the latest stable release of each at implementation time, added to
`libs.versions.toml` (constitution II). None duplicates an existing capability.

## 17. Scope check

Estimated at about 40 tasks: setup (4), domain (6), datastore (4), data (5), feature screens
(4 × 2), shell and legacy bridge (5), deletion (4), strings and tests (5). If `/speckit-tasks`
goes well beyond 40, split the legacy deletion and backup changes into a follow-up spec as the
roadmap allows.
