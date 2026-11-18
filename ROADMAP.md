# Alpha-AI Production Roadmap

A spec-driven plan for turning Alpha-AI from a graduation project into a production-ready
Android app. Each numbered spec below is one pass through the Spec Kit workflow and one merge
into `development`.

The rules every spec must follow are in `.specify/memory/constitution.md` (version 2.3.0).

---

## 1. Where the project is today

Analysis of the `development` branch on 2026-11-16.

### Size and shape

| Item | Current state |
|---|---|
| Modules | One (`:app`) |
| Kotlin code | About 3,800 lines in 45 files |
| UI | 30 XML layouts, Data Binding, 12 Activities, 3 Fragments |
| Presentation pattern | Mixed: auth uses `StateFlow` + `LiveData`; tool screens use `ObservableField` |
| Dependency injection | Hilt for auth only; `ApiManager` is a manual singleton with hand-written ViewModel factories |
| Tests | None beyond the generated `ExampleUnitTest` and `ExampleInstrumentedTest` |
| Toolchain | Gradle 8.4, AGP 8.2.2 / 8.3.0 (declared twice, mismatched), Kotlin 1.9.22, kapt, Java 8 target, SDK 34 |
| CI | None |

### What works and what does not

| Area | State |
|---|---|
| Splash, onboarding, login, register | Working, refactored in commit `3f7434b` |
| Password reset (OTP, new password) | Removed in `3f7434b`, not replaced |
| Grammar correction (GEC) | Wired to the API: language detection, then an LLM call |
| Translation, summarization | Implemented in `ApiManager`, used by the "Words" screen |
| OCR, speech-to-text, document, ER, WR screens | UI exists but the ViewModels make no API calls |
| History | `HistoryRepository` exists but is unused; the screen shows hardcoded sample data |
| Arabic UI | `values-ar` exists in every module with strings (spec 003); English and Arabic parity is checked by `StringsParityTest` |

### Problems that block production

| # | Problem | Where |
|---|---|---|
| 1 | **Hugging Face tokens are hardcoded and pushed to GitHub.** 16 distinct tokens appear in the working tree, and both files are on `origin/master`. | `core/common/Constants.kt`, `api_keys.properties` |
| 2 | HTTP bodies are logged at `BODY` level in every build, including the auth header | `ApiManager.kt` |
| 3 | `android.util.Log` calls print user IDs and errors | `LoginViewModel.kt`, `HomeActivity.kt`, `BaseActivity.kt` and others |
| 4 | Release build has minification off and no signing config | `app/build.gradle.kts` |
| 5 | `applicationId` is `com.example.alpha_ai`, which Google Play rejects | `app/build.gradle.kts` |
| 6 | `allowBackup="true"` with empty backup rules | `AndroidManifest.xml`, `res/xml/` |
| 7 | Global mutable user holder | `core/common/UserProvider.kt` |
| 8 | Prompts concatenate user text into the instruction; responses are parsed by hand with `JSONObject` | `ApiManager.kt` |
| 9 | Error messages are hardcoded English strings | `NetworkResponseHandler.kt` |
| 10 | No fallback when a model is rate-limited, loading, or withdrawn | `ApiManager.kt` |

---

## 2. Phase 0: do this before any spec (manual)

These steps are not spec work. Do them first, because problem 1 is live now.

- [x] **Revoke every Hugging Face token** at <https://huggingface.co/settings/tokens>. Removing
      them from the code is not enough: they stay in git history on GitHub.
- [x] Create one new token and add it to `local.properties` (already git-ignored) as
      `HF_API_TOKEN=hf_...`. Then delete the old `api_keys.properties` file from disk.
- [x] Remove the tokens from `Constants.kt`, run `git rm --cached api_keys.properties`, and add
      `api_keys.properties` to `.gitignore`. The build now reads a single `HF_API_TOKEN` from
      `local.properties`.
- [x] Decide whether to rewrite git history to purge the old tokens. Once they are revoked this
      is optional. **Decided: no rewrite.** All 12 old tokens show as Invalidated on Hugging Face.
- [x] Commit the Spec Kit setup (`.specify/`, `.claude/skills/`, this file). Delete the Sync
      Impact Report comment at the top of the constitution first.
- [x] Push `development` to GitHub so the work is backed up.

---

## 3. The loop for every spec

Run these commands in order for each spec in section 4. Do not start the next spec until the
current one is merged.

