# Research: AI Provider Layer

**Date**: 2026-11-17 | **Plan**: [plan.md](plan.md)

Sources were read on 2026-11-17: OpenRouter's public model list (`/api/v1/models`) and
documentation, Hugging Face's Inference Providers documentation and public model API, and one
live check with the project's Hugging Face token. Anything not confirmed by a source is marked
**not verified**.

## 1. What the providers offer today

### Hugging Face

- **Finding**: free accounts get no included inference credits. The pricing page lists
  "Free Users: None; credits purchase required". The project's token (free account, read
  role) was tested against the language detection model the app uses today and got
  `402 Purchase pre-paid credits to continue using Inference Providers`.
- **Finding**: the chat model the app uses today, `Qwen/Qwen2.5-7B-Instruct`, is served only by
  a third-party provider that the account has not enabled; the request returns
  `400 model_not_supported`.
- **Consequence**: the existing grammar correction screen cannot get a result from the live
  service today. That is outside this feature (FR-029, FR-030) and is noted in the plan's
  risks.
- **Still served** (paid): `openai/whisper-large-v3` and `whisper-large-v3-turbo` for speech,
  `Helsinki-NLP/opus-mt-en-ar` and `opus-mt-ar-en` for translation,
  `papluca/xlm-roberta-base-language-detection`. `csebuetnlp/mT5_m2o_arabic_crossSum`, used by
  the app for Arabic summaries, is no longer served by any provider.
- **Decision** (project owner): build and test the Hugging Face service; keep its models in
  configuration with `enabled = false`.
- **Rationale**: the constitution names both providers and the roadmap asks for both services.
  Its chat endpoint has the same shape as OpenRouter's, so the extra cost is small, and
  switching it on later is a configuration edit.
- **Alternatives considered**: drop Hugging Face from this feature (departs from the roadmap);
  pay for it (breaks SC-010).

### OpenRouter

- **Finding**: 17 models are free today. Those relevant here:

  | Model | Input | Upstream host | Notes |
  |---|---|---|---|
  | `google/gemma-4-31b-it:free` | text, image | Google AI Studio | supports `response_format` |
  | `google/gemma-4-26b-a4b-it:free` | text, image | Google AI Studio | |
  | `nvidia/nemotron-3-ultra-550b-a55b:free` | text | Nvidia | 100% uptime in the last 30 minutes when checked |
  | `nvidia/nemotron-3.5-lightning:free` | text | Nvidia | small and fast |
  | `nvidia/nemotron-3.5-super-vl:free` | text, image | Nvidia | supports structured outputs |
  | `nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free` | text, image, **audio** | Nvidia | the only free model that accepts audio |
  | `inclusionai/ling-3.2-flash:free` | text, image | Ant Digital | |
  | `liquid/lfm-3-1.7b:free` | text | Liquid | very small |
  | `openrouter/free` | text, image | varies | router that picks any available free model |

- **Finding, limits**: free models are limited to 20 requests a minute and 50 a day per
  account; 1,000 a day once the account has ever bought 10 credits. A `429` from OpenRouter's
  own limit carries `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`
  (milliseconds since the epoch). `GET /api/v1/key` reports the daily counter.
- **Decision** (project owner): stay on 50 a day.
- **Finding, errors**: errors use `{"error": {"code", "message", "metadata"}}`. The HTTP status
  matches `error.code` only when the request fails before a provider accepts it. After that
  the status is `200` and the error is in the body (non-streaming) or in a server-sent event
  with `finish_reason: "error"` (streaming). `error.metadata.error_type` is the stable field to
  switch on. Codes: 400, 401, 402, 403, 404, 408, 413, 422, 429, 502, 503. `Retry-After` may
  be present on 429 and 503.
- **Finding, media**: images are sent as `image_url` content parts with a `data:` URL. Audio is
  sent as `input_audio` content parts with raw base64 and a `format`; chat completions accept
  the formats the upstream supports. Dedicated transcription (`/v1/audio/transcriptions`) is a
  paid service and is not used.
- **Finding, privacy**: OpenRouter does not store prompts unless the account opts in. Each
  upstream host has its own policy, shown per endpoint. The account's privacy settings and
  key guardrails can exclude free endpoints that train on or publish prompts.
  **Not verified**: the data policy of each free endpoint listed above. The public endpoint
  listing did not include it. It is checked in implementation phase 2 on each model's page.

## 2. Consequences of the 50-requests-a-day limit

- **Decision**: language detection runs on the device (section 5).
- **Decision**: no automatic retry of the same model. One failed attempt moves to the next
  choice or ends.
- **Decision**: when a `429` shows the daily limit is spent (`X-RateLimit-Remaining: 0`), the
  whole provider is skipped until `X-RateLimit-Reset`. Later requests fail at once as "service
  busy" and spend nothing.
