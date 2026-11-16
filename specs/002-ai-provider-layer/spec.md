# Feature Specification: AI Provider Layer

**Feature Branch**: `002-ai-provider-layer`

**Created**: 2026-11-17

**Status**: Draft

**Input**: User description: "Give the app one reliable way to call AI models for text correction, translation, summarization, speech recognition, and text extraction. It must use free models from OpenRouter and Hugging Face, switch to a fallback when a model is busy, rate-limited, or gone, report failures in a form the app can explain to the user in Arabic or English, and let models be changed without touching feature code."

## Background

Today the app reaches AI models through one class that mixes request building, response
parsing, and error handling, with model names fixed in code. Only grammar correction and
language detection are connected end to end. Translation and summarization have calls but no
fallback, and speech recognition and text extraction have screens but no working model behind
them. When a free model is busy or withdrawn, the feature simply fails, often with a raw
technical message.

This feature provides the shared layer that every later feature (grammar correction,
translation, summarization, speech to text, image and document text extraction) will call. It
delivers no new screen. Its "users" are the app's feature screens and, through them, the
person using the app.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A feature asks for an AI result and gets it (Priority: P1)

A feature screen needs an AI result: corrected text, a translation, a summary, the words in an
audio clip, or the text in an image. It asks for that capability by name, supplies the input
and the language, and receives either the result or a clear failure. It does not know or care
which provider or model did the work.

**Why this priority**: Nothing else in the product works without this. Every user-facing AI
feature on the roadmap depends on it.

**Independent Test**: For each of the five capabilities, submit a valid Arabic input and a
valid English input and confirm a usable result comes back in the expected language and form.

**Acceptance Scenarios**:

1. **Given** an English sentence with grammar mistakes, **When** correction is requested,
   **Then** the corrected sentence is returned in English, with no added commentary.
2. **Given** an Arabic sentence with grammar mistakes, **When** correction is requested,
   **Then** the corrected sentence is returned in Arabic, with no added commentary.
3. **Given** a text and a source and target language (Arabic to English or English to Arabic),
   **When** translation is requested, **Then** the translated text is returned in the target
   language.
4. **Given** a long Arabic or English text, **When** a summary is requested, **Then** a shorter
   text in the same language is returned.
5. **Given** an audio clip of Arabic or English speech, **When** speech recognition is
   requested, **Then** the spoken words are returned as text.
6. **Given** an image containing printed Arabic or English text, **When** text extraction is
   requested, **Then** the text in the image is returned.
7. **Given** a text whose language is not stated, **When** the language is requested, **Then**
   Arabic or English is identified, or the text is reported as unsupported.
8. **Given** a request in progress, **When** the person leaves the screen or cancels, **Then**
   the request stops and no result or error is delivered afterwards.

---

### User Story 2 - The app keeps working when a model is unavailable (Priority: P1)

Free models are often busy, rate-limited, still loading, or withdrawn without notice. When the
first-choice model for a capability cannot answer, the app automatically tries the next choice.
The person using the app sees a result, perhaps a little later, and not an error.

**Why this priority**: Free models are the product's operating constraint. Without automatic
fallback, the app fails daily for reasons the user cannot fix.

**Independent Test**: For each capability, make the first-choice model unavailable in each of
the ways listed below and confirm a correct result still arrives from the fallback.

**Acceptance Scenarios**:

1. **Given** the first-choice model reports it is rate-limited or out of quota, **When** a
   request is made, **Then** the fallback is tried and its result is returned.
2. **Given** the first-choice model reports it is still loading or overloaded, **When** a
   request is made, **Then** the fallback is tried and its result is returned.
3. **Given** the first-choice model no longer exists, **When** a request is made, **Then** the
   fallback is tried and its result is returned.
4. **Given** the first-choice model does not answer within the time limit, **When** a request
   is made, **Then** the fallback is tried and its result is returned.
5. **Given** the first-choice model returns output that is empty or not in the expected form,
   **When** a request is made, **Then** the fallback is tried and its result is returned.
6. **Given** every choice for a capability has failed, **When** a request is made, **Then** one
   classified failure is returned that reflects the most useful cause for the user.
7. **Given** the device has no internet connection, **When** a request is made, **Then** a "no
   connection" failure is returned at once, without trying each fallback in turn.
