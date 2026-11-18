# Data Model: Onboarding and Authentication

Types are Kotlin, but this file describes shape and rules only. Domain types live in
`:core:domain` (package `com.example.alpha_ai.core.domain.auth`) and are pure Kotlin.

## Domain models

### AuthUser

The signed-in account as the app sees it.

| Field | Type | Rules |
|---|---|---|
| `id` | `String` | Firebase uid; never shown, never logged |
| `name` | `String?` | Trimmed, not blank when present; `null` until known (old account offline, or profile sync pending) |
| `email` | `String` | Normalized (see `Email`) |
| `emailConfirmed` | `Boolean` | From the sign-in service; refreshed by `RefreshSessionUseCase` |

### Email (normalization)

`Email.normalize(raw) = raw.trim().lowercase(Locale.ROOT)` (FR-020). Valid when it matches
`local@domain.tld`: one `@`, non-empty local part, a domain with at least one dot, no spaces.
The service performs the final check (`InvalidEmail`).

### SignUpForm / SignInForm / PasswordResetForm

| Form | Fields | Validation (before anything is sent) |
|---|---|---|
| `SignUpForm` | `name`, `email`, `password`, `confirmPassword` | name not blank after trim → else `Empty`; email not blank → `Empty`, valid → `InvalidFormat`; password ≥ 8 chars → else `TooShort`; confirm equals password → else `Mismatch` |
| `SignInForm` | `email`, `password` | email not blank and valid; password not blank |
| `PasswordResetForm` | `email` | email not blank and valid |

`validate()` returns `Map<FormField, FieldError>`; empty means valid. `FormField` is
`Name | Email | Password | ConfirmPassword`. `FieldError` is `Empty | InvalidFormat | TooShort | Mismatch`.
Password length is counted in Unicode code points so Arabic characters count once each.

### AuthFailure

| Case | When | Message (FR-017) | Retry |
|---|---|---|---|
| `NoConnection` | No network | "You're offline. Check your connection and try again." | yes |
| `InvalidCredentials` | Wrong email or wrong password (never says which) | "The email or password is incorrect." | yes |
| `EmailAlreadyInUse` | Sign-up with a registered email | "This email already has an account." + action "Sign in" | no |
| `WeakPassword` | Service rejects the password | "Choose a stronger password." | no |
| `InvalidEmail` | Service rejects the email | "Enter a valid email address." | no |
| `TooManyAttempts` | Service throttles | "Too many attempts. Wait a few minutes, or reset your password." | later |
| `AccountDisabled` | Account disabled | "This account has been disabled." | no |
| `SessionExpired` | Session invalid on refresh | "Your session ended. Please sign in again." | — |
| `Unknown` | Anything else | "Something went wrong. Please try again." | yes |

`AuthResult<T>` is `Success(value)` or `Failure(failure: AuthFailure)`, the same shape as spec
002's `AiResult`. Cancellation is never a value.

### StartDestination

`Introduction | SignIn(reason: SignInReason?) | Home`. `SignInReason` is
`SessionExpired | AccountDisabled | SignedOut`.

Resolution (`ResolveStartDestinationUseCase`), in order:

```text
LegacyCleanup.runOnce()
currentUser != null           → mark introduction completed; Home
introductionCompleted         → SignIn(null)
otherwise                     → Introduction
```

If resolution takes longer than 2 s (storage misbehaving), `StartViewModel` decides from
`currentUser()` alone: `Home` if present, otherwise `SignIn(null)`.

### CooldownAction

`PasswordReset | EmailConfirmation`, each with a 60 s window (`ActionCooldown.WINDOW`).

## Repository interfaces (domain)

```kotlin
interface AuthRepository {
    fun currentUser(): AuthUser?                       // local, no network
    fun observeSignedIn(): Flow<Boolean>
    suspend fun signIn(email: String, password: String): AuthResult<AuthUser>
    suspend fun createAccount(email: String, password: String, name: String): AuthResult<AuthUser>
    suspend fun saveProfile(name: String): AuthResult<Unit>
    suspend fun sendEmailConfirmation(): AuthResult<Unit>
    suspend fun sendPasswordReset(email: String): AuthResult<Unit>   // unknown email = Success
    suspend fun refreshSession(): AuthResult<AuthUser>
    suspend fun signOut()                                             // local; never fails
    suspend fun consumeConfirmationNotice(): String?                  // email, once
}

interface AppPreferencesRepository {
    suspend fun isIntroductionCompleted(): Boolean     // a read failure returns false, never throws
    suspend fun setIntroductionCompleted()             // a write failure is logged, never throws
}

interface LegacyCleanup {                              // implemented by LegacyAuthCleanup in :core:data
    suspend fun runOnce()                              // erases v1's stored password; never throws
}
```