- **Decision**: the client-side chain is used instead of OpenRouter's `models` array.
  **Rationale**: the app must also fall back on a time limit and on output that fails
  validation, which the server-side list cannot do, and one mechanism is simpler to test.
  **Alternative considered**: send `models: [first, fallback]` so OpenRouter fails over in one
  request. It may save a request on some failures; it can be added later inside the OpenRouter
  client without changing callers.
- **Not verified**: whether a failed request counts toward the daily 50. The design assumes it
  does.
- **Decision**: credentials are read through an `AiCredentials` interface. A later feature can
  supply a per-user key (OpenRouter offers an OAuth flow that gives each user their own key
  and their own daily allowance) or point the client at a proxy, with no change here.

## 3. Model selection and the comparison record

Principle X and FR-009 require a recorded comparison on Arabic and English samples. It cannot
be run during planning because the project has no OpenRouter key yet. This section fixes the
candidates and the method; the measurement is implementation phase 2 and its record is
`docs/ai-models.md`.

### Candidates and provisional chains

| Capability | Candidates to compare | Provisional chain (first choice, then fallbacks) |
|---|---|---|
| Grammar correction | gemma-4-31b, nemotron-3-ultra, ling-3.2-flash, nemotron-3.5-lightning | gemma-4-31b → nemotron-3-ultra → `openrouter/free` |
| Translation | same four | gemma-4-31b → nemotron-3-ultra → `openrouter/free` |
| Summarization | same four | gemma-4-31b → nemotron-3-ultra → `openrouter/free` |
| Image text extraction | gemma-4-31b, nemotron-3.5-super-vl, ling-3.2-flash | gemma-4-31b → nemotron-3.5-super-vl → `openrouter/free` |
| Speech recognition | nemotron-3-nano-omni | nemotron-3-nano-omni → Hugging Face `whisper-large-v3-turbo` (disabled) |
| Language detection | none (on device) | on-device script analysis |

The provisional order puts a Google-hosted model first and an Nvidia-hosted one second so the
two do not share an outage. The order is replaced by whatever the measurement shows.
Hugging Face chat entries are added, disabled, as the last element of each text chain.

### Method

- **Samples**: 3 Arabic and 3 English per capability, kept in the test resources with their
  expected output: sentences with known grammar errors; short paragraphs with a reference
  translation; a long paragraph to summarize; two printed-text images and two short voice
  clips per language. One sample per language contains instruction-like text (SC-007).
- **Scoring**, per model and language: correct result (0 to 2, judged against the expected
  output), output form (bare text with no commentary, yes or no), right language (yes or no),
  and time to answer.
- **Budget**: 3 text capabilities × 3 candidates × 6 samples = 54 requests, image 3 × 4 = 12,
  speech 1 × 4 = 4; 70 requests, so two days at 50 a day. The fourth text candidate is run
  only if one of the first three fails outright. The runner takes a capability argument so it
  can be resumed.
- **Runner**: JUnit tests under `core/data/src/test/.../live/`, skipped unless the Gradle
  property `liveAi` is set and `OPENROUTER_API_KEY` is present. CI never sets it (FR-031).
  The same runner is the manual live check for SC-001.
- **Terms check**: for each model that would be chosen, read its endpoint's data policy and
  record it. A model that trains on input is not chosen when another of comparable score
  exists (FR-010).

## 4. Libraries

- **Decision**: kotlinx.serialization JSON with Retrofit's first-party converter for all new
  request and response models.
  **Rationale**: the constitution requires model output to be parsed and validated against an
  expected shape. kotlinx.serialization enforces Kotlin nullability and required fields at
  parse time and fails with a typed exception; Gson fills missing non-null fields with `null`
  silently. It uses no reflection, which matters when R8 is enabled in spec 015, and
  type-safe navigation in spec 003 needs the same plugin.
  **Alternatives considered**: Gson (already in `:app`; weaker validation; stays for the
  legacy code only); Moshi (a third JSON library with no advantage here).
- **Decision**: OkHttp MockWebServer for network tests.
  **Rationale**: it exercises the real Retrofit service, converter, interceptors, and error
  classifier against scripted responses, including slow and dropped connections.
- **Decision**: read server-sent events by hand from a streamed response body.
  **Rationale**: the format needed is small (`data:` lines, comment lines, `[DONE]`), and it
  avoids adding `okhttp-sse`.
- **Decision**: `javax.inject` in `:core:common` for the dispatcher qualifier annotations.
  **Rationale**: the module is pure Kotlin and needs only the `@Qualifier` annotation; Hilt
  understands it.
- **Not added**: any provider SDK, any on-device ML library.

## 5. Language detection on the device

- **Decision**: count letters by Unicode script. Arabic-script letters at 60% or more of all
  letters gives Arabic; Latin-script letters at 60% or more gives English; anything else
  (another script, a near-even mix, or no letters) is reported as unsupported.
