# Feature Specification: Onboarding and Authentication

**Feature Branch**: `004-onboarding-auth`

**Created**: 2026-11-18

**Status**: Draft

**Input**: User description: "A new user opens the app, sees a short introduction once, creates an account or signs in, and lands on the home screen. A returning user goes straight to home. Users can reset a forgotten password and sign out. Errors are explained clearly in the user's language, and the user never sees a screen they are immediately redirected away from."

## Clarifications

### Session 2026-11-18

- Q: Should users also be able to sign in with their Google account, or only with email and password? → A: Email and password only in this spec; Google sign-in can be added by a later spec.
- Q: Must a new user confirm their email address before they can use the app? → A: No. A confirmation email is sent at sign-up and the user reaches home right away; home shows a reminder with a resend option until the email is confirmed.
- Q: Until spec 005 rebuilds home, where should the email confirmation reminder and its resend option appear? → A: This spec shows a one-time "confirmation link sent to <email>" message on the first arrival at home after sign-up; the lasting reminder with resend moves to spec 005's new home, and the old home screen is not changed.

## User Scenarios & Testing *(mandatory)*

This feature replaces the first minutes of Alpha-AI: the splash, the introduction, sign-up, and
sign-in. It also restores the password reset that was removed earlier, and makes sign-out
reliable. Everything a user sees here follows the design system from spec 003: light and dark
themes, Arabic and English with the correct reading direction, and large font sizes.

The home screen itself is not part of this feature. After a successful sign-in the user lands
on the home screen that exists today; spec 005 replaces it.

### User Story 1 - Returning user goes straight to home (Priority: P1)

A person who has already signed in on this phone opens Alpha-AI and the first screen they see
is home. They do not see the introduction, the sign-in screen, or a screen that flashes and
disappears. This keeps working when the phone has no connection.

**Why this priority**: most app launches are by returning users. A slow or flickering start on
every launch is the most visible quality problem in this part of the app, and the old version
kept the user signed in by storing their password on the phone, which must stop.

**Independent Test**: sign in once, close the app fully, reopen it (with and without a
connection) and check that home is the first screen drawn.

**Acceptance Scenarios**:

1. **Given** a user signed in earlier on this phone, **When** they open the app, **Then** the
   first screen drawn after the launch screen is home, with no other screen shown in between.
2. **Given** a signed-in user and no network connection, **When** they open the app, **Then**
   they still reach home, and features that need the network explain that themselves.
3. **Given** a signed-in user whose account was disabled or deleted elsewhere, **When** the app
   learns this (on launch with a connection, or later), **Then** the user is signed out, taken
   to sign-in, and told in their language that they need to sign in again.
4. **Given** a user who signed in with the old version of the app, **When** they update to this
   version and open it, **Then** they reach home without signing in again if their session is
   still valid, and any password the old version stored on the phone is erased.

---

### User Story 2 - New user is introduced once, signs up, and reaches home (Priority: P1)

A person opens Alpha-AI for the first time. They see a short introduction to what the app does
(at most three pages, skippable at any point), then choose to create an account. They enter
their name, email, and a password, and land on home already signed in.

**Why this priority**: without it no new user can use the app at all.

**Independent Test**: install the app fresh, go through the introduction, create an account
with a new email, and check that home appears and the user is signed in. Close and reopen the
app and check that the introduction is not shown again.

**Acceptance Scenarios**:

1. **Given** the app has never been opened on this phone, **When** it is opened, **Then** the
   introduction is the first screen drawn after the launch screen.
2. **Given** the introduction is showing, **When** the user taps skip on any page or finishes
   the last page, **Then** they reach the sign-in screen, with a clear way to create an account.
3. **Given** the user has finished or skipped the introduction once, **When** they open the app
   again while signed out, **Then** they go straight to sign-in and never see the introduction
   again on this phone.
4. **Given** the sign-up form, **When** the user enters a name, a valid email that has no
   account, and a password that meets the rules, and confirms it, **Then** the account is
   created, they are signed in, and they land on home.
