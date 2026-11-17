# Feature Specification: Design System and App Shell

**Feature Branch**: `003-design-system-shell`

**Created**: 2026-11-17

**Status**: Draft

**Input**: User description: "Define the look and feel of Alpha-AI and the frame every screen lives in: colors, typography, spacing, shapes, motion, and the shared components (buttons, text fields, cards, top bar, loading, empty, and error states). Support light and dark themes, Arabic and English with correct right-to-left layout, and large font sizes. Provide one navigation frame that later screens plug into."

## Clarifications

### Session 2026-11-17

- Q: Which blue should Alpha-AI use where text sits on the brand color? → A: The darker brand blue #1976D2 (white text on it passes WCAG AA); #2196F3 stays for accents, icons, and the logo.
- Q: Which typeface should Alpha-AI use, given that Inter has no Arabic letters? → A: IBM Plex Sans Arabic, one family for both Arabic and Latin text.
- Q: Should users pick Arabic or English inside the app, or should it follow the phone's language? → A: Follow the phone's language in this spec (including the system per-app language setting where the device offers one); the in-app choice is added by spec 005.
- Q: Should the app recolor itself to match the user's wallpaper on newer Android versions? → A: No; it always uses the Alpha-AI palette so the brand and the tested contrast stay the same on every phone.

## User Scenarios & Testing *(mandatory)*

This feature has two audiences. People using Alpha-AI see one consistent, readable app in their
language and theme. The people building the next specs (onboarding, home, grammar correction,
and the AI tools) get one set of building blocks and one frame to place each new screen in, so
no screen has to invent its own colors, spacing, or states.

Nothing in the existing user flow changes in this feature. The legacy screens keep working
until their own specs replace them; this feature only provides what those specs will use.

### User Story 1 - One visual language in light and dark themes (Priority: P1)

A reviewer opens the component gallery and sees every color, text style, spacing step, shape,
and motion style of Alpha-AI in one place, in the light theme and in the dark theme. Every
element in both themes is readable, and the brand is recognizably Alpha-AI (its blue).

**Why this priority**: every later screen is built from these values. Without them, each spec
would pick its own colors and sizes and the app would look like separate tools rather than one
product.

**Independent Test**: open the gallery, switch the device between light and dark mode, and
check each section against the acceptance scenarios below. It delivers value alone because the
next spec can start using the values immediately.

**Acceptance Scenarios**:

1. **Given** the device is in light mode, **When** the gallery is opened, **Then** it shows the
   full color palette with each color's role (for example primary, surface, error), every text
   style from the largest heading to the smallest label, the spacing scale, the shape scale,
   and a demonstration of each motion style.
2. **Given** the gallery is open, **When** the device switches to dark mode, **Then** the
   gallery redraws in the dark palette without restarting, and every text and icon remains
   readable against its background.
3. **Given** any text and background pair defined for use together, **When** its contrast is
   measured, **Then** it meets the WCAG AA level (4.5:1 for body text, 3:1 for large text and
   icons) in both themes.
4. **Given** the device has "remove animations" turned on, **When** a motion demonstration runs,
   **Then** the change happens without animation and the end state is the same.

---

### User Story 2 - Arabic and English with correct direction and large text (Priority: P1)

A user whose device is set to Arabic sees the app in Arabic, laid out right to left: text,
icons that imply direction (back arrows, chevrons), and the order of items in rows are
mirrored. A user whose device is set to English sees the same screens left to right. A user who
needs large text sets the system font size to the maximum and every text remains fully visible
and usable.

**Why this priority**: Alpha-AI is a writing assistant for Arabic and English speakers. A screen
that is wrong in Arabic or unreadable at large font sizes fails a core audience, and fixing it
per screen later costs far more than building it in once.

**Independent Test**: switch the device language between Arabic and English and the font size
between default and largest, then walk the gallery and the app frame. Every component must pass
in all four combinations.

**Acceptance Scenarios**:

1. **Given** the device language is Arabic, **When** the gallery or frame is shown, **Then** all
   text is in Arabic, the layout runs right to left, and directional icons point the right way.
2. **Given** the device language is English, **When** the same screens are shown, **Then** all
   text is in English and the layout runs left to right.
3. **Given** the device font size is at its largest setting (up to 200% of default), **When** any
   component is shown, **Then** no text is cut off or overlaps other content, and content that
   no longer fits on screen can be scrolled.
4. **Given** text that mixes Arabic and English (for example an English product name inside an
   Arabic sentence), **When** it is displayed, **Then** each part keeps its own reading order and
   the sentence reads correctly.
5. **Given** the app's existing 60 user-visible strings, **When** the device language is Arabic,
   **Then** each of them has an Arabic translation.

---

### User Story 3 - Shared components with every state (Priority: P2)

A developer building a new screen takes a button, text field, card, top bar, or a loading,
empty, or error state from the shared set instead of making their own. A user then meets the
same component behaving the same way on every screen: the same button press feedback, the same
field error style, and the same way of saying "loading", "nothing here yet", or "something went
wrong, try again".

**Why this priority**: the components depend on the visual language (story 1) and must already
respect direction and font size (story 2). They are what makes the app consistent across its
many tools.

**Independent Test**: the gallery shows each component in each of its states, in both themes
and both languages; each can be checked against the scenarios below without any feature
screen existing.

**Acceptance Scenarios**:

1. **Given** the gallery, **When** the buttons section is viewed, **Then** it shows a primary, a
   secondary, and a text-style button, each in its normal, pressed, disabled, and busy (action
   in progress) states.
2. **Given** a text field, **When** it is empty, focused, filled, disabled, or showing an error,
   **Then** each state is visually distinct and the error state shows its message below the
   field, readable by a screen reader.
3. **Given** a multi-line text field meant for long input, **When** text longer than the visible
   area is entered, **Then** the field grows to a limit and then scrolls, and the character
   count (where a limit applies) is shown.
4. **Given** the loading state, **When** it is shown, **Then** it tells the user something is in
   progress and, where the operation can be stopped, offers a cancel action.
5. **Given** the empty state, **When** it is shown, **Then** it explains in a short sentence that
   there is nothing to show yet and, where useful, offers one action to get started.
6. **Given** the error state with a failure that can be retried, **When** it is shown, **Then** it
   gives the localized failure message and a retry action; for a failure that cannot be
   retried, it gives the message without a retry action.
7. **Given** any interactive component, **When** it is measured, **Then** its touch target is at
   least 48 by 48 density-independent pixels, and a component with no visible text has a spoken
   description for screen readers.

---

### User Story 4 - One frame for every screen (Priority: P3)

A user opens Alpha-AI and lands in the existing flow exactly as before. Behind it, the app now
has one frame that new screens are placed into: a consistent top bar, a consistent way to move
between screens, and predictable system back behavior. A developer adds a new screen to the
frame without changing other screens, and the legacy screens stay reachable until their own
specs replace them.

**Why this priority**: the frame is needed by spec 004 (onboarding and authentication), the
next one. It can be built last within this feature because it only hosts the components from
stories 1 to 3.

**Independent Test**: install the app and walk the existing flow (onboarding, sign-in, home,
each tool screen) to confirm nothing changed; then open the component gallery through the frame
and use system back to leave it.

**Acceptance Scenarios**:

1. **Given** a fresh install, **When** the app is opened, **Then** the user sees the same first
   screen and flow as before this feature.
2. **Given** a signed-in user on the existing home screen, **When** they open any existing tool
   screen and press back, **Then** they return to home as before.
3. **Given** the frame, **When** a screen in it is opened and system back is pressed, **Then** the
   user returns to the previous screen in the frame, and from the frame's first screen back
   leaves the frame as it did before.
4. **Given** a debug build, **When** a developer opens the component gallery, **Then** it is
   reachable through the frame; in a release build the gallery is not reachable by users.
