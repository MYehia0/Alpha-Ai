# Contract: Auth Screens (MVI)

Every screen in `:feature:auth` follows constitution III: one immutable `UiState`, one sealed
`Intent`, one sealed `Effect`, `onIntent` as the only input. Screen composables take `state` and
`onIntent`; only the route-level composable sees the ViewModel. Each screen has previews for
content, loading, and error, in light, dark, Arabic, and 200% font (`AlphaPreviews`).

Shared pieces:

- `submitting: Boolean` disables the submit button and shows its progress indicator; a submit
  intent while `submitting` is ignored (FR-018).
- `cancellable: Boolean`: while `true` a Cancel text button is shown next to the progress
  (constitution VI). The `Cancel` intent cancels the request job, sets `submitting = false`, and
  keeps every field; the ViewModel does nothing else. System back while submitting also
  cancels, because leaving the screen clears the ViewModel. Firebase cannot abort a request
  already sent, so the repository, which outlives the screen, makes sure a cancelled request
  that completes later leaves the user signed out (data-model.md "Cancellation"). The user
  always ends up signed out after Cancel, even if the screen is gone by then.
- `fieldErrors: Map<FormField, FieldError>` shows each error under its field. Editing a field
  clears that field's error.
- `failure: AuthFailure?` shows a message above the submit button, in the user's language.
  Editing any field clears it. Typed text is never cleared by a failure (FR-019).
- Password fields: hidden by default, with a show/hide icon button that has a content description.
- Keyboard: Next moves between fields; Done on the last field submits.

## Introduction

| | |
|---|---|
| Route | `IntroductionRoute` |
| State | `page: Int` (0..2), `pageCount = 3` |
| Intents | `NextPage`, `PreviousPage`, `GoToPage(n)` (swipe), `Skip`, `Finish` |
| Effects | `ToSignIn` (after `CompleteIntroductionUseCase`) |
| Back | page > 0 → `PreviousPage`; page 0 → system default (activity finishes) |

Pages: (1) grammar correction in Arabic and English, (2) the supporting tools (speech to text,
image and document text, translation, summaries), (3) history and getting started. Skip is
visible on every page; the last page's primary button is "Get started" (= `Finish`).

## Sign-in

| | |
|---|---|
| Route | `SignInRoute(reason: SignInReason? = null)` |
| State | `email`, `password`, `passwordVisible`, `fieldErrors`, `submitting`, `cancellable`, `failure`, `notice: SignInReason?` |
| Intents | `EmailChanged`, `PasswordChanged`, `TogglePasswordVisibility`, `Submit`, `Cancel`, `ForgotPassword`, `CreateAccount`, `DismissNotice` |
| Effects | `Authenticated`, `ToPasswordReset(email)`, `ToSignUp` |

- `notice` comes from the route argument and shows the reason (signed out, session expired,
  account disabled) once.
- `InvalidCredentials` shows one message for both wrong email and wrong password (FR-010).
- `TooManyAttempts` message includes a "Reset password" action (= `ForgotPassword`).

## Sign-up

| | |
|---|---|
| Route | `SignUpRoute` |
| State | `name`, `email`, `password`, `confirmPassword`, `passwordVisible`, `fieldErrors`, `submitting`, `cancellable`, `failure` |
| Intents | `NameChanged`, `EmailChanged`, `PasswordChanged`, `ConfirmPasswordChanged`, `TogglePasswordVisibility`, `Submit`, `Cancel`, `SignInInstead` |
| Effects | `Authenticated`, `ToSignIn` |

- Validation runs on `Submit`; nothing is sent while any field error exists (FR-007).
- `cancellable` is `true` only while the account is being created. Once it exists, Cancel is
  hidden and saving the profile and sending the confirmation email finish (research §8).
- `EmailAlreadyInUse` shows the message with a "Sign in" action (= `SignInInstead`).
- Password hint under the field: "At least 8 characters".

## Password reset

| | |
|---|---|
| Route | `PasswordResetRoute(email: String = "")` |
| State | `email`, `fieldErrors`, `submitting`, `cancellable`, `failure`, `sent: Boolean`, `cooldownSeconds: Int` |
| Intents | `EmailChanged`, `Submit`, `Cancel`, `BackToSignIn` |
| Effects | `ToSignIn` |

- The email is prefilled from the route argument (Story 4 scenario 1).
- After success `sent = true`: the confirmation text is the same for any email (FR-013), asks
  the user to check the inbox and spam folder, and offers "Back to sign in".
- While `cooldownSeconds > 0`, Submit is disabled and reads "Send again in N s" (FR-014).

## Failure → message mapping

`AuthFailureMessages.messageRes(failure): Int` and `FieldErrorMessages.messageRes(field, error): Int`
in `:feature:auth`. A unit test asserts every `AuthFailure` and every `FieldError` has a string,
and `StringsParityTest` asserts each has an Arabic translation (SC-004).