5. **Given** the sign-up form, **When** a field is invalid (empty name, malformed email,
   password too short, confirmation that does not match), **Then** the problem is shown next to
   that field in the user's language before anything is sent, and the form keeps what was typed.
6. **Given** an email that already has an account, **When** the user tries to sign up with it,
   **Then** they are told the email is already registered and offered a way to sign in instead.
7. **Given** a user who has just signed up, **When** they land on home for the first time,
   **Then** a confirmation email has been sent to their address and a one-time message tells
   them so, naming the address; the message is not shown again, and nothing in the app is
   blocked while the email is unconfirmed.

---

### User Story 3 - Existing user signs in (Priority: P1)

A signed-out user with an account enters their email and password and reaches home. If
something goes wrong, they are told what happened and what to do, in their language.

**Why this priority**: existing users who reinstall, change phones, or sign out need it to get
back to their work.

**Independent Test**: with an existing account and a signed-out app, sign in with correct
details and reach home; then try a wrong password, no connection, and a malformed email, and
check each message.

**Acceptance Scenarios**:

1. **Given** the sign-in screen, **When** the user enters the correct email and password,
   **Then** they land on home and stay signed in across app restarts.
2. **Given** the sign-in screen, **When** the email or password is wrong, **Then** the user sees
   one message saying the email or password is incorrect, without revealing which one.
3. **Given** there is no connection, **When** the user tries to sign in or sign up, **Then** they
   are told they are offline, nothing they typed is lost, and they can try again.
4. **Given** too many failed attempts, **When** the service temporarily blocks further tries,
   **Then** the user is told to wait and try again later, or to reset their password.
5. **Given** a request is in progress, **When** the user taps the submit button again, **Then**
   no second request is sent, and the screen shows that it is working.

---

### User Story 4 - User resets a forgotten password (Priority: P2)

A user who cannot remember their password chooses "Forgot password" on the sign-in screen,
enters their email, and receives a message with a link to choose a new password. They then sign
in with the new password.

**Why this priority**: without it, a user who forgets their password loses their account and
history. It is second to the main sign-in and sign-up paths because it is used less often.

**Independent Test**: from sign-in, request a reset for an existing account, open the email,
set a new password, and sign in with it in the app.

**Acceptance Scenarios**:

1. **Given** the sign-in screen, **When** the user taps "Forgot password", **Then** a reset
   screen opens with the email already filled in if they had typed one.
2. **Given** the reset screen, **When** the user submits a valid email, **Then** they see the
   same confirmation whether or not an account exists for that email, telling them to check
   their inbox (and spam folder) and how to get back to sign-in.
3. **Given** the reset email was sent, **When** the user follows its link and chooses a new
   password, **Then** they can sign in in the app with the new password and the old one no
   longer works.
4. **Given** the user has just requested a reset, **When** they try to request another one
   right away, **Then** they are asked to wait a short time before sending again.

---

### User Story 5 - User signs out (Priority: P2)

A signed-in user signs out from the app. They are taken to sign-in, and nothing about their
session remains on the phone.

**Why this priority**: users who share or hand over a phone need it, and it is needed to test
every other story; it is small.

**Independent Test**: while signed in, sign out, press back, close and reopen the app, and check
that the user remains on sign-in each time.

**Acceptance Scenarios**:

1. **Given** a signed-in user, **When** they choose to sign out and confirm, **Then** they land on
   sign-in and their session is removed from the phone.
2. **Given** the user has just signed out, **When** they press back, **Then** the app closes
   rather than showing home or any signed-in screen.
3. **Given** the user signed out, **When** they reopen the app, **Then** they see sign-in, not the
   introduction and not home.
4. **Given** there is no connection, **When** the user signs out, **Then** sign-out still
   completes on the phone.

---

### Edge Cases

- The app is closed or killed while an account is being created or a sign-in is in progress:
  on next launch the user is either fully signed in (and goes home) or fully signed out (and
  goes to sign-in), never in between.
- The account is created but saving the user's name fails: the user still reaches home, and the
  name is saved again later rather than the account being left without a profile.