### Cancellation (`signIn`, `createAccount`)

Firebase cannot abort a request already sent, and when it completes it signs the user in on the
phone even if nobody is waiting. `FirebaseAuthRepository` (a `@Singleton`, so it outlives any
screen) handles this:

- The SDK call is awaited with `suspendCancellableCoroutine`. If the caller is cancelled while
  the task is still running, `invokeOnCancellation` registers a completion listener on the task.
- `signIn` completes after cancel → `signOut()` (session store cleared).
- `createAccount(email, password, name)` completes after cancel → write `users/{uid}` with the
  name (best effort, so a later sign-in has it), then `signOut()`.
- The listener runs in an application-lifetime `CoroutineScope` injected into the repository,
  not in the cancelled caller's scope.

Because the name is needed for this, `createAccount` takes the name as well. The resulting state
is always "signed out" after Cancel, whatever the timing (contracts/auth-screens.md).

## Use cases (domain, one `operator fun invoke` each)

| Use case | Does |
|---|---|
| `ResolveStartDestinationUseCase` | The resolution above |
| `CompleteIntroductionUseCase` | Marks the introduction completed |
| `SignInUseCase` | Normalize, validate `SignInForm`, `signIn`; retry pending profile sync |
| `SignUpUseCase` | Normalize the email, validate `SignUpForm`, `createAccount`, `saveProfile` (failure → pending), `sendEmailConfirmation` (failure ignored) — research §8 |
| `RequestPasswordResetUseCase` | Validate, check `ActionCooldown`, `sendPasswordReset`, start cooldown on success. Returns `Sent`, `CoolingDown(remaining)`, or a failure |
| `SignOutUseCase` | `signOut()`; clears the session store |
| `RefreshSessionUseCase` | `refreshSession`; network errors are ignored; retries pending profile sync |
| `ConsumeConfirmationNoticeUseCase` | Returns the email for the one-time message, then clears the flag |

## Local storage (`:core:datastore`)

### SessionStore (encrypted)

| Field | Type | Notes |
|---|---|---|
| `userId` | `String?` | Matches Firebase uid; a mismatch with the current user means the cache is discarded |
| `name` | `String?` | |
| `email` | `String?` | |
| `emailConfirmed` | `Boolean` | |
| `profileSyncPending` | `Boolean` | Name not yet written to `users/{uid}` |
| `confirmationNoticePending` | `Boolean` | One-time "link sent" message still to show |

- File: `files/datastore/session.enc`. Format: `iv (12 bytes) || AES-256-GCM(ciphertext + tag)`
  over a JSON object. Key alias `alpha_session_key` in `AndroidKeyStore`.
- `clear()` writes the empty value and is what sign-out calls (FR-016, SC-005).
- Decrypt or parse failure → empty value, then rewritten (research §3).

### AppPreferencesStore (plain Preferences DataStore, `app_preferences`)

| Key | Type | Cleared by sign-out |
|---|---|---|
| `introduction_completed` | `Boolean` | no |
| `legacy_cleanup_done` | `Boolean` | no |

## Remote data

### Firebase Auth account

Email and password; `isEmailVerified` gives `emailConfirmed`. Unchanged from v1.

### Firestore `users/{uid}`

| Field | Type | Notes |
|---|---|---|
| `uid` | string | Same as document id |
| `uName` | string | The user's name |
| `uEmail` | string | Normalized email |

Same shape v1 writes, so old and new accounts read the same way. Mapped to `AuthUser.name` in
the data layer; the DTO never leaves `:core:data`.

## State transitions

```text
            first launch                     finish / skip
 (none) ───────────────▶ Introduction ─────────────────────▶ SignIn
                                                     ┌──────────┘ ▲  ▲
                                         sign-in ok  │  sign-up ok│  │ sign-out,
                                                     ▼            │  │ session expired,
                                                    Home ─────────┘──┘ account disabled
 relaunch: signed in → Home; signed out → SignIn (introduction completed stays true)
```

Session lifecycle: `SignedOut → (signIn | createAccount) → SignedIn → (signOut | refresh says
invalid) → SignedOut`. Profile sub-state: `Synced | SyncPending`. Notice: `None | Pending → (shown) → None`.
