# AI models

The one place to learn which model serves each AI capability and why. Update it with a dated
note whenever a model, its order, or a provider setting changes.

The models themselves are set in two files and nowhere else:

- `core/data/src/main/kotlin/com/example/alpha_ai/core/data/ai/config/OpenRouterModels.kt`
- `core/data/src/main/kotlin/com/example/alpha_ai/core/data/ai/config/HuggingFaceModels.kt`

## Status

| Item | State |
|---|---|
| Key restriction on the provider side | **Not done yet** (see below) |
| Model comparison on Arabic and English samples | **Not run yet**: it needs the restricted key |
| Data-use terms of each chosen endpoint | **Not checked yet** |
| Chains in the configuration files | Provisional, in the order planned before any measurement |

Until the first three rows are done, the feature must not be merged and no live request may
be made.

## Key restriction

Principle VII allows a key to ship in the app only when it is restricted on the provider
side, so that an extracted key can do nothing except use up the free daily allowance.

To be done by the project owner, then recorded here with the date:

| Setting | Applied | Date |
|---|---|---|
| OpenRouter account has no payment method and no credits | no | |
| The key is used only by this app and has a credit limit of 0 | no | |
| Privacy settings: free endpoints that publish prompts are turned off | no | |
| Privacy settings: endpoints that train on prompts are turned off, if the chosen models stay available | no | |
| A guardrail limits the key to the free models listed below | no | |
| The key is in `local.properties` as `OPENROUTER_API_KEY` and nowhere else | no | |

## Chains (provisional)

A capability's chain is tried in order. The first two choices are served by different
upstream hosts so one outage does not take a capability down. `openrouter/free` is
OpenRouter's router to any free model that is available; it covers a named model being
withdrawn.

| Capability | 1st | 2nd | 3rd |
|---|---|---|---|
| Grammar correction, translation, summarization | `google/gemma-4-31b-it:free` (Google) | `nvidia/nemotron-3-ultra-550b-a55b:free` (Nvidia) | `openrouter/free` |
| Image text extraction | `google/gemma-4-31b-it:free` (Google) | `nvidia/nemotron-3.5-super-vl:free` (Nvidia) | `openrouter/free` |
| Speech recognition | `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free` (Nvidia) | none | |
| Language detection | on the device, by script analysis; no hosted model | | |

### Known exceptions

- **Speech recognition has no working fallback.** Only one free model accepts audio. The
  Hugging Face entry `openai/whisper-large-v3-turbo` is built and tested but switched off,
  because Hugging Face no longer serves inference on a free account. If the one model fails,
  the user gets a classified failure.
- **Hugging Face is switched off.** Its client is built and tested. To switch it on, add
  credits to the account behind `HF_API_TOKEN`, use a fine-grained token with only the
  "make calls to Inference Providers" permission, set `enabled = true` in
  `HuggingFaceModels.kt`, and record the change here.
- **Limits.** The free allowance is 20 requests a minute and 50 a day for the whole app key.

## Comparison

Not run yet. Method (specs/002-ai-provider-layer/research.md, section 3):

- Samples: 3 Arabic and 3 English per capability, in
  `core/data/src/test/resources/ai-samples/`. One per language contains instruction-like
  text. The image and audio samples still have to be added (see the README there).
- Scoring, per model and language: correct result (0 to 2 against the expected output),
  bare output with no commentary (yes or no), right language (yes or no), time to answer.
- Run, one capability at a time (about 70 requests in total, so two days of allowance):

  ```bash
  ./gradlew :core:data:testDebugUnitTest -PliveAi -Pcapability=grammar -Pcompare
  ```

  Copy each table from `core/data/build/reports/ai-live/` into this section, then set the
  chains to the measured order.
- For each model that would be chosen: read its endpoint's data policy on its OpenRouter
  page and record it here with the date. A model that trains on input is not chosen when
  another of comparable score exists. Also confirm its replies carry no reasoning text.

## Change log

| Date | Change | Reason |
|---|---|---|
| 2026-11-17 | Provisional chains set; Hugging Face entries switched off | Hugging Face returns "no credits" on a free account; OpenRouter free models chosen before measurement |