5. **Given** the frame, **When** the device is rotated or the app is sent to the background and
   restored, **Then** the user is on the same screen with the same content.

### Edge Cases

- The device language is neither Arabic nor English (for example French): the app shows English,
  left to right.
- The user changes the device language or theme while the app is open: the visible screen
  updates to the new language or theme without losing what the user typed.
- Very long words or unbroken text (a URL, a long Arabic word) in a card or field: it wraps or
  scrolls and never pushes other content off screen.
- Largest font size on a small phone (a 360dp-wide screen): the top bar title shortens with an
  ellipsis rather than overlapping its actions, and buttons with long labels grow in height
  instead of cutting text.
- Numbers in Arabic text: the app uses the digits of the device's Arabic locale setting
  consistently within one screen.
- A loading state shown for a very short operation: it does not flash on and off; an operation
  that ends quickly shows no loading state at all.
- An error message that is long in Arabic: the error state wraps it and keeps the retry action
  visible.

## Requirements *(mandatory)*

### Functional Requirements

**Visual language**

- **FR-001**: The app MUST have one defined color palette, with a named role for each color
  (for example primary, on-primary, surface, error), in a light and a dark variant. In the
  light theme the primary color MUST be the darker brand blue #1976D2, used wherever text or
  icons sit on the brand color; the lighter brand blue #2196F3 MAY be used only for accents,
  decorative icons, and the logo, never behind text. The app MUST always use this palette and
  MUST NOT take its colors from the user's wallpaper.
- **FR-002**: Every pair of foreground and background colors defined for use together MUST meet
  WCAG AA contrast in both themes.
- **FR-003**: The app MUST have one typography scale (display, headline, title, body, and label
  sizes) using one typeface, IBM Plex Sans Arabic, for both Arabic and Latin letters, so mixed
  Arabic and English text looks like one family. The typeface MUST be bundled with the app so
  it looks the same on every device and works offline.
- **FR-004**: The app MUST have one spacing scale and one shape (corner rounding) scale that all
  components use. Screens MUST NOT need values outside these scales.
- **FR-005**: The app MUST have a small set of named motion styles (durations and easing) for
  entering, leaving, and changing content, and all motion MUST use them.
- **FR-006**: When the system "remove animations" setting is on, every animation MUST be skipped
  and go straight to its end state.

**Language, direction, and text size**

- **FR-007**: The app MUST follow the device language for Arabic and English, and MUST honor the
  system's per-app language setting on devices that offer one; any other language falls back
  to English. This spec adds no in-app language or theme switch, but the design MUST let spec
  005 (settings) add one without changing existing screens.
- **FR-008**: Layout MUST mirror for Arabic: reading order, alignment, the order of items in a
  row, and directional icons. Icons that do not imply direction (for example a microphone) MUST
  NOT mirror.
- **FR-009**: Every user-visible string in the app, including the existing 60, MUST have an
  English and an Arabic version.
- **FR-010**: Every component MUST stay fully usable at system font sizes up to 200%: no cut-off
  or overlapping text, and scrolling where content no longer fits.

**Shared components**

- **FR-011**: The shared set MUST include at least: primary, secondary, and text buttons; an
  icon button; single-line and multi-line text fields; a card; a top bar; and loading, empty,
  and error states.
- **FR-012**: Buttons MUST show normal, pressed, disabled, and busy states. A busy button MUST
  NOT accept another press.
- **FR-013**: Text fields MUST show empty, focused, filled, disabled, and error states; an error
  message MUST be announced by screen readers. Multi-line fields MUST grow up to a limit, then
  scroll, and MUST be able to show a character count.
- **FR-014**: The loading state MUST be able to offer a cancel action, and MUST NOT appear for
  operations that finish in under about half a second.
- **FR-015**: The error state MUST show a localized message for every failure type the AI layer
  reports (spec 002) and MUST offer retry only for failures that can be retried.