| Step | Command | What you do |
|---|---|---|
| 1 | `/speckit-specify <prompt>` | Paste the **Specify prompt** from the spec below. Describes what and why, no technology. |
| 2 | `/speckit-clarify` | Answer the questions. The **Decide** items in each spec are the ones to settle here. |
| 3 | `/speckit-plan <prompt>` | Paste the **Plan prompt**. This is where technology choices go. Check that the Constitution Check passes. |
| 4 | `/speckit-checklist` | Optional. Use for security, accessibility, or UX-heavy specs. |
| 5 | `/speckit-tasks` | Generates `tasks.md`. |
| 6 | `/speckit-analyze` | Fix every inconsistency it reports between spec, plan, and tasks before writing code. |
| 7 | `/speckit-implement` | Executes the tasks. |
| 8 | `/speckit-converge` | Compares the code with the spec and appends any missing work as new tasks. Repeat step 7 until it reports nothing left. |
| 9 | Verify and merge | Build, run unit tests and lint, run the app by hand through the spec's **Done when** list, then merge into `development`. |

Rules that keep the loop safe:

- `development` is the main branch; `master` is frozen as the v1.0.0 archive until every spec
  here is done, then receives one merge tagged `v2.0.0`.
- One spec, one branch, one merge. The app must build and its existing flows must work after
  every merge.
- When a spec migrates a screen, it deletes that screen's XML layout, Activity, and old
  ViewModel in the same change.
- If a spec turns out too large during `/speckit-tasks` (more than about 40 tasks), split it
  and add the new spec to this file.

---

## 4. Phases and specs

### Phase 1: Foundation

Nothing user-visible changes in this phase. It builds what every later spec depends on.

#### 001 — Build foundation and module structure

- **Specify prompt:** Restructure the project so that features can be built and tested
  independently. Developers need an up-to-date build, a clear module layout with enforced
  dependency rules, secrets kept out of source control, and an automated check that runs the
  build, tests, and lint on every push. The existing app must keep working unchanged.
- **Plan prompt:** Upgrade Gradle, AGP, and Kotlin to current stable versions with Java 17 and
  KSP replacing kapt. Move every dependency into `libs.versions.toml`. Add convention plugins.
  Create empty `:core:common`, `:core:domain`, `:core:data`, `:core:network`,
  `:core:datastore`, `:core:designsystem`, `:core:ui`, `:core:testing` modules. Enable Compose.
  Read secrets from `local.properties`. Add a GitHub Actions workflow.
- **Done when:** the app builds and runs as before; CI is green; no secret is in the repo.

#### 002 — AI provider layer

- **Specify prompt:** Give the app one reliable way to call AI models for text correction,
  translation, summarization, speech recognition, and text extraction. It must use free models
  from OpenRouter and Hugging Face, switch to a fallback when a model is busy, rate-limited, or
  gone, report failures in a form the app can explain to the user in Arabic or English, and
  let models be changed without touching feature code.
- **Plan prompt:** Implement in `:core:network`, `:core:domain`, and `:core:data` following
  Principles VIII and X. Retrofit services for OpenRouter chat completions and Hugging Face
  Inference, typed request and response models, a classified error type, an auth interceptor,
  a model configuration source, and a fallback chain. Unit tests with fakes and a mock web
  server.
- **Decide:** whether keys ship in the app (restricted on the provider side) or sit behind a
  small backend proxy. A proxy is safer but needs hosting. Also decide whether model
  configuration is remote (for example Firebase Remote Config) or bundled.
- **Done when:** each capability interface has a tested implementation and a tested fallback;
  the old `ApiManager` still works for screens not yet migrated.
- **Outcome (2026-11-17):** keys ship in the app, restricted on the provider side; model
  configuration is bundled, with a hook for a remote source later. Hugging Face is switched
  off because it is no longer free, so the app runs on OpenRouter's free allowance of 50
  requests a day. The old grammar screen still calls Hugging Face through `ApiManager` and
  cannot get a live result until spec 006 moves it to the new layer.

#### 003 — Design system and app shell

- **Specify prompt:** Define the look and feel of Alpha-AI and the frame every screen lives in:
  colors, typography, spacing, shapes, motion, and the shared components (buttons, text
  fields, cards, top bar, loading, empty, and error states). Support light and dark themes,
  Arabic and English with correct right-to-left layout, and large font sizes. Provide one
  navigation frame that later screens plug into.
- **Plan prompt:** Build `:core:designsystem` and `:core:ui` in Compose with Material 3, motion
  tokens, and previews for every component. Add a single `MainActivity` with a Navigation
  Compose host using type-safe routes. Add `values-ar` with translations for all existing
  strings. Legacy Activities stay reachable until their specs migrate them.
