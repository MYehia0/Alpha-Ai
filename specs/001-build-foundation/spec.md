# Feature Specification: Build Foundation and Module Structure

**Feature Branch**: `001-build-foundation`

**Created**: 2026-11-16

**Status**: Draft

**Input**: User description: "Restructure the project so that features can be built and tested independently. Developers need an up-to-date build, a clear module layout with enforced dependency rules, secrets kept out of source control, and an automated check that runs the build, tests, and lint on every push. The existing app must keep working unchanged."

## User Scenarios & Testing *(mandatory)*

The users of this feature are the developers of Alpha-AI. End users of the app must notice no
difference.

### User Story 1 - The project builds on a current, supported toolchain (Priority: P1)

A developer clones the repository, adds their local secrets file, and builds and runs the app
with a currently supported version of the build tools. The app installs and behaves exactly as
it did before the restructuring.

**Why this priority**: Every later piece of work depends on a build that works with current
tools. If the app stops building or behaves differently, nothing else can proceed.

**Independent Test**: On a clean checkout with a valid local secrets file, run the debug build
and install it on a device. Walk through the existing flows and compare against the app as it
was before the change.

**Acceptance Scenarios**:

1. **Given** a clean checkout and a valid local secrets file, **When** the developer runs the
   debug build, **Then** the build succeeds without manual workarounds.
2. **Given** the newly built app, **When** a tester walks through every existing flow
   (onboarding, sign-in, sign-up, home, each AI tool, history, settings), **Then** each flow
   behaves as it did before the change.
3. **Given** the build configuration, **When** a developer needs to change the version of a
   library or tool, **Then** there is exactly one place to change it.

---

### User Story 2 - Secrets stay out of source control (Priority: P1)

A developer supplies API tokens and other credentials through a local file that is never
committed. The automated check receives the same values through its own protected secret
store. No valid secret exists in any tracked file.

**Why this priority**: Tokens were previously committed to the repository. This must not be
possible to repeat by accident, and it is a non-negotiable project principle.

**Independent Test**: Scan every tracked file for credential patterns and confirm none are
found. Remove the local secrets file and confirm the build reports clearly what is missing.

**Acceptance Scenarios**:

1. **Given** the repository at its latest commit, **When** all tracked files are scanned for
   credentials, **Then** no valid secret is found.
2. **Given** a developer without a local secrets file, **When** they build the project,
   **Then** the build either succeeds with the affected features disabled or fails with a
   message naming the missing value and where to put it.
3. **Given** a developer with secret files in their working folder, **When** they stage all
   changes, **Then** the secret files are not included.

---

### User Story 3 - Every push is checked automatically (Priority: P2)

When a developer pushes a branch or opens a pull request, an automated check builds the
project, runs the unit tests, and runs the static code checks. The developer sees a pass or
fail result on the push, with enough detail to find the cause of a failure.

**Why this priority**: The refactor replaces working code piece by piece. An automatic check
on every push is what catches a broken build or regression before it is merged.

**Independent Test**: Push a commit that passes and confirm a green result. Push a commit with
a deliberate compile error, a failing test, and a lint violation in turn, and confirm each one
produces a red result that identifies the failure.

**Acceptance Scenarios**:

1. **Given** a push to any branch, **When** the automated check runs, **Then** it builds the
   project, runs all unit tests, and runs lint, and reports a single pass or fail status.
2. **Given** a change that breaks the build, a test, or a lint rule, **When** it is pushed,
   **Then** the check fails and its output identifies which step failed and why.
3. **Given** a pull request into `development` or `master`, **When** the check has not passed,
   **Then** the failure is visible on the pull request before merge.
4. **Given** the automated check, **When** it runs, **Then** it does not print any secret
   value in its output.

---

### User Story 4 - A module layout with enforced dependency rules (Priority: P2)

A developer starting a new feature finds an agreed set of shared modules ready to receive
code, each with a stated purpose. If they add a dependency that breaks the layering rules (a
shared module depending on a feature or on the app, or one feature depending on another), the
build rejects it.

**Why this priority**: The module layout is the base for every later spec, but it delivers
value only once the build (Story 1) is sound. The modules start empty; later specs fill them.

**Independent Test**: Confirm each planned shared module exists and builds on its own. Add a
forbidden dependency and confirm the build fails with a message that names the rule.

**Acceptance Scenarios**:

1. **Given** the restructured project, **When** a developer lists its modules, **Then** the
   app module and every planned shared module exist, and each has a short statement of its
   purpose.
2. **Given** any single shared module, **When** a developer builds and tests only that module,
   **Then** it succeeds without building the app module.
3. **Given** a shared module, **When** a developer makes it depend on the app module or on a
   feature module, **Then** the build fails and names the violated rule.
4. **Given** a new module is needed, **When** a developer creates it, **Then** it takes its
   shared build settings from a common definition instead of a copied configuration.

---

### Edge Cases

- A developer has no local secrets file, or the file lacks a required value.
- A contributor's fork or pull request has no access to the automated check's secrets: the
  check must still build, test, and lint.