- The user rotates the phone, switches language, or switches theme while filling in a form: what
  they typed and any message on screen are kept.
- The user's phone language is neither Arabic nor English: the app uses English.
- An email typed with spaces before or after it, or in mixed case: it is treated as the same
  email.
- The user presses back on the sign-in screen: the app closes; it does not return to the
  introduction. Back on sign-up or password reset returns to sign-in.
- The user presses back on home right after signing in: the app closes; it does not return to
  sign-in.
- The service is unreachable or answers with an unexpected error: the user sees a general
  "something went wrong, try again" message in their language, never a raw technical message.
- The user has system font size at its largest: every field, button, and message stays fully
  visible and reachable by scrolling.

## Requirements *(mandatory)*

### Functional Requirements

**Start and routing**

- **FR-001**: On every launch, the app MUST decide where the user starts (introduction, sign-in,
  or home) before drawing the first screen after the launch screen, so that no screen is shown
  and then immediately replaced.
- **FR-002**: The app MUST send a user who has a valid session on this phone directly to home,
  including when there is no network connection.
- **FR-003**: The app MUST show the introduction only until the user finishes or skips it once
  on this phone; afterwards a signed-out user MUST start on sign-in.
- **FR-004**: When the app learns that a stored session is no longer valid (account disabled,
  deleted, or session revoked), it MUST sign the user out, show sign-in, and explain why.

**Introduction**

- **FR-005**: The introduction MUST have at most three pages describing what Alpha-AI does, MUST
  let the user skip it from any page, and MUST let the user move between pages forward and back.

**Sign-up**

- **FR-006**: Users MUST be able to create an account with their name, email, and a password
  entered twice.
- **FR-007**: The app MUST check fields before sending: name not empty (after trimming), email in
  a valid format, password at least 8 characters, and both password entries identical. Each
  problem MUST be shown next to its field.
- **FR-008**: After a successful sign-up the user MUST be signed in and taken to home, and the
  user's name MUST be stored with their account so later screens can show it.
  A confirmation email MUST be sent at sign-up, and confirming it MUST NOT be required to use
  any part of the app. On the first arrival at home after sign-up, the app MUST show a one-time
  message saying a confirmation link was sent, naming the address. If sending the email fails,
  sign-up still succeeds and the message is not shown. A lasting reminder with a resend option
  is out of scope here and belongs to spec 005.

**Sign-in**

- **FR-009**: Users MUST be able to sign in with their email and password.
- **FR-010**: A wrong email or a wrong password MUST produce the same message, so the app never
  reveals whether an email has an account through sign-in.
- **FR-011**: After a successful sign-in the user MUST stay signed in across app restarts until
  they sign out or the session becomes invalid. There is no "remember me" choice.

**Password reset**

- **FR-012**: Users MUST be able to request a password reset by email from the sign-in screen.
- **FR-013**: The reset confirmation MUST be the same whether or not the email has an account.
- **FR-014**: The app MUST prevent the user from sending reset requests faster than once every
  60 seconds from the reset screen, and show how long they need to wait.

**Sign-out**

- **FR-015**: A signed-in user MUST be able to sign out from the home screen, with a
  confirmation step.
- **FR-016**: Sign-out MUST remove every piece of session and profile data this feature stored
  on the phone, MUST work without a connection, and MUST leave the user on sign-in with no way
  back to signed-in screens through the back button.

**Errors, feedback, and forms**

- **FR-017**: Every failure MUST be shown as a message in the user's language (Arabic or
  English) that says what happened and what the user can do. At least these cases MUST have
  their own message: no connection, wrong email or password, email already registered, password
  too weak, malformed email, too many attempts, account disabled, and unknown error.
- **FR-018**: While a request is in progress, the screen MUST show that it is working and MUST
  NOT send a second request if the user taps again.
- **FR-019**: Forms MUST keep what the user typed after an error, a rotation, or a theme or
  language change. Password fields MUST be hidden by default with a control to show them.
