---
name: compose-ui-tests
description: Patterns and pitfalls for Pickaroo's Jetpack Compose instrumented tests (state-encoding test tags, logging in with a mock session, waiting for async screens, navigation and map assertions). Use when writing or fixing tests under app/src/androidTest.
---

# Compose UI tests in Pickaroo

Run them with the `android-test-run` skill (`instrumented [package]`). They need the emulator and take 2-5 minutes in total.

## Two kinds of tests

1. **Pure screen tests.** Render a composable with a hand-built state and assert on it (`TrackingContentTest`, `CartViewTest`). Fast and stable. Prefer these for layout and states.
2. **App-flow tests.** Compose the whole `AppNavigation()` with a real mock session (`RoleNavigationTest`, `OrdersFlowTest`). Use for navigation, roles and end to end behaviour.

## Setup for app-flow tests

```kotlin
private val context = InstrumentationRegistry.getInstrumentation().targetContext
private val container = context.appContainer()
private val preferences = AppPreferences(context)
private var onboardingWasCompleted = false

@Before fun setUp() {
    onboardingWasCompleted = preferences.getBoolean("completed_onboarding")
    preferences.putBoolean("completed_onboarding", true)      // otherwise the app starts on onboarding
    container.sessionRepository.clear()
}
@After fun tearDown() {
    container.sessionRepository.clear()
    preferences.putBoolean("completed_onboarding", onboardingWasCompleted)
}
// log in without the UI:
runBlocking { container.sessionRepository.save(container.authApi.login(LoginRequest("customer1", "demo1234"))) }
```
- `owner1` starts in shop mode; call `container.sessionRepository.setActiveMode(RoleMode.CUSTOMER)` before testing customer screens.
- Users, orders and what each can see: see the table in `emulator-ui-check`.

## Waiting and finding

- Mock calls take 300-600 ms and screens poll, so wait explicitly:
  `composeRule.waitUntil(10_000) { composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }`.
- Put tag constants in an `object XTestTags`. Encode state **in the tag** when the state is what you assert (`tracking_step_READY_CURRENT`), so `assertExists()` is the assertion and failures name the state.
- Text fields: `composeRule.onAllNodes(hasSetTextAction())[0].performTextInput(...)` (first is the username, second the password). Tag the buttons: the label "Iniciar Sesión" appears twice on the login screen.
- Items inside a scrolling column: `performScrollTo()` before `performClick()`.
- A custom Android view (the osmdroid map) is opaque to Compose: expose what matters through semantics (`stateDescription` on the `AndroidView`, tag `osm_map`) and assert on that.

## Pitfalls

- `composeRule.onAllNodes(matcher)` is a member of the rule (do not import it); `onNodeWithTag`, `onAllNodesWithTag`, `onNodeWithText`... are extension functions and must be imported from `androidx.compose.ui.test`.
- `assertTextContains("x")` matches a whole text item; pass `substring = true` for part of a text.
- Badge counts and other merged text: use `useUnmergedTree = true`.
- Espresso 3.7.0 + ext-junit 1.3.0 are required on the Android 17 emulator (older ones fail with `NoSuchMethodException InputManager.getInstance`).
- Tab switching with `saveState/restoreState` brings back screens stacked on top (cart, orders, tracking): `navigateToTab` pops them first. Test "switch tab, switch back" after adding any screen above a tab.
- Everything that navigates must run on the main thread; a test that triggers a navigation from a background thread exposes it.
- Unit tests (`src/test`) cannot render Compose; keep logic in ViewModels and pure functions so it can be tested there with `MainDispatcherRule` and `runTest`.
- Real bugs found by these tests before: app crash at startup (container built too early), navigation on the wrong thread, cart screen restored on the wrong tab. Add a test for each bug you fix.