- **Decide:** brand colors and typeface; whether the app language follows the system or is
  selectable in the app.
- **Done when:** a component gallery screen renders every component in light, dark, Arabic, and
  English; the app still launches into the existing flow.

### Phase 2: Core user journey

#### 004 — Onboarding and authentication

- **Specify prompt:** A new user opens the app, sees a short introduction once, creates an
  account or signs in, and lands on the home screen. A returning user goes straight to home.
  Users can reset a forgotten password and sign out. Errors are explained clearly in the
  user's language, and the user never sees a screen they are immediately redirected away from.
- **Plan prompt:** New `:feature:auth` module in Compose with MVI. Use cases in domain,
  Firebase Auth behind a repository interface, session state in `:core:datastore` with
  Keystore-backed encryption. Start destination resolved before the first frame. Delete the
  XML splash, onboarding, login, and register screens and `UserProvider`.
- **Decide:** whether to offer Google sign-in; whether email verification is required.
- **Done when:** register, sign in, password reset, sign out, and auto-login all work; ViewModel
  and use case tests pass; legacy auth code is deleted.

#### 005 — Home and settings

- **Specify prompt:** The home screen shows every tool the app offers and gets the user into
  any of them in one tap, with their most recent work close at hand. A settings area lets the
  user see their profile, change language and theme, read the privacy policy, sign out, and
  delete their account.
- **Plan prompt:** `:feature:home` and `:feature:settings` in Compose with MVI. Replace
  `HomeActivity`, `TasksFragment`, and the drawer menu.
- **Carried from 004:** home shows a lasting "confirm your email" reminder with a resend option
  (at most once every 60 seconds) until the user's email is confirmed.
- **Done when:** every tool is reachable from home; settings changes persist across restarts.

#### 006 — Grammar correction (flagship)

- **Specify prompt:** A user types or pastes text in Arabic or English and gets a corrected
  version. The app detects the language, shows exactly what changed so the user can learn from
  it, lets the user accept the result, copy it, share it, or try again, and keeps working on
  long texts. The user can cancel a request in progress. Results are saved to history.
- **Plan prompt:** `:feature:gec` in Compose with MVI, using the grammar and language detection
  capabilities from spec 002. Word-level diff between input and output. Record the model
  comparison on Arabic and English samples required by Principle X.
- **Decide:** maximum input length; whether results stream in; whether to explain each
  correction or only highlight it.
- **Done when:** Arabic and English samples are corrected with highlighted changes; every
  failure type shows a localized message with retry; `GECActivity` is deleted.

#### 007 — History

- **Specify prompt:** Everything a signed-in user does with any tool is saved to their account.
  They can browse it newest first, filter by tool, search it, reopen an item to reuse it, and
  delete one item or all of them. Their history is private to them and available after
  reinstalling the app.
- **Plan prompt:** `:feature:history` in Compose with MVI over the existing Firestore
  `users/{uid}/histories` collection, exposed as a Flow. Add Firestore security rules so users
  can read and write only their own documents. Replace the hardcoded sample list.
- **Decide:** whether history works offline; whether there is a retention limit.
- **Done when:** GEC results appear in history; filter, search, and delete work; security rules
  are deployed and tested.

### Phase 3: AI tools

Each of these follows the pattern set by spec 006 and saves to history. Order them by what you
want to ship first.

#### 008 — Speech to text

- **Specify prompt:** A user records speech or picks an audio file and gets the text in Arabic
  or English, which they can edit, copy, share, or send straight to grammar correction. The
  microphone permission is requested only when needed, and the feature explains itself if the
  permission is denied.
- **Decide:** maximum recording length; hosted model or on-device recognition.

#### 009 — Image text extraction (OCR)

- **Specify prompt:** A user takes a photo or picks an image and gets the text in it, for
  printed Arabic and English. They can crop before extracting, then edit, copy, share, or send
  the text to grammar correction or translation.
- **Decide:** handwriting support; hosted model or on-device recognition.

#### 010 — Document text extraction

- **Specify prompt:** A user picks a PDF or Word document and gets its text, which they can
  correct, translate, or summarize. Large documents show progress and can be cancelled.
- **Decide:** supported formats and size limit; whether scanned PDFs go through OCR.

#### 011 — Translation

- **Specify prompt:** A user translates text between Arabic and English in either direction,
  with the source language detected automatically, and can swap direction, copy, share, and
  listen to the result.
- **Decide:** whether to support more languages now.

#### 012 — Summarization

