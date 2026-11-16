# Quickstart: Validating the Build Foundation

**Date**: 2026-11-16 | **Plan**: [plan.md](plan.md) | **Contracts**: [contracts/build-contracts.md](contracts/build-contracts.md)

Run these checks after implementation. Each maps to a user story in [spec.md](spec.md).

## Prerequisites

- JDK 17. For terminal builds set `JAVA_HOME`, for example in PowerShell:
  `$env:JAVA_HOME = "$HOME\.jdks\jbr-17.0.14"`
- Android SDK, with `sdk.dir` in `local.properties` or `ANDROID_HOME` set.
- `local.properties` containing `HF_API_TOKEN=...` (copy `local.properties.example`).
- A device or emulator on API 26 or later.

## 1. The app builds and behaves as before (Story 1)

```powershell
./gradlew clean assembleDebug
./gradlew installDebug
```

**Expected**: both succeed. Then walk through each flow and compare with the app before the
change: onboarding, sign-up, sign-in, home, grammar correction, speech to text, image text
extraction, document text extraction, translation, summarization, history, settings.
Record any difference; there should be none (SC-002).

Check the catalog rule:

```powershell
# Expect no output: no version strings in module build files
Select-String -Path app/build.gradle.kts, core/*/build.gradle.kts -Pattern '"[\w.\-]+:[\w.\-]+:[\d]'
```

## 2. Secrets stay out of source control (Story 2)

```powershell
# Expect no output
git grep -nE "hf_[A-Za-z0-9]{30,}"
# Expect: both paths reported as ignored
git check-ignore -v local.properties api_keys.properties
```

Missing-secret behavior: temporarily rename `local.properties` (keep `sdk.dir` available via
`ANDROID_HOME`), run `./gradlew assembleDebug`, and confirm the build succeeds with one
warning that names `HF_API_TOKEN` and `local.properties`. Rename the file back.

## 3. Every push is checked (Story 3)

1. Push the feature branch. **Expected**: a `CI` run appears and passes within 15 minutes.
2. On a throwaway branch, push each of these in turn and confirm a failed run that names the
   failing step, with reports attached:
   - a compile error in any Kotlin file;
   - a unit test with a failing assertion;
   - a new lint error, for example a hardcoded string in a new layout;
   - a fake token matching the `hf_` format in a tracked file.
3. Open a pull request from the failing branch into `development`. **Expected**: the failed
   status is shown on the pull request.
4. Open the log of a passing run and confirm no secret value is printed.
5. Delete the throwaway branch.

## 4. Module layout and dependency rules (Story 4)

```powershell
./gradlew projects
```

**Expected**: `:app` and the eight `:core:*` modules are listed.

```powershell
./gradlew :core:domain:build
./gradlew :core:designsystem:assembleDebug
```

**Expected**: each succeeds without compiling `:app`.

Rule checks, each on a scratch edit that is then reverted:

| Edit | Expected failure |
|---|---|
| In `core/data/build.gradle.kts` add `implementation(project(":app"))` | DR-1 named in the error |
| In `core/domain/build.gradle.kts` add `implementation(project(":core:data"))` | DR-3 named in the error |
| In `core/domain`, add a Kotlin file that imports `android.content.Context` | compile error: unresolved reference |

The convention-plugin tests cover the same rules, including DR-2 for feature modules:

```powershell
./gradlew -p build-logic test
```

## 5. Final gate

```powershell
./gradlew assembleDebug testDebugUnitTest test lintDebug
```

**Expected**: success, with no new lint findings outside `app/lint-baseline.xml`.
