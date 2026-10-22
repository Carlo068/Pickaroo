---
name: mock-endpoint
description: Add a PickarooAPI endpoint to the in-app mock the way auth, orders and driver-location were built - contract interface, DTOs, seed data, mock service with permission checks, repository with token refresh, container wiring and tests. Use when a feature needs data from the (not yet connected) backend.
---

# Add a mock PickarooAPI endpoint

The app talks to `PickarooAPI` only through interfaces. Today the implementations are mocks on top of `MockPickarooApi`; `RemoteXApi` stubs mark where the real Retrofit client goes later (`USE_MOCK_API` in `app/build.gradle.kts` picks). Routes come from `docs/PLAN.md` section 5. Reference implementations: `ui/auth/network/MockAuthService.kt`, `ui/orders/network/MockOrdersService.kt`.

## Steps (in this order)

1. **Contract.** `ui/<feature>/network/XApi.kt`: Retrofit-annotated `suspend` functions, `@Header("Authorization") bearer: String` on protected calls. Add `class RemoteXApi : XApi` whose methods throw `UnsupportedOperationException("PickarooAPI todavía no está conectada")`.
2. **DTOs.** `ui/<feature>/model/`: snake_case through `@SerializedName`; lowercase enum values need `@SerializedName` per constant; optional fields are `val x: T? = null` last in the constructor (Gson ignores Kotlin defaults, so a missing field is simply null).
3. **Seed data.** JSON under `docs/mock/` (debug builds bundle the folder as assets). Register a `MockCollection(name, file, key)` in `common/mock/MockStore.kt` and add it to `MockCollections.all` so `reset()` clears it; a collection with no file starts empty (used for sessions). Add its integrity checks to `MockDataFilesTest` (references, roles, UTF-8, totals).
4. **Mock service.** `class MockXService(api: MockPickarooApi, authenticator: MockAuthenticator) : XApi`. Every call runs inside `api.call { ... }`, which adds 300-600 ms latency and honours `api.failNext { ... }`. Inside: `authenticator.authenticate(this, bearer)` (401 for missing/invalid/expired token), then `caller.can(Permission.X)` else `MockErrors.forbidden()`, ownership else `MockErrors.forbidden("NOT_YOUR_...")`, absence `MockErrors.notFound("CODE")`, conflicts `MockErrors.conflict("CODE")`. Writes use `store.put/remove` (persisted across restarts). Join related data on the "server" side (orders carry their shop's name and coordinates).
5. **Repository.** `interface XRepository` + `ApiXRepository(api, authorized: AuthorizedCalls)` wrapping each call as `authorized.call { api.method(it, ...) }` (refreshes an expired token, retries a 401 once, logs out when the refresh fails). Turn a 404 into `null` where "nothing yet" is normal.
6. **Container.** In `common/di/AppContainer.kt`: `val xApi by lazy { if (useMockApi) MockXService(mockApi, MockAuthenticator { System.currentTimeMillis() / 1000 }) else RemoteXApi() }` and `val xRepository by lazy { ApiXRepository(xApi, authorizedCalls) }`.
7. **ViewModel.** Takes the repository in its constructor; companion `Factory` uses `appContainer().xRepository`. Polling is a `suspend fun poll()` that the screen runs under `repeatOnLifecycle(STARTED)`, so it stops when the screen leaves.
8. **Tests** (see `android-test-run`, `compose-ui-tests`):
   - Service: real seed via `FileSeedSource` + `InMemoryStringStore`, log in through `MockAuthService` for a bearer, fake clock. Cover happy path, every role that must get 403, someone else's data (403), unknown id (404), no token / garbage / expired token (401).
   - ViewModel: scripted fake repository with `runTest` virtual time (`currentTime`, `advanceTimeBy`); infinite polling loops need a launched job you cancel.
   - Seed integrity in `MockDataFilesTest`.

## Pitfalls that already cost time

- **Gson 2.8.5**: use `JsonParser().parse(text)`, not `JsonParser.parseString`.
- **minSdk 24**: no `java.time` or `java.util.Base64` in app code. Use `SimpleDateFormat` and `kotlin.io.encoding.Base64`.
- **Never build things in `Application`'s constructor** (`Context` is still null); keep `PikarooApp.container` lazy.
- **An `HttpException` body can be read once.** Inject failures with a factory (`failNext { MockErrors.conflict("X") }`), never by reusing an instance.
- Two mock access tokens issued in the same second were identical until `jti` was added; dedupe token refreshes by the (rotating) refresh token.
- Navigation calls must run on the main thread (`withContext(Dispatchers.Main.immediate)` when reacting to a flow).
- Money is `Double` in DTOs for now; round only for display (`formatPrice`).
