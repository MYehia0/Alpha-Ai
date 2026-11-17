# :core:data

Repository implementations and mappers between DTOs and domain models.

**May depend on**: `:core:common`, `:core:domain`, `:core:network`, `:core:datastore`.

## What is here

The implementations of the AI capabilities declared in `:core:domain` (spec 002), under
`ai/`:

- `config/`: which model serves which capability. `OpenRouterModels.kt` and
  `HuggingFaceModels.kt` are the only places a model identifier or provider address appears.
  `ModelConfigResolver` checks the entries and builds each capability's fallback chain.
- `validate/`: input limits, on-device Arabic/English detection, and output checks.
- `prompt/`: the versioned instructions. User content is always a separate message.
- `fallback/`: the fallback chain, cooldowns for models that failed recently, time limits,
  and the diagnostic log lines (which never contain input, output, or a key).
- `engine/`: sends a request to whichever provider a model lives on.

To change a model, edit one of the two files in `config/`, run the tests, run the live check
for that capability, and record the reason in [docs/ai-models.md](../../docs/ai-models.md).

### Live check

Tests never call a real provider. `LiveCapabilityCheck` does, and is skipped unless asked
for. It spends requests from the 50-a-day free allowance:

```bash
./gradlew :core:data:testDebugUnitTest -PliveAi -Pcapability=grammar
```

`capability` is `grammar`, `translation`, `summarization`, `image`, or `speech`. Add
`-Pcompare` to run every candidate model. Results go to `core/data/build/reports/ai-live/`.

See [the module rules](../../specs/001-build-foundation/data-model.md).
