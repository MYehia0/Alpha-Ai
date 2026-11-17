# :core:testing

Shared fakes, test dispatchers, and test rules for other modules' tests.

**May depend on**: `:core:common`, `:core:domain`.

## What is here

`ai/FakeAiCapabilities.kt`: a fake for each AI capability interface. Set the `result` to
return and read `calls` to see what was asked.

See [the module rules](../../specs/001-build-foundation/data-model.md).
