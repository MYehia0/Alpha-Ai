# Quickstart: Verify the Design System and App Shell

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

## Prerequisites

- JDK 17 (`$HOME\.jdks\jbr-17.0.14`), Android SDK, an emulator (AVD `Medium_Phone`) or device.
- `local.properties` with the keys from `local.properties.example` (the gallery needs none).

## 1. Build gate

```powershell
$env:JAVA_HOME = "$HOME\.jdks\jbr-17.0.14"
.\gradlew.bat assembleDebug assembleRelease testDebugUnitTest lintDebug
```

Expected: success. The unit tests include:

| Test | Proves |
|---|---|
| `ContrastTest` | every listed colour pair passes WCAG AA in light and dark (SC-002) |
| `MotionTokensTest` | reduced motion turns every spec into `snap()` (FR-006) |
| `ButtonRulesTest` | a busy or disabled button accepts no click (FR-012) |
| `LoadingVisibilityTest` | hidden under 400 ms, shown at least 500 ms (FR-014) |
| `ScreenStateTest` | retry only for retryable failures; every `AiFailure` classified (FR-015) |
| `StringsParityTest` | `values` and `values-ar` hold the same string names in every module (SC-005) |

## 2. Release build has no gallery (FR-022)

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<version>\aapt2.exe" dump xmltree app\build\outputs\apk\release\app-release-unsigned.apk --file AndroidManifest.xml | Select-String "Gallery"
```

Expected: no match. The release manifest has one launcher, `SplashActivity`.

## 3. Existing flow unchanged (FR-021, SC-006)

Install the debug build, tap the normal **Alpha-AI** icon, and walk: splash → onboarding →
register / sign in → home → each tool screen → back to home → history. Everything looks and
behaves as on `development` before this spec. In English, every moved string reads exactly as
before.

## 4. Gallery in four combinations (SC-001, SC-004)

Open **Alpha-AI Gallery** (debug icon). Check each section (colours, type, spacing, shapes,
motion, buttons, fields, cards, top bar, states) in:

| Language | Theme |
|---|---|
| English | light, dark |
| Arabic (Settings → System → Languages → App languages → Alpha-AI → العربية) | light, dark |

Expected: Arabic runs right to left; the back arrow points right; the microphone icon does not
mirror; mixed "Alpha-AI" inside Arabic text reads correctly; all text is translated. Switching
theme or language while open keeps typed text in the field samples.

## 5. Large text (SC-003)

Settings → Display → Font size at maximum (and Display size at default) on a 360 dp wide
emulator. Expected: no cut or overlapping text in the gallery; the top bar title ends with "…";
long button labels wrap and the button grows.

## 6. Remove animations (FR-006)

Settings → Accessibility → Remove animations on. The motion section changes state instantly
with the same end state.

## 7. Accessibility (SC-008)

Run Google's Accessibility Scanner on each gallery section. Expected: no touch-target or
missing-label findings; contrast findings only on deliberately decorative samples labelled as
such.

## 8. Rotation and restore (FR-020)

In the gallery, open a section, type in a field, rotate, then send the app to the background
with "Don't keep activities" on and return. Expected: same section, same text.

## 9. Add a screen (SC-007)

Follow "Adding a screen" in [contracts/app-shell.md](contracts/app-shell.md) with a throwaway
route linked from the gallery. Expected: done in under 30 minutes, no other screen edited; the
change is then reverted.