- **FR-016**: Every interactive component MUST have a touch target of at least 48 by 48
  density-independent pixels and, where it has no visible text, a spoken description.
- **FR-017**: Every component MUST be shown in a gallery in each of its states, in light and
  dark themes, in Arabic and English.

**App frame**

- **FR-018**: The app MUST have one frame that hosts new screens, with one consistent top bar
  and one consistent way to move between screens. New screens MUST be added to the frame
  without changing other screens.
- **FR-019**: System back MUST behave predictably in the frame: it returns to the previous
  screen, and from the frame's first screen it leaves as the app did before.
- **FR-020**: The frame MUST keep the current screen and its content across rotation and after
  the app is restored from the background.
- **FR-021**: The existing user flow (onboarding, sign-up, sign-in, home, each tool screen,
  history) MUST look and behave exactly as before. Legacy screens MUST stay reachable until
  their own specs replace them.
- **FR-022**: The component gallery MUST be reachable in development builds and MUST NOT be
  reachable by users of release builds.

### Key Entities

- **Design tokens**: the named values every screen uses: color roles (light and dark), text
  styles, spacing steps, shape steps, and motion styles.
- **Component**: a reusable building block with a defined set of states (for example a button
  with normal, pressed, disabled, busy) that reads only from the design tokens.
- **Screen state presentation**: the shared way a screen shows that it is loading, empty, in
  error, or showing content.
- **App frame**: the single container that hosts screens, owns the top bar and moving between
  screens, and keeps the current screen across rotation and restore.
- **Destination**: a screen the frame can show, identified so that later specs can add their own
  without editing others.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the components in the shared set appear in the gallery in all four
  combinations of theme (light, dark) and language (Arabic, English), in every state.
- **SC-002**: 100% of the text and background pairs in the palette meet WCAG AA contrast in both
  themes.
- **SC-003**: At 200% system font size on a 360dp-wide screen, a reviewer finds zero cut-off or
  overlapping texts across the gallery and the frame.
- **SC-004**: With the device in Arabic, a reviewer finds zero screens in the gallery or frame
  with left-to-right layout, untranslated text, or a directional icon pointing the wrong way.
- **SC-005**: 100% of the app's user-visible strings (at least the existing 60) have an Arabic
  translation.
- **SC-006**: A walk-through of the existing flow after this feature shows zero differences from
  before it (same screens, same order, same results).
- **SC-007**: A developer can add a new screen to the frame and reach it from the gallery in
  under 30 minutes by following the documented steps, without editing any existing screen.
- **SC-008**: Every interactive component in the gallery measures at least 48 by 48
  density-independent pixels and has a spoken label where it has no text, as checked with the
  system accessibility scanner.

## Assumptions

- The brand stays recognizably Alpha-AI: its blues (#1976D2 as primary, #2196F3 as accent) and
  the logo are unchanged. A full rebrand is out of scope. The dark theme's primary is a lighter
  tint of the same blue, chosen in planning to pass contrast on dark surfaces.
- The current typeface (Inter) has no Arabic letters, so it is replaced by IBM Plex Sans Arabic
  (free to bundle under the SIL Open Font License) in the new design system. Legacy screens
  keep Inter until their own specs migrate them.
- The app follows the device language and theme in this feature. The in-app language and theme
  choice belongs to spec 005 (home and settings); this feature only makes it possible.
- Existing screens are not restyled in this feature. Each one adopts the new design when its own
  spec migrates it (004 onward). Arabic translations of their strings are added now so they are
  ready.
- The component gallery is a development tool for reviewers and developers, not a user feature.
- The loading, empty, and error states are visual building blocks; deciding when a screen uses
  them belongs to each feature spec.
- Arabic translations are written for Modern Standard Arabic and reviewed by a native speaker on
  the team.
- The frame is introduced alongside the legacy screens; removing legacy screens is done by the
  specs that replace them (004 to 013) and the final cleanup in spec 014.