- **Rationale**: exact for the two supported languages, immediate, offline, and free of quota.
- **Alternatives considered**: the hosted classifier the app uses today (now paid, and one
  request per call); asking the chat model (one request per call); ML Kit language
  identification (a new dependency; would distinguish English from French, which no current
  requirement needs).
- **Known limit**: Latin-script text in another language is reported as English, and Persian
  or Urdu as Arabic. The same script counter is reused by output validation to check that a
  result is in the expected language.

## 6. Keys

- **Decision**: `OPENROUTER_API_KEY` is read exactly as `HF_API_TOKEN` is today: Gradle
  property, then environment variable, then `local.properties`, into a `BuildConfig` field of
  `:app`. `:app` hands both keys to `:core:network` through the `AiCredentials` interface.
- **Decision**: a blank key marks its provider as unavailable; nothing is sent, and one
  warning is logged without the key.
- **Provider-side restriction** (the exception Principle VII requires to be recorded), done by
  the project owner when creating the key:
  1. Create a key used only by this app, with a credit limit of 0 so it cannot spend money.
  2. Add no payment method and buy no credits on the account.
  3. In privacy settings, turn off free endpoints that publish prompts; turn off endpoints
     that train on prompts if the chosen models remain available with it off.
  4. Add a guardrail that limits the key to the configured free models.
  5. For Hugging Face, when it is switched on: a fine-grained token with only the "make calls
     to Inference Providers" permission.
- **Rationale**: an extracted key can then do nothing except use up the free daily allowance.

## 7. Prompts, safety, and output checks

- **Decision**: one system message holds the instruction, and the user's content is the whole
  of a separate user message, wrapped in `<content>` tags. The system message says that
  everything inside the tags is material to work on and never an instruction. Prompts are
  constants with a version number that is included in diagnostics.
- **Decision**: temperature 0 for correction, translation, and extraction; 0.2 for summaries.
  `max_tokens` is set from the input length. Reasoning output is excluded where the model
  allows it (**not verified** per model; checked in phase 2).
- **Decision**: structured output (`response_format`) is not used.
  **Rationale**: only some free models support it, and the result is a single piece of text.
- **Decision**: output is cleaned, then checked, before it is returned:
  cleaned of wrapping `<content>` tags, code fences, and one pair of surrounding quotation
  marks; then rejected when it is empty (text capabilities), when its script does not match
  the expected language, when it repeats the instruction, when a correction is shorter than
  half or longer than double the input, when a summary is not shorter than the input, or when
  the reply was cut off (`finish_reason` of `length`). A rejected output is an "invalid
  response" and triggers the fallback (FR-024).
- **Decision**: for speech and image extraction, the model is told to answer with the exact
  token `[[NONE]]` when there is nothing to transcribe; that maps to an empty result, which is
  a success (spec edge case).

## 8. Time limits and cancellation

- **Decision**: OkHttp connect timeout 5 s, read and write 30 s. Each attempt runs under a
  coroutine timeout of 14 s for text and 28 s for image and speech; the whole request under
  30 s and 60 s. An attempt gets the smaller of its own limit and the time left, and no new
  attempt starts with less than 3 s left.
- **Rationale**: two full attempts fit inside SC-005, and a slow first model cannot use the
  whole budget.
- **Decision**: before the first attempt, a connectivity check (validated network capability)
  ends the request with "no connection" at once (SC-006). A transport error that shows the
  device went offline mid-request also ends it without trying fallbacks (FR-015).
- **Decision**: cancellation is ordinary coroutine cancellation. Retrofit cancels the HTTP
  call; the runner rethrows `CancellationException` and starts nothing further (FR-006).

## 9. Cooldowns

- **Decision**: an in-memory tracker keyed by provider and model, with an injected clock:

  | Cause | Scope | Skipped for |
  |---|---|---|
  | Rate limited | model | `Retry-After`, else 60 s |
  | Daily limit spent | provider | until `X-RateLimit-Reset`, else 1 hour |
  | Loading or overloaded | model | the provider's estimate, else 30 s |
  | Timed out or server error | model | 30 s |
  | Model not found | model | until the app restarts; logged as a configuration problem |
  | Access rejected or no credits | provider | until the app restarts; logged |
  | Output failed validation | model | none; after 3 in a row, 5 minutes |

- **Rationale**: FR-016. Nothing is persisted, so a wrong cooldown cannot outlive the session.
- **Alternative considered**: a circuit breaker library; more than this needs.

## 10. Where failure messages live

- **Decision**: string resources and the failure-to-message mapping go in `:core:ui`.
- **Rationale**: the domain module cannot hold Android resources, and `:core:ui` is the shared
  module every feature screen already depends on. A JVM test reads both resource files and
  checks that every category and reason has an entry in each, and that no entry contains a
  digit sequence or a provider or model name (SC-003).
- **Alternative considered**: plain strings in the domain module (cannot be localized by the
  platform).