- **FR-020**: Emails MUST be compared without leading or trailing spaces and without regard to
  letter case.
- **FR-021**: Back navigation MUST follow the rules in Edge Cases: back on sign-in and on home
  closes the app; back on sign-up and password reset returns to sign-in; back on introduction
  pages goes to the previous page, and on the first page closes the app.

**Security and privacy**

- **FR-022**: The app MUST NOT store the user's password on the phone in any form. Any password
  stored by the previous version of the app MUST be erased on first launch of this version.
- **FR-023**: Session data kept on the phone MUST be encrypted and MUST NOT be included in device
  backups.
- **FR-024**: Passwords, session tokens, and email addresses MUST NOT appear in logs or crash
  reports.

**Quality and replacement of the old screens**

- **FR-025**: Every screen in this feature MUST support light and dark themes, Arabic
  (right-to-left) and English (left-to-right), and the largest system font size, and MUST meet
  the accessibility rules of the design system (touch targets, labels for icon-only controls,
  text contrast).
- **FR-026**: The old splash, introduction, sign-in, and sign-up screens, and the old way of
  holding the current user in memory, MUST be removed once the new screens replace them. Every
  screen that used the old user holder MUST get the current user another way and keep working.

### Key Entities

- **Account**: a person's identity in Alpha-AI. Has an email (unique, used to sign in), a name,
  a password known only to the user and the sign-in service, and whether the email has been
  confirmed. Accounts made by the old version of the app remain valid.
- **Session**: the proof on this phone that a user is signed in. Belongs to one account, survives
  app restarts, and ends at sign-out or when the service invalidates it. Never contains the
  password.
- **Introduction status**: a per-phone record of whether the introduction has been finished or
  skipped. It is not tied to an account and is not cleared by sign-out.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In 20 consecutive launches by a signed-in user on a mid-range phone, home is the
  first screen drawn after the launch screen every time, and no other screen appears in between.
- **SC-002**: A returning signed-in user sees home within 2 seconds of tapping the app icon
  (cold start, mid-range phone), with or without a network connection.
- **SC-003**: A first-time user can go from opening the app to home, through the introduction
  and sign-up, in under 2 minutes.
- **SC-004**: Every failure case listed in FR-017 shows its own message, and each message is
  available in both Arabic and English (100% coverage, checked by test).
- **SC-005**: After sign-out, no session, profile, or password data from this feature remains in
  the phone's app storage, and none of it appears in a device backup.
- **SC-006**: A user who forgot their password can request a reset, set a new password, and sign
  in with it in under 5 minutes, not counting email delivery time.
- **SC-007**: Every screen in this feature passes a manual check in light, dark, Arabic, English,
  and largest-font combinations with no clipped or overlapping text.
- **SC-008**: The decisions behind every start, sign-in, sign-up, reset, and sign-out outcome are
  covered by automated tests, including at least one failure path each.

## Assumptions

- Accounts use email and password only. Signing in with Google or another provider is out of
  scope for this feature and can be added by a later spec without changing what this one
  builds.
- Email confirmation is encouraged but never required: an unconfirmed email blocks nothing in
  the app. Confirmation happens through the link in the email, outside the app. Spec 005 adds
  the lasting "confirm your email" reminder with resend to its new home screen.
- The sign-in service already used by the app stays the source of truth for accounts, so
  accounts created by the old version keep working and password reset emails are sent by that
  service.
- The password reset is completed through the link in the email, outside the app, rather than
  with a code typed into the app.
- The app follows the phone's language as set up in spec 003; changing language inside the app
  arrives with spec 005.
- The home screen the user lands on is the one that exists today, and its layout is not changed
  by this feature. Sign-out uses the sign-out button already in its top bar; spec 005 moves it
  into settings. Deleting an account also arrives with
  spec 005.
- The introduction content (up to three pages describing grammar correction and the supporting
  tools) reuses the messages of the current introduction, adjusted to the new design.
- The minimum password length of 8 characters is a product choice; the sign-in service may
  enforce additional rules, which are reported through the "password too weak" message.
