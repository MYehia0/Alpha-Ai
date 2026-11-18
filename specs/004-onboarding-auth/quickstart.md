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

## Results

### 2026-11-18, automated checks (T047, partial)

| Check | Result |
|---|---|
| Unit tests: `:core:domain` 37, `:core:data` 149 (5 skipped), `:core:datastore` 8, `:feature:auth` 33, `:app` 19 | Pass (rerun after the device fixes; connected tests also rerun and pass) |
| `:app:lintDebug` | Pass: 11 warnings, none new for spec 004 (version notices, `TrustAllX509TrustManager` from a dependency, `DataExtractionRules`); the 55 resources orphaned by the legacy deletion are removed |
| `:feature:auth:lintDebug` | Pass: no issues |
| `assembleDebug`, `:app:compileReleaseKotlin` | Pass |
| Connected tests on the `Medium_Phone` emulator (API 34): `:core:datastore` 3, `:feature:auth` 4 | Pass. The password-toggle test first failed because Compose's `InputText` property holds the raw value; it now checks `EditableText`, the text drawn on screen |
| Start-up time, **signed out** (`Medium_Phone`, API 34, 4 cores, 2 GB RAM, debug build) | First launch after `pm clear`: 2547 ms. Next five: 1976, 1989, 1738, 1719, 1764 ms. Opens on introduction page 1 |
| Start-up time, signed in (SC-002), same emulator, debug build | Online, 20 runs: 18 within 2000 ms (median about 1660 ms); outliers 2092 and 4829 ms. Offline, 6 runs: 2444, 4356, 1952, 1897, 2077, 2028 ms. One offline run hit the 2 s start fallback (local storage read slow under load) and still opened home. **Not met on this emulator** (2 GB RAM, half the 4 GB this guide asks for); to be re-measured with a release build on a mid-range phone |
| Storage, signed out | `files/datastore` holds only `app_preferences` (flags `introduction_completed`, `legacy_cleanup_done`) and Firebase's heartbeat file; no `session.enc`; no `alpha_ai_secure_prefs.xml` |
| Storage, signed in | `session.enc` is 128 bytes of ciphertext with no readable email; no file in the app's data names the email. After sign-out it is 30 bytes (the encrypted empty value) |
| Backup | `bmgr backupnow` → "Backup is not allowed"; package flags have no `ALLOW_BACKUP` |
| Logcat during the run | No email, password, or token |
| Manual scenarios | See below |

Note: run `:app:lintDebug` in its own Gradle invocation. Run together with
`:app:compileReleaseKotlin`, lint can read the release data-binding sources while they are being
regenerated and fail with "Unexpected failure during lint analysis".

### 2026-11-18, manual scenarios on the emulator (English, then Arabic)

| # | Result |
|---|---|
| 1 | Pass: splash, then page 1; no other screen |
| 2 | Pass: back on page 2 returns to page 1; back on page 1 closes the app |
| 3 | Pass: Skip opens sign-in; back closes; reopen opens sign-in, no introduction |
| 4 | Pass: each error under its field (empty; bad email; 7-character password; mismatched confirmation); text kept |
| 5 | Pass: "This email already has an account." with a Sign in action |
| 6 | **Not run**: needs a second inbox to receive the confirmation email |
| 7 | Pass: text and errors kept through rotation and a switch to dark mode |
| 8 | Pass: 20 reopens, home every time; one back press closes the app |
| 9 | Pass: home in airplane mode |
| 10 | Pass: Cancel stays on home; Sign out opens one sign-in screen in one task with "You've signed out."; back closes; reopen shows sign-in |
| 11 | Pass: the same "The email or password is incorrect." for a wrong password and an unknown email |
| 12 | Pass: "You're offline…" and the text kept |
| 13 | Pass after a fix (below): four quick taps sent one request |
| 13a | **Not verified on a device**: the emulator's network delay did not slow Firebase calls (about 300 ms), so a request could not be held open to tap Cancel. Covered by unit tests and the connected UI test |
| 14 | Pass: the same confirmation for an unknown email and the test account; "Send again in N s" for 60 s, kept when the screen is left and reopened |
| 15 | **Not run**: needs the reset link from the inbox |
| 16 | **Not run**: needs the account disabled in the Firebase console |
| 17 | **Not run**: needs the v1 build installed first |
| 18 | Pass: "Alpha-AI Gallery" opens the component gallery |
| SC-007 | Pass in Arabic (right to left), light and dark, at font scale 2.0: introduction, sign-in, sign-up with errors, password reset, sign-out dialog. The legacy home's "Tasks"/"History" labels are untranslated and its title clips at 2.0; that screen belongs to spec 005 |

Logcat: the app's own lines name only failure cases. The Firebase Auth SDK itself logs the email
at INFO when signing in ("Logging in as … with empty reCAPTCHA token"); that line is the SDK's,
not the app's, and cannot be turned off from the app.

### Bugs found and fixed during the device run

1. **Double tap could cancel a request.** Submitting cleared the old error message, the button
   moved up, and Cancel appeared where the finger was. The previous failure now stays until the
   new result arrives (sign-in, sign-up, reset), with a unit test.
2. **A missing profile was logged as a network failure.** `withTimeoutOrNull` returned `null` for
   both a timeout and a missing document. The profile timeout now uses `withTimeout`. Profile
   failures now log their kind and Firestore status code (enum names only).
3. **The show/hide password icon was invisible.** `AlphaIcons` used `path {}`, which has no fill;
   it now uses `materialPath {}`.
4. **The Arabic "Skip" had the wrong diacritic** (تخطٍّ); now تخطَّ.
5. **The password-toggle UI test matched the raw `InputText`**; it now checks the drawn text.

### Sign-off

The project owner accepted the items not run here (scenarios 6, 15, 16, 17, 13a on a device, and
SC-002 on a release build on a real phone) and closed T047 on 2026-11-18.