- **Specify prompt:** A user gets a short summary of a long Arabic or English text and can
  choose how brief it is.
- **Decide:** length options; maximum input length.

#### 013 — Remaining legacy tools (ER, WR, Words, text to speech)

- **Specify prompt:** Review the ER, WR, and Words screens from the original app. For each one,
  either define it as a proper tool with a clear user purpose or remove it. Add listening to
  any result (text to speech) where it helps the user.
- **Decide:** keep, merge, or cut each screen. This spec starts with `/speckit-clarify` doing
  most of the work.

### Phase 4: Production readiness

#### 014 — Quality hardening

- **Specify prompt:** The app must feel fast and polished and be usable by everyone: smooth
  animations, quick startup, full screen reader support, readable contrast, correct behavior
  on small and large screens and after rotation or process death, and no leftover code from
  the old app.
- **Plan prompt:** Remove all remaining XML layouts, Data Binding, AppCompat screens, and
  unused dependencies (Apache POI if replaced, SwipeLayout, FirebaseUI). Add a baseline
  profile, Compose stability checks, an accessibility pass, and Compose UI tests for sign-in
  and grammar correction.
- **Done when:** no XML layouts remain; TalkBack can complete every flow; cold start and scroll
  performance are measured and recorded.

#### 015 — Release readiness

- **Specify prompt:** Prepare Alpha-AI for publication on Google Play: a real application
  identity, a signed and optimized release build, crash and performance reporting that
  contains no personal data, a privacy policy and data safety declaration that match what the
  app sends to AI providers, account deletion, and an automated release build.
- **Plan prompt:** Change `applicationId` (and re-register the app in Firebase), enable R8 and
  resource shrinking with tested keep rules, signing through CI secrets, Crashlytics, backup
  exclusion rules, network security config, versioning scheme, and a release workflow.
- **Decide:** final application ID and app name; free only or with a paid tier.
- **Done when:** a signed release bundle installs and passes every flow; the Play pre-launch
  report is clean; the checklist in section 5 is fully ticked.

---

## 5. Production-ready checklist

The project is production ready when all of these are true.

**Security**
- [x] No secret in the repository or its history that is still valid
- [ ] Release build is minified, not debuggable, and logs no HTTP bodies or user content
- [ ] Cleartext traffic disabled; sensitive data excluded from backup
- [ ] Firestore rules restrict each user to their own data
- [ ] AI provider keys restricted on the provider side or held behind a proxy

**Quality**
- [ ] Unit tests cover every use case, ViewModel, repository, and mapper
- [ ] CI runs build, tests, and lint on every push and is green
- [ ] No XML layouts, Data Binding, `LiveData`, or `ObservableField` remain
- [ ] Every screen has loading, empty, error, and success states

**Experience**
- [ ] Full Arabic and English UI with correct right-to-left layout
- [ ] Light and dark themes; font scaling up to 200%
- [ ] TalkBack can complete every flow
- [ ] Every AI request can be cancelled and has a working fallback (spec 002: done for text and image; speech recognition needs $0.50 of OpenRouter credit or an on-device recognizer, see `docs/ai-models.md`)

**Release**
- [ ] Real application ID, signed bundle, version scheme
- [ ] Crash reporting live with no personal data
- [ ] Privacy policy, data safety form, and account deletion in place
- [ ] Play pre-launch report clean

---

## 6. Progress

Update this table as each spec is merged.

| Spec | Name | Status | Merged |
|---|---|---|---|
| — | Phase 0 manual steps | Done | |
| 001 | Build foundation and module structure | Done | Merged into `development` on 2026-11-17; CI green. Device walk-through done on the emulator on 2026-11-17 |
| 002 | AI provider layer | Done | Merged into `development` on 2026-11-17 (PR #2); checked against live models. Before release the OpenRouter key needs its credit limit and guardrail (`docs/ai-models.md`, T027) |
| 003 | Design system and app shell | Done | Merged into `development` on 2026-11-18 (PR #5); CI green, checked on the emulator. Owner still to review the Arabic strings (T053) |
| 004 | Onboarding and authentication | Not started | |
| 005 | Home and settings | Not started | |
| 006 | Grammar correction | Not started | |
| 007 | History | Not started | |
| 008 | Speech to text | Not started | |
| 009 | Image text extraction | Not started | |
| 010 | Document text extraction | Not started | |
| 011 | Translation | Not started | |
| 012 | Summarization | Not started | |
| 013 | Remaining legacy tools | Not started | |
| 014 | Quality hardening | Not started | |
| 015 | Release readiness | Not started | |
