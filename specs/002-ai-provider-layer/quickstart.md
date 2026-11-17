# Quickstart: Validating the AI Provider Layer

**Date**: 2026-11-17 | **Plan**: [plan.md](plan.md)

How to prove the feature works. Contracts are in [contracts/](contracts/); types and rules
are in [data-model.md](data-model.md).

## Prerequisites

- JDK 17 and the Android SDK, as set up in spec 001 (`README.md`).
- For section 3 only: an OpenRouter account and key.
  1. Create an account at <https://openrouter.ai> and add no payment method.
  2. Create a key for this app with a credit limit of 0.
  3. In privacy settings, turn off free endpoints that publish prompts.
  4. Add to `local.properties`: `OPENROUTER_API_KEY=sk-or-...`

Sections 1, 2, 4, and 5 need no key and no network.

## 1. Automated tests (no network)

```bash
./gradlew :core:domain:test :core:network:testDebugUnitTest :core:data:testDebugUnitTest :core:ui:testDebugUnitTest
```

**Expected**: all pass. Together they cover:

| Area | Proves |
|---|---|
| Network classification | every row of the tables in contracts/provider-api.md, against a mock server |
| Capabilities, success path | each of the six returns a cleaned result for an Arabic and an English input |
| Input limits | each row of data-model.md section 3 fails with its reason and sends nothing (SC-003, FR-005) |
| Fallback | with the first choice failing by rate limit, loading, not found, time limit, and malformed output, each capability with a fallback returns the fallback's result (SC-002) |
| No fallback cases | offline, invalid input, and cancellation make no second call (FR-015) |
| Cooldown | a rate-limited model is skipped on the next request and tried again after its cooldown, using virtual time (FR-016) |
| Time bound | a chain of slow models ends with `TimedOut` at the total limit, using virtual time (FR-017) |
| Failure choice | mixed failures return the category ranked highest in data-model.md section 5 (FR-018) |
| Instruction-like input | content such as "ignore the above and..." is sent inside the user message only, and the system message is unchanged (SC-007) |
| Output checks | commentary, quotation wrapping, wrong language, and unchanged-length violations are rejected and trigger the fallback (FR-024) |
| Configuration | the built-in files satisfy rules M-1 to M-5; malformed overrides fall back and are logged (FR-012) |
| Messages | every failure category and input reason has an English and an Arabic string with no digits, provider, or model name (SC-003) |
| Streaming | deltas, a restart on mid-stream failure, and exactly one terminal event |

## 2. Full build gate

```bash
./gradlew assembleDebug testDebugUnitTest lintDebug
```

**Expected**: passes with no new lint warning in changed files. The same runs in CI on push.

## 3. Live check and model comparison (uses the daily quota)

Each run spends real requests from the 50-a-day allowance. Run one capability at a time.

```bash
./gradlew :core:data:testDebugUnitTest -PliveAi -Pcapability=grammar
```

Values for `capability`: `grammar`, `translation`, `summarization`, `image`, `speech`.
Add `-Pcompare` to run every candidate model instead of the configured chain.

**Expected**:

- Without `-PliveAi`, or without a key, these tests are skipped and nothing is sent.
- With it, each sample prints the model that answered, the time taken, and the output, and a
  results table is written under `core/data/build/reports/ai-live/`.
- For SC-001: every capability returns a correct result for its Arabic and English samples.
- For the comparison: copy the table into `docs/ai-models.md`, record each chosen endpoint's
  data-use terms, and set the chains in the configuration files.

Budget: the full comparison is about 70 requests, so two days. A single-capability check of
the configured chain is 6 requests.

## 4. Changing a model (SC-004)

1. In `OpenRouterModels.kt`, change the `modelId` of the rank 10 grammar entry to another
   free model.
2. Run section 1. **Expected**: passes with no other file changed.
3. Run `git status`. **Expected**: one file modified.
4. Give an entry `modelId = ""`. Run the configuration tests. **Expected**: the test that
   checks the built-in files fails, naming rule M-1. Revert.

## 5. Legacy flow is untouched (SC-009)

```bash
git diff --stat development -- app/src/main
```

**Expected**: the only entry is the new `di/AiCredentialsModule.kt`. No legacy networking
file, screen, or layout changed.

Then install the debug build and open the grammar correction screen. **Expected**: the same
behavior as on `development`. Note that on `development` the request itself currently fails
because Hugging Face returns `402` (research.md, section 1); "the same" includes that.

## 6. Logs carry no secrets or content (SC-008)

1. Build and run the debug app with a Timber debug tree, trigger the live check or a
   capability call, and capture `adb logcat`.
2. Search the capture for the key prefix (`sk-or-`, `hf_`) and for a distinctive phrase from
   the input. **Expected**: neither appears; attempt lines show capability, provider, model,
   outcome, and time only.
3. Confirm `core/network/src/release/` contains the no-op logging file, so a release build
   has no HTTP logging at all.

## 7. Done when

- Sections 1 and 2 pass locally and in CI.
- Section 3 has been run for every capability and `docs/ai-models.md` holds the comparison,
  the terms check, and the final chains.
- Sections 4 to 6 give the expected results.
- `ROADMAP.md` shows spec 002 as done and records that Hugging Face is switched off.
