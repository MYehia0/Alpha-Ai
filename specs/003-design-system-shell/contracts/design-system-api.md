# Contract: Design System API

The public surface of `:core:designsystem` and the screen-state part of `:core:ui` that every
feature module from spec 004 onward uses. Anything not listed here is `internal`.

Rules for callers (constitution IV):

- Screens wrap their content in `AlphaTheme` only at the root (`AlphaApp`, previews); features
  never create their own theme.
- Colours, text styles, spacing, shapes, and motion come only from `AlphaTheme.*`. Feature code
  has no `Color(0x…)`, no literal `sp`, and no literal `dp` outside these tokens.
- Every user-visible string passed in is a string resource read with `stringResource`.

## Theme

```kotlin
@Composable
fun AlphaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)

object AlphaTheme {
    val colors: AlphaColors        @Composable get   // scheme + brandAccent
    val typography: Typography     @Composable get
    val spacing: AlphaSpacing      @Composable get
    val shapes: Shapes             @Composable get
    val motion: AlphaMotion        @Composable get
}
```

## Components

| Composable | Required parameters | Optional parameters | Notes |
|---|---|---|---|
| `AlphaPrimaryButton` | `text: String`, `onClick: () -> Unit` | `modifier`, `enabled = true`, `busy = false`, `leadingIcon: ImageVector?` | Min height 48 dp; label wraps |
| `AlphaSecondaryButton` | same | same | Outlined |
| `AlphaTextButton` | same | same | For low-emphasis actions |
| `AlphaIconButton` | `icon: ImageVector`, `contentDescription: String`, `onClick` | `enabled`, `mirrorInRtl: Boolean = false` | 48×48 dp target |
| `AlphaTextField` | `value: String`, `onValueChange`, `label: String` | `placeholder`, `errorMessage: String?`, `enabled`, `keyboardOptions`, `keyboardActions` | Single line |
| `AlphaMultilineTextField` | same | `minLines = 3`, `maxLines = 8`, `maxLength: Int?` | Grows then scrolls; counter when `maxLength` set |
| `AlphaCard` | `content: @Composable ColumnScope.() -> Unit` | `onClick: (() -> Unit)?`, `modifier` | |
| `AlphaTopBar` | `title: String` | `onBack: (() -> Unit)?`, `actions: @Composable RowScope.() -> Unit` | Title one line, ellipsis |
| `AlphaLoadingState` | — | `message: String?`, `onCancel: (() -> Unit)?` | Delayed show, minimum display |
| `AlphaEmptyState` | `message: String` | `icon`, `actionLabel: String?`, `onAction` | |
| `AlphaErrorState` | `message: String` | `onRetry: (() -> Unit)?` | Retry shown only when `onRetry` is set |

## Screen state (`:core:ui`)

```kotlin
@Immutable
sealed interface ScreenState<out T> {
    data class Loading(val cancellable: Boolean = false) : ScreenState<Nothing>
    data class Empty(@StringRes val messageRes: Int, @StringRes val actionLabelRes: Int? = null) : ScreenState<Nothing>
    data class Error(@StringRes val messageRes: Int, val retryable: Boolean) : ScreenState<Nothing>
    data class Content<T>(val value: T) : ScreenState<T>
}

fun AiFailure.isRetryable(): Boolean   // = canRetry; false for ServiceUnavailable and InvalidInput
fun AiFailure.toErrorState(): ScreenState.Error

@Composable
fun <T> AlphaScreenStateHost(
    state: ScreenState<T>,
    onRetry: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onEmptyAction: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
)
```

## Previews

```kotlin
@Preview(name = "Light · EN", locale = "en")
@Preview(name = "Dark · EN", locale = "en", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Light · AR", locale = "ar")
@Preview(name = "Dark · AR", locale = "ar", uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Font 200%", fontScale = 2f, widthDp = 360)
annotation class AlphaPreviews
```

Every component file has an `@AlphaPreviews` function per state group.

## Compatibility promise

Adding a parameter with a default value is allowed in later specs. Removing or renaming a
parameter, or changing a default's behaviour, needs a note in that spec's plan.
