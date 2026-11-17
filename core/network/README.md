# :core:network

HTTP clients, AI provider services, and interceptors.

**May depend on**: `:core:common`.

## What is here

The clients that talk to the AI providers (spec 002):

- `OpenRouterClient` and `HuggingFaceClient`: one call is one HTTP request. They return a
  `ProviderResult` or a stream of `ChatStreamEvent`, and never throw (except cancellation).
- `ProviderFailure`: what went wrong, classified from the status, headers, and error body.
- `AiCredentials`: how the app hands over the provider keys. A blank key sends nothing.
- `ConnectivityChecker`: whether the device is online.
- `AiClientFactory`: builds the clients without Hilt, for the opt-in live check only.

Everything under `internal/` (Retrofit services, wire models, error classifiers, the
event-stream reader) is private to this module. HTTP logging exists in debug builds only
(`src/debug`); the release source set has a no-op, so a release build cannot log requests.

Nothing here knows about capabilities, prompts, or which model to use: that is `:core:data`.

See [the module rules](../../specs/001-build-foundation/data-model.md).
