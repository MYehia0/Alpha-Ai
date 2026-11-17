# Contract: Provider API

**Owner**: `:core:network`, package `com.example.alpha_ai.core.network`

**Consumer**: `:core:data` only.

This file has two parts: what the module offers to `:core:data`, and the wire formats it
speaks to each provider. The wire formats were read from the providers' documentation on
2026-11-17 (research.md, section 1).

## Part 1: What `:core:network` offers

```kotlin
interface OpenRouterClient {
    suspend fun chat(request: ChatRequest): ProviderResult<ChatReply>
    fun chatStream(request: ChatRequest): Flow<ChatStreamEvent>
}

interface HuggingFaceClient {
    suspend fun chat(request: ChatRequest): ProviderResult<ChatReply>
    fun chatStream(request: ChatRequest): Flow<ChatStreamEvent>
    suspend fun transcribe(model: String, audio: ByteArray, mimeType: String): ProviderResult<String>
}

interface AiCredentials { fun tokenFor(provider: AiProvider): String? }

interface ConnectivityChecker { fun isOnline(): Boolean }
```

| # | Guarantee |
|---|---|
| N-1 | No function throws, except `CancellationException`. Every other outcome is a `ProviderResult` or a terminal `ChatStreamEvent` |
| N-2 | Every failure is one of the `ProviderFailure` values in data-model.md section 5 |
| N-3 | An HTTP `200` whose body carries an `error` object is reported as a failure, never as success |
| N-4 | A reply with no choices or blank text is `MalformedResponse`; a reply cut off by the token limit sets `ChatReply.truncated` |
| N-5 | The `Authorization` header is added by an interceptor from `AiCredentials`; callers never pass a key. A blank key yields `AccessRejected` without sending anything |
| N-6 | Cancelling the coroutine cancels the HTTP call |
| N-7 | No retry is made inside the client. One call is one HTTP request |
| N-8 | Nothing is logged in release builds. In debug builds only the request line and status are logged, with `Authorization` redacted |
| N-9 | Each provider has its own `OkHttpClient`, derived from one shared base client: connect 5 s, read 30 s, write 30 s |

## Part 2: Wire formats

### OpenRouter

- Base URL: `https://openrouter.ai/api/v1/`
- Headers: `Authorization: Bearer <key>`, `Content-Type: application/json`

**`POST chat/completions`** request:

```json
{
  "model": "google/gemma-4-31b-it:free",
  "messages": [
    { "role": "system", "content": "<instruction>" },
    { "role": "user", "content": [
        { "type": "text", "text": "<content>...</content>" },
        { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,..." } },
        { "type": "input_audio", "input_audio": { "data": "<base64>", "format": "wav" } }
    ] }
  ],
  "max_tokens": 1024,
  "temperature": 0,
  "stream": false
}
```

A text-only user message sends `content` as a plain string. Image and audio parts appear only
for image text extraction and speech recognition.

Success response (fields used):

```json
{
  "model": "google/gemma-4-31b-it:free",
  "choices": [ { "message": { "content": "..." }, "finish_reason": "stop" } ]
}
```

Error response, with a matching HTTP status or with status `200`:

```json
{ "error": { "code": 429, "message": "...", "metadata": { "error_type": "rate_limit_exceeded" } } }
```

**Streaming** (`"stream": true`): `text/event-stream`. Each `data:` line holds a chunk with
`choices[0].delta.content`. Lines starting with `:` are comments and are ignored.
`data: [DONE]` ends the stream. An error after the stream has started is a chunk with a
top-level `error` object and `finish_reason: "error"`.

**Classification** (`error_type` first, then status):

| Signal | `ProviderFailure` |
|---|---|
| 429 with `X-RateLimit-Remaining: 0` | `QuotaExhausted(resetAt = X-RateLimit-Reset)` |
| 429, `rate_limit_exceeded` | `RateLimited(Retry-After)` |
| 503, `provider_overloaded` | `Overloaded` |
| 502, `provider_unavailable`, 500 | `ServerError` |
| 404, `not_found` | `ModelNotFound` |
| 401, `authentication`; 403 `permission_denied` | `AccessRejected` |
| 402, `payment_required` | `NoCredits` |
| 403 `content_policy_violation`, `refusal` | `ContentBlocked` |
| 408 | `Timeout` |
| 400, 413, 422 and their `error_type` values | `RequestRejected` |
| `finish_reason: "length"` | success with `truncated = true` |
| body does not parse, or no usable choice | `MalformedResponse` |

### Hugging Face

- Base URL: `https://router.huggingface.co/`
- Headers: `Authorization: Bearer <token>`

**`POST v1/chat/completions`**: same request and success shape as OpenRouter's (text and
`image_url` parts). Streaming uses the same event format.

Error response shapes (both occur):

```json
{ "error": "Purchase pre-paid credits to continue using Inference Providers. ..." }
```

```json
{ "error": { "message": "...", "type": "invalid_request_error", "code": "model_not_supported" } }
```

**`POST hf-inference/models/{model}`** for speech recognition: the body is the raw audio
bytes with `Content-Type` set to the audio's MIME type. Response:

```json
{ "text": "..." }
```

**Classification**:

| Signal | `ProviderFailure` |
|---|---|
| 402 | `NoCredits` |
| 401, 403 | `AccessRejected` |
| 404; 400 with `model_not_supported` | `ModelNotFound` |
| 429 | `RateLimited(Retry-After)` |
| 503 with `estimated_time` in the body | `Loading(estimate)` |
| 503 otherwise | `Overloaded` |
| 500, 502, 504 | `ServerError` |
| other 4xx | `RequestRejected` |
| body does not parse, or blank text | `MalformedResponse` |

The `estimated_time` field on 503 is the shape of the older serverless API and is **not
verified** against the current service; the classifier treats a 503 without it as
`Overloaded`.

### Transport errors (both providers)

| Signal | `ProviderFailure` |
|---|---|
| `UnknownHostException`, `ConnectException`, `NoRouteToHostException` | `NoConnection` |
| `SocketTimeoutException` | `Timeout` |
| other `IOException` while reading the body | `MalformedResponse` |
| `SerializationException` | `MalformedResponse` |
| anything else | `Unknown` |

## Tests required (MockWebServer)

One test per row of each classification table, plus: success for text, image, and audio
requests (asserting the request body sent); a `200` with an error body; a stream with comment
lines, a normal end, and a mid-stream error; a dropped connection; the `Authorization` header
present and correct; a blank key sending nothing; cancellation closing the connection.