8. **Given** a model was rate-limited a moment ago, **When** the next request arrives shortly
   after, **Then** that model is skipped for a short period so the user does not wait for a
   failure that is already known.

---

### User Story 3 - Failures are explained to the user in their language (Priority: P2)

When a request cannot be completed, the feature screen receives a failure it can turn into a
plain message in Arabic or English, and it knows whether offering "Try again" makes sense. The
user never sees a status code, a provider name, or raw technical text.

**Why this priority**: A failure the user understands is recoverable; a cryptic one loses the
user. It ranks below fallback because good fallback makes failures rare.

**Independent Test**: Trigger each failure category and confirm that it is reported as that
category, that an Arabic and an English message exist for it, and that the "can retry" flag is
correct.

**Acceptance Scenarios**:

1. **Given** no internet connection, **When** a request fails, **Then** the failure is reported
   as "no connection" and marked as worth retrying.
2. **Given** all models for a capability are busy or rate-limited, **When** a request fails,
   **Then** the failure is reported as "service busy" and marked as worth retrying later.
3. **Given** an input the capability cannot accept (empty, too long, unsupported language,
   unsupported file type, or too large), **When** a request is made, **Then** the failure is
   reported as "invalid input" with the specific reason, is not marked as worth retrying, and
   no model is called.
4. **Given** the app's access to a provider is rejected, **When** a request fails, **Then** the
   failure is reported as "service unavailable", and the cause is recorded for developers
   without exposing the credential.
5. **Given** any failure, **When** the feature screen asks for the message, **Then** a message
   exists in both Arabic and English and contains no technical detail.

---

### User Story 4 - A developer changes a model without touching feature code (Priority: P2)

A model is withdrawn, or a better free model appears. A developer changes which model serves a
capability, or reorders the fallbacks, by editing one configuration place. No feature screen
and no capability definition changes.

**Why this priority**: Free models change often. If every change needs code edits across
features, the app decays quickly. It ranks below the first two stories because it concerns
maintenance, not day-one behavior.

**Independent Test**: Replace the first-choice model for one capability with another model, on
the other provider, by changing configuration only. Confirm the capability still passes its
acceptance scenarios and no feature code changed.

**Acceptance Scenarios**:

1. **Given** a capability served by one model, **When** a developer changes the configured
   model to another on the same provider, **Then** the capability works with no other change.
2. **Given** a capability served by one provider, **When** a developer changes the first choice
   to a model on the other provider, **Then** the capability works with no change to feature
   code.
3. **Given** a configuration entry that is missing or malformed, **When** the app starts,
   **Then** the built-in default for that capability is used and the problem is recorded for
   developers.
4. **Given** a developer reading the project, **When** they look for which model serves a
   capability and why, **Then** they find the chosen model, its fallbacks, and the comparison
   that justified the choice in one documented place.

---

### User Story 5 - Screens not yet migrated keep working (Priority: P3)

The existing grammar correction screen and the other screens that use the current way of
calling models continue to work exactly as they do today until each is migrated by its own
later feature.

**Why this priority**: It protects the working app during the transition. It is a guard, not
new value, so it ranks last.

**Independent Test**: After this feature is delivered, run the existing grammar correction flow
and confirm the same behavior as before.

**Acceptance Scenarios**:

1. **Given** the app built with this feature, **When** a user corrects a sentence on the
   existing grammar correction screen, **Then** the result and the screen behavior match the
   previous version.

---

### Edge Cases

- A model returns the answer wrapped in extra text, such as an explanation, quotation marks, or
  a repeat of the instructions. The result delivered must contain only the requested content.
