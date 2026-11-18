# Quickstart: Validate Onboarding and Authentication

How to prove spec 004 works. Behaviour details are in [contracts/](contracts/) and
[data-model.md](data-model.md).

## Prerequisites

- `local.properties` with the keys from spec 002, and `app/google-services.json` for the
  Alpha-AI Firebase project.
- Firebase console → Authentication:
  - Email/Password provider enabled.
  - **Settings → User actions → Email enumeration protection: on.**
  - Templates: password reset and email address verification enabled (default text is fine).
- A test account that already exists (made with v1 or the console), and a second email inbox
  you can read for sign-up.
- An emulator or device on API 26 or later; a second one on API 34 for the splash and backup
  checks.

## Automated checks

```powershell
./gradlew :core:domain:test :core:data:testDebugUnitTest :core:datastore:testDebugUnitTest `
          :feature:auth:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:lintDebug :feature:auth:lintDebug
./gradlew :feature:auth:connectedDebugAndroidTest :core:datastore:connectedDebugAndroidTest   # device required
```

Expected: all pass; lint reports no new issues; `StringsParityTest` passes with the new strings.

## Manual scenarios

Run each in English and Arabic, light and dark, and once at the largest font size (SC-007).

| # | Steps | Expected |
|---|---|---|
| 1 | Fresh install, open the app | Splash, then page 1 of the introduction. No other screen flashes (FR-001) |
| 2 | Swipe through pages, press back on page 2 | Returns to page 1; back on page 1 closes the app |
| 3 | Reopen, tap Skip | Sign-in. Back closes the app. Reopen: sign-in again, no introduction (FR-003) |
| 4 | Sign-up with empty fields, a bad email, a 7-character password, mismatched confirm | Each error under its field; nothing sent; text kept |
| 5 | Sign-up with the existing account's email | "Already has an account" with a Sign in action |
| 6 | Sign-up with a new email | Home. Snackbar "We sent a confirmation link to …" once; not on the next home visit. Email arrives |
| 7 | Rotate and switch dark mode while the sign-up form is half filled | Text and errors kept |
| 8 | Close and reopen the app 20 times (SC-001); then press back once on home | Splash, then home every time; sign-in never appears. One back press closes the app (no empty screen) |
| 9 | Airplane mode, reopen | Home (FR-002) |
| 10 | Home → sign-out icon → Cancel, then → Sign out | Cancel stays; Sign out shows sign-in once, with the "signed out" notice (not "session ended"). Back closes the app. Reopen: sign-in (Story 5) |
| 11 | Sign in with a wrong password, then with an unknown email | The same "email or password is incorrect" message both times |
| 12 | Airplane mode, sign in | "You're offline" message; text kept |
| 13 | Tap submit several times quickly | One request; button shows progress |
| 13a | With a throttled network (emulator "Network: GPRS"), submit sign-in, then tap Cancel; repeat on sign-up and reset | Progress stops, the form keeps its text, and the user is still signed out (constitution VI) |
| 14 | Forgot password with an email typed | Reset screen prefilled. Send → same confirmation for a known and an unknown email; "Send again in N s" for 60 s |
| 15 | Follow the reset link, set a new password, sign in | Works with the new password; the old one fails (SC-006) |
| 16 | Disable the test account in the console while signed in, bring the app to the front | Sign-in with "This account has been disabled" |
| 17 | Install the v1 build, sign in with "remember me", install this build over it | Home without signing in; `adb shell run-as com.example.alpha_ai ls shared_prefs` has no `alpha_ai_secure_prefs.xml` (FR-022) |
| 18 | Debug build: open "Alpha-AI Gallery" | The gallery, as in spec 003 |

## Start-up time (SC-002)

Signed in, on a mid-range device or an emulator with 4 cores and 4 GB of RAM:

```powershell
adb shell am force-stop com.example.alpha_ai
adb shell am start -W -n com.example.alpha_ai/.shell.MainActivity   # repeat 5 times
```

Expected: `TotalTime` is at most 2000 ms on every run, with and without a connection. `logcat`
`Displayed com.example.alpha_ai/.ui.main.home.HomeActivity` gives the time until home is drawn.

## Storage and backup checks (SC-005)

```powershell
adb shell run-as com.example.alpha_ai ls -R files/datastore shared_prefs
adb shell run-as com.example.alpha_ai cat files/datastore/session.enc | Select-String "@"   # no readable email
adb shell bmgr backupnow com.example.alpha_ai                                               # reports backup not allowed
```

After sign-out, `session.enc` decodes to the empty value (checked by the datastore test) and the
app holds no profile data. `logcat` during scenarios 6, 11, and 15 shows no email, password, or
token (FR-024).
