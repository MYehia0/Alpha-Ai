# Contract: App Shell and Navigation

How screens plug into the single-activity frame. Spec 004 is the first consumer.

## Entry points

| Component | Manifest | Exported | Launcher | Notes |
|---|---|---|---|---|
| `SplashActivity` (legacy) | `main` | yes | **yes** | Unchanged; the user flow starts here until spec 004 |
| `MainActivity` | `main` | no | no | Hosts `AlphaApp`; theme `Theme.AlphaAi.Compose` (NoActionBar, edge-to-edge) |
| `activity-alias` "Alpha-AI Gallery" → `MainActivity` | `debug` only | yes | yes (debug) | Opens the gallery; absent from release builds (FR-022) |

## Root composable

```kotlin
@Composable
fun AlphaApp(navController: NavHostController = rememberNavController()) {
    AlphaTheme {
        NavHost(navController, startDestination = ShellDestinations.startRoute) {
            with(ShellDestinations) { register(navController) }
            // spec 004+: authGraph(navController), homeGraph(navController), …
        }
    }
}
```

`ShellDestinations` exists once in `app/src/debug/kotlin` (gallery) and once in
`app/src/release/kotlin` (legacy entry). Both expose:

```kotlin
object ShellDestinations {
    val startRoute: Any
    fun NavGraphBuilder.register(navController: NavHostController)
}
```

## Adding a screen (the steps SC-007 measures)

1. Declare a route in the feature module: `@Serializable data object HistoryRoute` or
   `@Serializable data class ItemRoute(val id: String)` (identifiers and primitives only).
2. Expose `fun NavGraphBuilder.historyGraph(onNavigate: …)` with
   `composable<HistoryRoute> { HistoryScreen(…) }`.
3. Call it inside the `NavHost` in `AlphaApp`.
4. Navigate from a `UiEffect` at the screen boundary: `navController.navigate(HistoryRoute)`.
   ViewModels never hold the `NavController` (constitution V).

No existing screen changes in steps 1–4.

## Back behaviour (FR-019)

- Inside the frame, system back pops the `NavHost` back stack.
- At the start destination, back finishes `MainActivity`, returning to whatever launched it (the
  launcher for the gallery alias).
- Legacy Activities keep their own back behaviour.

## Configuration changes (FR-020)

`MainActivity` does not override `configChanges`; the `NavHost` back stack and
`rememberSaveable` state are restored after rotation and process death. Theme and language
changes recreate the activity and keep the same state.

## Languages (FR-007)

- `generateLocaleConfig = true` and `resources.properties` with `unqualifiedResLocale=en-US`.
- Supported: `en`, `ar`. Anything else falls back to the default `values/` (English).
- No in-app switch in this spec. Spec 005 calls `AppCompatDelegate.setApplicationLocales`.