- The input contains text that looks like instructions to the model ("ignore the above
  and..."). It must be handled as content to correct, translate, or summarize, never obeyed.
- The input mixes Arabic and English in one text.
- The input is at or just over the length or size limit for the capability.
- A fallback model answers in the wrong language or returns the input unchanged when it was
  clearly incorrect.
- The connection drops halfway through a response.
- A request is cancelled while a fallback attempt is in progress: no further fallbacks may be
  tried.
- Two requests for the same capability run at the same time.
- An audio clip contains silence only, or an image contains no text: this is an empty result,
  not a failure.
- The total time across the first choice and its fallbacks would exceed what a user will wait
  for.
- A provider's free allowance is used up for the day, so every model on that provider fails.

## Requirements *(mandatory)*

### Functional Requirements

**Capabilities**

- **FR-001**: The app MUST offer these capabilities, each as a separate, named operation that
  features call without reference to a provider or model: grammar correction, translation,
  summarization, speech recognition, text extraction from images, and language detection.
- **FR-002**: Every capability MUST work for Arabic and English input. Translation MUST support
  Arabic to English and English to Arabic.
- **FR-003**: Grammar correction MUST return only the corrected text, in the language of the
  input, preserving the meaning.
- **FR-004**: Summarization MUST return a text shorter than the input, in the language of the
  input.
- **FR-005**: Each capability MUST define and enforce limits on its input (text length, audio
  duration and size, image size, accepted formats) and MUST reject input outside them before
  any model is called, stating the specific reason.
- **FR-006**: A request MUST be cancellable. After cancellation no result or failure is
  delivered and no further model is called.
- **FR-007**: Long text results (correction, translation, summarization) SHOULD be deliverable
  progressively, so a screen can show text as it arrives.

**Providers and models**

- **FR-008**: Capabilities MUST be served only by OpenRouter and Hugging Face. Models with no
  usage charge MUST be used for every first choice and fallback.
- **FR-009**: The model chosen for each capability MUST be selected from a recorded comparison
  of candidate free models on Arabic and English samples. The comparison and the choice MUST be
  kept with the project documentation.
- **FR-010**: A model MUST NOT be adopted before its provider's data-use terms are checked. A
  model whose terms allow user input to be retained or used for training MUST NOT be chosen
  when an alternative of comparable quality exists.
- **FR-011**: Which model serves each capability, and the order of its fallbacks, MUST be
  defined in one configuration place per provider. Changing or reordering models MUST NOT
  require changes to feature code or to the capability definitions.
- **FR-012**: A missing or invalid configuration entry MUST fall back to a built-in default and
  MUST be recorded for developers. It MUST NOT crash the app.

**Fallback**

- **FR-013**: Every capability MUST have at least one fallback. Where a second provider offers
  a suitable free model, the fallback MUST be on a different provider from the first choice.
- **FR-014**: The fallback MUST be tried automatically when a model is rate-limited, out of
  quota, loading, overloaded, withdrawn, too slow, or returns output that is empty or not in
  the expected form.
- **FR-015**: The fallback MUST NOT be tried when the failure cannot be fixed by another model:
  no internet connection, invalid input, or cancellation.
- **FR-016**: A model that has just failed for a temporary reason MUST be skipped for a short
  period so that following requests go straight to a working choice.
- **FR-017**: The total time for one request, across all attempts, MUST be bounded. When the
  bound is reached, the request ends with a classified failure.
- **FR-018**: When all choices fail, exactly one failure MUST be returned, chosen to be the
  most useful for the user (for example "service busy" in preference to "invalid response").

**Failures**

- **FR-019**: Every request MUST end in exactly one of: a result, a classified failure, or
  cancellation. No unclassified error may reach a feature screen.
- **FR-020**: Failures MUST be classified into at least: no connection, timed out, service busy
  (rate limit, quota, loading, overloaded), service unavailable (model gone, access rejected,
  provider error), invalid input (with the specific reason), invalid response, and unknown.
- **FR-021**: Every failure category MUST state whether retrying can help.
- **FR-022**: Every failure category, and every invalid-input reason, MUST have a user-facing
  message in Arabic and in English that contains no status code, provider name, model name, or
  other technical detail.

**Safety and privacy**

- **FR-023**: User-supplied content MUST be passed to a model as clearly separated data. Text
  inside the content that resembles instructions MUST NOT change what the model is asked to do.
- **FR-024**: Model output MUST be checked against the expected form for the capability before
  it is delivered. Output that fails the check is an "invalid response" failure and triggers
  the fallback.
- **FR-025**: Provider credentials MUST NOT appear in the repository, in logs, in failure
  messages, or in crash reports.
- **FR-026**: User content (text, audio, images) and model output MUST NOT be written to logs
  in builds released to users.
- **FR-027**: All communication with providers MUST be encrypted in transit.
- **FR-028**: Diagnostic records for developers MUST state which capability, provider, and
  model were tried, the failure category, and the time taken, without user content.

**Compatibility**

- **FR-029**: Existing screens that use the current way of calling models MUST keep working
  unchanged until each is migrated by its own feature.
- **FR-030**: This feature MUST NOT add or change any screen.

**Verification**

- **FR-031**: Each capability MUST have automated tests for its success path, for each failure
  category, and for its fallback path. These tests MUST NOT call live providers.

### Key Entities

- **Capability**: One kind of AI task the app can ask for (grammar correction, translation,
  summarization, speech recognition, image text extraction, language detection), with its
  input, its result, and its input limits.
- **Provider**: A hosted service that runs models (OpenRouter or Hugging Face), with its own
  address and credential.
- **Model choice**: A specific model on a specific provider that can serve a capability.
- **Fallback chain**: For one capability, the ordered list of model choices: the first choice
  followed by one or more fallbacks.
- **Model configuration**: The one place that states the fallback chain for every capability.
- **AI result**: The outcome of a request: a result, a classified failure, or cancellation.
- **Failure category**: The class of a failure, with whether retrying helps and its Arabic and
  English messages.
- **Model comparison record**: The documented evaluation of candidate models for a capability
  on Arabic and English samples, with the chosen model and the reasons.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For each of the six capabilities, a valid Arabic input and a valid English input
  both produce a correct result in manual checks against the live providers.
- **SC-002**: With the first-choice model made unavailable in each of the five ways (rate
  limit, loading or overloaded, withdrawn, too slow, malformed output), every capability still
  returns a result from its fallback in 100% of automated test cases.
- **SC-003**: 100% of failures reaching a feature screen belong to a defined category and have
  both an Arabic and an English message; none contains a status code or a provider or model
  name.
- **SC-004**: A developer can switch the first-choice model of any capability, including to the
  other provider, by editing one configuration place, in under 10 minutes, with zero changes to
  feature code.
- **SC-005**: A text request (correction, translation, or summarization of a typical paragraph)
  ends with a result or a failure within 30 seconds, including fallbacks. Speech and image
  requests end within 60 seconds.
- **SC-006**: When the device is offline, a request reports "no connection" within 3 seconds.
- **SC-007**: Input that contains instruction-like text is treated as content in 100% of the
  automated test cases written for it.
- **SC-008**: No provider credential and no user content appears in the logs of a build
  released to users, verified by inspection of a full session's log output.
- **SC-009**: The existing grammar correction flow behaves the same before and after this
  feature.
- **SC-010**: Running the app costs nothing in model usage fees.

## Assumptions

Two of these are decisions the roadmap left open. They are stated here as defaults and can be
changed before planning.

- **Where provider keys live (roadmap decision)**: keys ship inside the app and are treated as
  public. Each key is limited on the provider side to free usage with the smallest permissions
  available, and this exception is recorded in the plan, as the constitution allows. A backend
  proxy is safer but needs hosting, which conflicts with the goal of an app that is free to
  run; the layer is designed so a proxy can be placed in front later without changing feature
  code.
- **Where model configuration lives (roadmap decision)**: configuration is bundled with the
  app, in one place per provider. Changing a model therefore needs an app update. The
  configuration is shaped so that a remote source can override it later without changing
  feature code; adding that remote source is outside this feature.
- **Languages**: Arabic and English only, matching the product today.
- **Language detection is included** although the description does not list it, because
  grammar correction and summarization depend on knowing the input language and the app already
  performs it.
- **Text to speech is out of scope.** The description does not list it; it is planned with the
  remaining legacy tools and may be done on the device.
- **"Text extraction" here means text from an image.** Extracting text from PDF and Office
  documents is done on the device without a model and belongs to the document feature. Scanned
  documents will reuse image text extraction.
- **On-device alternatives**: where a hosted free model does not meet the quality bar for
  speech recognition or image text extraction, an on-device option may serve as the first
  choice or the fallback, as the constitution permits. This is decided from the model
  comparison during planning.
- **No user-facing change**: no screen is added or migrated. Feature screens adopt this layer
  in their own later features.
- **Free allowances are limited.** A provider's free daily allowance can run out; when every
  choice is exhausted the user sees "service busy". Raising the limits is outside this feature.
- **No result caching and no offline mode** in this feature.
- **Depends on feature 001** (build foundation), which provides the modules this layer lives
  in. Feature 001 is implemented and awaiting merge.
- **A second provider credential is needed**: an OpenRouter key supplied by the project owner,
  handled the same way as the existing Hugging Face token.
