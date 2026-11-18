# Contract: App Entry, Start Routing, and the Legacy Home Bridge

Replaces the "Entry points" table of `specs/003-design-system-shell/contracts/app-shell.md`.

## Entry points

| Component | Exported | Launcher | Notes |
|---|---|---|---|
| `MainActivity` | yes | **yes** | Theme `Theme.AlphaAi.Starting` (splash), `postSplashScreenTheme` = `Theme.AlphaAi.Compose` |
| `activity-alias` "Alpha-AI Gallery" (debug only) | yes | yes (debug) | Unchanged; `MainActivity` detects the alias component and starts at the gallery |
| `HomeActivity` (legacy) | no | no | Until spec 005; gains `@AndroidEntryPoint` and `LegacyHomeSessionBridge` |
| `SplashActivity`, `OnBoardingActivity`, `LoginActivity`, `RegisterActivity` | — | — | **Deleted** |

## MainActivity start sequence

```text
onCreate
  installSplashScreen(); keepOnScreen = { startViewModel.state is Resolving }
  launched through the gallery alias?  → setContent { AlphaApp(start = GalleryRoute) }
  else collect StartViewModel.state:
     Resolved(Home)               → startActivity(HomeActivity, no animation); finish()   (no setContent)
     Resolved(Introduction)       → setContent { AlphaApp(start = IntroductionRoute) }
     Resolved(SignIn(reason))     → setContent { AlphaApp(start = SignInRoute(reason)) }
```

- `StartViewModel` (`:app`) calls `ResolveStartDestinationUseCase` once. After a 2 s timeout it
  decides from `AuthRepository.currentUser()` alone (synchronous, local): `Home` if present,
  otherwise `SignIn(null)`.
- Intent extra `start_reason` (String): accepted only if it equals a `SignInReason` name;
  otherwise ignored. When present and valid, resolution is skipped and the start is
  `SignIn(reason)`.

## AlphaApp

```kotlin
@Composable
fun AlphaApp(startRoute: Any, onAuthenticated: () -> Unit, navController: NavHostController = rememberNavController())
```

Inside the `NavHost`: `register(navController)` (debug gallery; the release `LegacyEntryRoute` is
deleted), then `authGraph(navController, onAuthenticated)`.

`onAuthenticated` (provided by `MainActivity`): start `HomeActivity`, then `finish()`.

## :feature:auth public API

```kotlin
@Serializable data object IntroductionRoute
@Serializable data class SignInRoute(val reason: SignInReason? = null)
@Serializable data object SignUpRoute
@Serializable data class PasswordResetRoute(val email: String = "")

fun NavGraphBuilder.authGraph(navController: NavHostController, onAuthenticated: () -> Unit)
```

Everything else in the module is `internal`.

| From | Effect | Navigation |
|---|---|---|
| Introduction | `ToSignIn` | `navigate(SignInRoute()) { popUpTo<IntroductionRoute> { inclusive = true } }` |
| Sign-in | `ToSignUp` | `navigate(SignUpRoute)` |
| Sign-in | `ToPasswordReset(email)` | `navigate(PasswordResetRoute(email))` |
| Sign-up | `ToSignIn` | `popBackStack()` |
| Reset | `ToSignIn` | `popBackStack()` |
| Sign-in, Sign-up | `Authenticated` | `onAuthenticated()` |

Sign-in is always the bottom of the auth back stack, so back on sign-in finishes the activity
(FR-021). Sign-up and reset are reachable only from sign-in, so back on them always returns to
sign-in.

## LegacyHomeSessionBridge (temporary, `:app`, deleted by spec 005)

```kotlin
class LegacyHomeSessionBridge @Inject constructor(
    signOut: SignOutUseCase,
    refreshSession: RefreshSessionUseCase,
    consumeConfirmationNotice: ConsumeConfirmationNoticeUseCase,
    authRepository: AuthRepository,      // observeSignedIn()
) {
    fun attach(activity: AppCompatActivity, signOutButton: View, snackbarAnchor: View)
}
```

| Trigger | Behaviour |
|---|---|
| `btn_logout` tapped | Confirmation dialog ("Sign out?" / "Sign out" / "Cancel"); confirm → `LegacyHomeRedirectPolicy.onSignOutConfirmed()` → `SignOutUseCase` → open `MainActivity` once with `NEW_TASK or CLEAR_TASK` and `start_reason = SignedOut` |
| First `onStart` after sign-up | `ConsumeConfirmationNoticeUseCase` returns an email → snackbar "We sent a confirmation link to <email>" (long duration) |
| Every `onStart` | `RefreshSessionUseCase`; `SessionExpired` / `AccountDisabled` → sign out and open `MainActivity` with that reason |
| `observeSignedIn()` emits `false` while home is open | Open `MainActivity` with `SessionExpired` |

All work runs in `activity.lifecycleScope` with `repeatOnLifecycle(STARTED)` for the observer.
No layout file changes; the strings are new resources in `:app` with Arabic translations.

### LegacyHomeRedirectPolicy (pure Kotlin, tested on the JVM)

The bridge asks this class whether and why to leave home; it holds no Android types.

| Input | Output |
|---|---|
| `onSignOutConfirmed()` | `Redirect(SignedOut)`, and records that a sign-out is in progress |
| `onRefreshResult(SessionExpired \| AccountDisabled)` | `Redirect(that reason)` unless a redirect was already issued |
| `onRefreshResult(anything else)` | `Stay` |
| `onSignedInChanged(false)` | `Redirect(SessionExpired)` unless a sign-out is in progress or a redirect was already issued; then `Stay` |

At most one `Redirect` is issued per home instance, so signing out never opens `MainActivity`
twice or shows "session ended" instead of "signed out".

**Lifetime**: `LegacyHomeSessionBridge` and `LegacyHomeRedirectPolicy` are unscoped (no
`@Singleton` or other scope annotation), so each `HomeActivity` gets new instances. A user who
signs out, signs in again, and reaches a new home can be redirected again.

### Back on the legacy home (FR-021)

`HomeActivity.showTasksFragment()` stops calling `addToBackStack(null)` for the tab selected at
start, so the first back press on home closes the app. Switching tabs keeps its current
behaviour.

## Manifest and backup

- `MainActivity`: `exported="true"`, launcher intent filter, `theme="@style/Theme.AlphaAi.Starting"`.
- `application`: `allowBackup="false"`, `dataExtractionRules` excludes all domains for
  `cloud-backup` and `device-transfer`; `fullBackupContent` removed.