- The toolchain upgrade surfaces new warnings or deprecations in the existing code: these must
  not block the build, and must not hide new problems in changed files.
- An existing library has no version compatible with the upgraded toolchain.
- An empty shared module has no tests or sources yet: building and testing it must still pass.
- The old annotation processing setup and the new one both exist for a time during migration.
- A developer builds from a terminal without the development environment's bundled runtime.

## Requirements *(mandatory)*

### Functional Requirements

**Build**

- **FR-001**: The project MUST build with currently supported stable versions of its build
  tools and language, and MUST meet the platform and language levels set in the constitution.
- **FR-002**: Every library and tool version MUST be declared in a single shared catalog; no
  version number may be written directly in a module's build file.
- **FR-003**: Build settings common to several modules MUST be defined once and reused by each
  module.
- **FR-004**: The project MUST be ready to host screens built with the new UI toolkit named in
  the constitution alongside the existing screens, without migrating any screen in this
  feature.
- **FR-005**: The project documentation MUST state how to set up a machine and build the app
  from both the development environment and a terminal.

**Module layout**

- **FR-006**: The project MUST contain the app module plus the shared modules named in the
  constitution: common, domain, data, network, datastore, designsystem, ui, and testing.
- **FR-007**: Each shared module MUST build and run its tests independently of the app module.
- **FR-008**: The build MUST fail when a shared module depends on the app module or on a
  feature module, and when a feature module depends on another feature module.
- **FR-009**: The domain module MUST NOT be able to use platform, UI, network, or backend
  libraries.
- **FR-010**: Each module MUST carry a short written statement of its purpose and what it may
  depend on.

**Secrets**

- **FR-011**: Secrets MUST be read from a local file that is excluded from source control, and
  from the automated check's protected secret store.
- **FR-012**: No tracked file may contain a valid credential. An example file listing the
  required names with placeholder values MUST be provided.
- **FR-013**: When a required secret is missing, the build MUST either continue with a clear
  warning or stop with a message naming the missing value and where to supply it. It MUST NOT
  fail with an unexplained error.
- **FR-014**: The automated check MUST fail if a credential pattern is detected in the tracked
  files of a pushed change.

**Automated check**

- **FR-015**: An automated check MUST run on every push to any branch and on every pull
  request into `development` or `master`.
- **FR-016**: The check MUST build the debug variant, run all unit tests, and run lint, and
  MUST report one overall pass or fail status.
- **FR-017**: On failure, the check MUST make the failing step and its report available to the
  developer.
- **FR-018**: The check MUST NOT expose secret values in its logs or stored outputs, and MUST
  still run for changes that have no access to secrets.

**Behavior preservation**

- **FR-019**: Every existing user flow MUST behave as it did before this feature. No screen,
  text, or behavior visible to end users may change.
- **FR-020**: Existing code MUST stay where it is in the app module unless moving it is
  required to make the build work. Moving features into modules belongs to later specs.
- **FR-021**: Lint MUST fail the build only for new problems; problems already present in
  existing code MUST be recorded as a baseline and not block the check.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer with a prepared machine goes from a clean checkout to the app
  running on a device in under 15 minutes, following only the written setup steps.
- **SC-002**: 100% of the existing user flows pass a manual comparison against the app as it
  was before the change, with no differences observed.
- **SC-003**: A scan of all tracked files finds zero valid secrets.
- **SC-004**: 100% of pushes and pull requests receive an automated pass or fail result, and
  the result arrives within 15 minutes of the push.
- **SC-005**: Each of the three deliberate failures (compile error, failing test, lint
  violation) is caught by the automated check and identified by step.
- **SC-006**: Every forbidden dependency direction that is tried is rejected by the build.
- **SC-007**: Each shared module can be built and tested on its own, in less time than a full
  project build.
- **SC-008**: Changing a library version requires editing exactly one file.

## Assumptions

- The users of this feature are the project's developers. End users see no change.
- Shared modules are created empty, apart from their build setup and purpose statement. Later
  specs (002 onward) move or add code.
- No feature modules are created in this feature. The dependency rules for them are put in
  place now so that they apply when the first feature module is added.
- No existing screen is migrated to the new UI toolkit in this feature; the project is only
  made ready for it.
- The repository stays on GitHub, and the automated check uses the hosting service's built-in
  facility, on its free tier.
- The automated check covers the debug build, unit tests, and lint. Device or emulator tests
  and release signing are out of scope.
- Tokens already present in the git history are revoked (Phase 0) and are not purged by this
  feature.
- Existing lint findings are baselined instead of fixed here; fixing them belongs to the specs
  that touch the affected code.
- The single required secret today is the Hugging Face token. The backend service
  configuration file already tracked in the repository is treated as public client
  configuration, not a secret.
- If an existing library has no version that works with the upgraded toolchain, it is replaced
  by the nearest equivalent only where needed to keep the app working, and the replacement is
  recorded in the plan.
